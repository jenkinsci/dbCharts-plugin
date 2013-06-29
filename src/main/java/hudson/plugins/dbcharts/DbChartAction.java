package hudson.plugins.dbcharts;

import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.ProminentProjectAction;
import hudson.util.ChartUtil;
import hudson.util.ShiftedCategoryAxis;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.Connection;


import java.sql.Driver;
import java.util.Properties;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.imagemap.ImageMapUtilities;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.jdbc.JDBCCategoryDataset;
import org.jfree.data.jdbc.JDBCXYDataset;
import org.jfree.ui.RectangleInsets;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class DbChartAction implements Action {

    private static final Logger logger = Logger.getLogger(DbChartAction.class.getCanonicalName());
//    private final AbstractProject<?,?> project;
    private final DbChartPublisher publisher;
    /** All plots share the same JFreeChart drawing supplier object. */
    private static final DrawingSupplier supplier = new DefaultDrawingSupplier(
            DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE,
            DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
            DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
            DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
            // the plot data points are a small diamond shape 
            new Shape[]{new Polygon(new int[]{3, 0, -3, 0},
                new int[]{0, 4, 0, -4}, 4)});
    
    public DbChartAction(AbstractProject<?, ?> project, DbChartPublisher publisher) {
        super();
        logger.info("DbChartAction created for project:" + project);
//  /      this.project = project;
        this.publisher = publisher;
    }

    public String getDisplayName() {
       // return "Db Chart";
        return null;
    }

    public String getIconFileName() {
        return null;//"folder.gif";
    }
    
    public String getUrlName() {
        return "dbCharts";
    }

    public List<Chart> getCharts() {
    
        logger.info("getCharts().size=" + publisher.getCharts().size());
        return publisher.getCharts();
    }
    
    public Chart getChartByName(String name) {
        for (Chart c : getCharts()) {
            if (name.equals(c.name)) {
                return c;
            }
        }
        return null;
    }
            
    /**
     * Display the trend map. Delegates to the the associated
     * {@link ResultAction}.
     *
     * @param request
     *            Stapler request
     * @param response
     *            Stapler response
     * @throws IOException
     *             in case of an error
     * @throws SQLException 
     * @throws ClassNotFoundException 
     */
    public void doTrendMap(final StaplerRequest request, final StaplerResponse response) throws IOException, ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        Chart chartParams = getChartByName(request.getParameter("chart"));
        JFreeChart chart = buildChart(chartParams);
        
        ChartRenderingInfo info = new ChartRenderingInfo();
        chart.createBufferedImage(chartParams.width, chartParams.height, info);
     
        EntityCollection entities = info.getEntityCollection();
        if (entities != null) {
            int count = entities.getEntityCount();
            for (int i = count - 1; i >= 0; i--) {
                ChartEntity entity = entities.getEntity(i);
                logger.info("Entity " + entity);
                if (entity instanceof CategoryItemEntity) {
                    CategoryItemEntity cie = (CategoryItemEntity) entity;
                    logger.info(" - area:" + cie.getArea().getBounds());
                    logger.info(" - shapeCords:" + cie.getShapeCoords());
                    logger.info(" - columnKey:" + cie.getColumnKey() + " " + cie.getColumnKey().getClass().toString());
                    logger.info(" - rowKey:" + cie.getRowKey() + " " + cie.getRowKey().getClass().toString());
                    logger.info(" - columnIndex:" + cie.getDataset().getColumnIndex(cie.getColumnKey()));
                    logger.info(" - rowIndex:" + cie.getDataset().getRowIndex(cie.getRowKey()));
                }                
            }
        }
        
        Rectangle dataArea = info.getPlotInfo().getDataArea().getBounds();
        logger.info("chartArea:" + info.getChartArea());
        logger.info("dataArea:" + dataArea);
        logger.info("plotArea:" + info.getPlotInfo().getPlotArea());
        
        
        
        final int categoriesCount = chart.getCategoryPlot().getCategories().size();
        StringBuilder[] buffers = new StringBuilder[categoriesCount];
        Integer[] starts = new Integer[categoriesCount];
        Integer[] ends = new Integer[categoriesCount];
        
        
        if (entities != null) {
            int count = entities.getEntityCount();
            for (int i = count - 1; i >= 0; i--) {
                ChartEntity entity = entities.getEntity(i);
                if (entity instanceof CategoryItemEntity) {
                    CategoryItemEntity cie = (CategoryItemEntity) entity;
                    int columnIndex = cie.getDataset().getColumnIndex(cie.getColumnKey());
                    int rowIndex = cie.getDataset().getRowIndex(cie.getRowKey());

                    if (buffers[columnIndex] == null) {
                        buffers[columnIndex] = new StringBuilder(cie.getColumnKey() + ": ");
                        starts[columnIndex] = cie.getArea().getBounds().x;
                        ends[columnIndex] = cie.getArea().getBounds().x + cie.getArea().getBounds().width;
                   }                   
                    buffers[columnIndex].append(" " + cie.getRowKey() + "=" + cie.getDataset().getValue(rowIndex, columnIndex));
                   
                }                
            }
        }
         
        
        response.setContentType("text/plain;charset=UTF-8");
        
        PrintWriter ps = response.getWriter();
        ps.println("<map id='map' name='map'>");
        int last_start = dataArea.getBounds().x;
        
        for (int i = 0; i < categoriesCount; i++) {
            int new_last_start =
                    (i < categoriesCount - 1)
                    ? (ends[i] + starts[i + 1]) / 2
                    : (ends[i] + dataArea.x + dataArea.width) / 2;
            
            ps.print(" <area shape='rect' coords='"
                    + last_start + "," + dataArea.y + ","
                    + new_last_start + ","
                    + (dataArea.y + dataArea.height) + "' ");
            ps.print(" title='" + ImageMapUtilities.htmlEscape(buffers[i].toString()) + "'");
           ps.println(" alt='' nohref='nohref' />");
           
            last_start = new_last_start;
            
        }
        ps.println("</map>");
    }

    /**
     * Display the trend graph. Delegates to the the associated
     * {@link ResultAction}.
     *
     * @param request
     *            Stapler request
     * @param response
     *            Stapler response
     * @throws IOException
     *             in case of an error in
     *             {@link ResultAction#doGraph(StaplerRequest, StaplerResponse, int)}
     */
    public void doTrend(final StaplerRequest request, final StaplerResponse response) throws IOException, InstantiationException, IllegalAccessException {
        Chart c = getChartByName(request.getParameter("chart"));
        try {
            ChartUtil.generateGraph(
                    request,
                    response,
                    buildChart(c),
                    c.width,
                    c.height);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private Connection getConnection(String name, String url) throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException{
        // load the sqlite-JDBC driver using the current class loader
        //Class.forName("org.sqlite.JDBC");
        Driver drv = null;
        try{           
            Class drvcl =  DbChartAction.class.getClassLoader().loadClass(name);
            drv = ( Driver ) drvcl.newInstance();
            logger.log(Level.INFO, "Class [{0}] loaded", name);
        }catch(InstantiationException e){
            e.printStackTrace();
            logger.log(Level.INFO, "Cannot instantiate class:[{0}]", name);
            throw(e);
            
        }catch(ClassNotFoundException e){
            e.printStackTrace();
            logger.log(Level.INFO, "Cannot load file:[{0}]", name);
            throw(e);
        }
        
        Connection connection = null;
        Properties prop = new Properties();
        try {
            // create a database connection
            logger.log(Level.INFO, "Connecting with url[{0}]", url);
            connection = drv.connect(url, prop);
        } catch (SQLException e) {
            logger.log(Level.INFO, "Cannot connect with url:[{0}]", url);
            throw(e);
        }
        return connection;
    }

    private JFreeChart buildChart(Chart c) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        //JDBCCategoryDataset dataset = new JDBCCategoryDataset(c.getJDBCConnection().url, c.getJDBCConnection().getDriver(), c.getJDBCConnection().user, c.getJDBCConnection().passwd);
        Connection con = getConnection(c.getJDBCConnection().getDriver(), c.getJDBCConnection().url);
        JDBCCategoryDataset dataset = new JDBCCategoryDataset(con);
        //JDBCXYDataset dataset = new JDBCXYDataset(con);
        dataset.executeQuery(c.sqlQuery);
        
        JFreeChart chart = ChartFactory.createLineChart
                (c.title, c.categoryAxisLabel, c.valuesAxisLabel, dataset,
                PlotOrientation.VERTICAL, true, true, false);
        
        
        final CategoryPlot plot = chart.getCategoryPlot();
        //plot.set
        plot.setDomainGridlinePaint(Color.black);
        plot.setRangeGridlinePaint(Color.black);
        plot.setDrawingSupplier(supplier);
        plot.setBackgroundPaint(Color.white);

        chart.setBackgroundPaint(Color.white);
        chart.getTitle().setFont(new Font(
                chart.getTitle().getFont().getName(),
                chart.getTitle().getFont().getStyle(),
                14));
        
        CategoryItemRenderer cit = chart.getCategoryPlot().getRenderer();
        cit.setBaseItemLabelFont(new Font(
                chart.getTitle().getFont().getName(), 
                chart.getTitle().getFont().getStyle(), 
                12));

        CategoryItemRenderer r = (CategoryItemRenderer) plot.getRenderer();
        BasicStroke wideLine = new BasicStroke( 3.0f );
        for(int i = 0; i<dataset.getRowCount(); i++){
            r.setSeriesStroke( i, wideLine); 
        }


        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        
        CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
        plot.setDomainAxis(domainAxis);
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        domainAxis.setMaximumCategoryLabelWidthRatio(0.4f);
        domainAxis.setLowerMargin(0.0);
        domainAxis.setUpperMargin(0.0);
        domainAxis.setCategoryMargin(0.0);

        plot.setInsets(new RectangleInsets(0, 0, 0, 5.0));
        con.close();
        return chart;
    }
}
