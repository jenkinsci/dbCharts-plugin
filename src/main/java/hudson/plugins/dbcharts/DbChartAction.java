package hudson.plugins.dbcharts;

import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.util.ChartUtil;
import hudson.util.ShiftedCategoryAxis;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.imagemap.ImageMapUtilities;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.jdbc.JDBCCategoryDataset;
import org.jfree.ui.RectangleInsets;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class DbChartAction  implements Action
{
    private static final Logger logger=Logger.getLogger( DbChartAction.class.getCanonicalName());
    private final AbstractProject project;
    private final DbChartPublisher publisher;
    
    public DbChartAction( AbstractProject project, DbChartPublisher publisher )
    {        
        super();
        logger.info( "DbChartAction created for project:"+project);
        this.project = project;
        this.publisher = publisher;
    }

    public String getDisplayName()
    {
        return "Db Chart";
    }

    public String getIconFileName()
    {
        return "folder.gif";
    }
    
    public String getUrlName()
    {
        return "dbCharts";
    }

    
  
    public List<Chart> getCharts(){
        logger.info( "getCharts().size="+publisher.getCharts().size() );
        return publisher.getCharts();
    }
    
    public Chart getChartByName(String name){
        for(Chart c:getCharts()){
            if (name.equals( c.name )){
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
    public void doTrendMap(final StaplerRequest request, final StaplerResponse response) throws IOException, ClassNotFoundException, SQLException {
        Chart chartParams=getChartByName( request.getParameter( "chart" ) );
        JFreeChart chart=buildChart(chartParams);        
        
        ChartRenderingInfo info = new ChartRenderingInfo();
        chart.createBufferedImage(chartParams.width,chartParams.height,info);
     
        EntityCollection entities = info.getEntityCollection();
        if (entities != null) {
            int count = entities.getEntityCount();
            for (int i = count - 1; i >= 0; i--) {
                ChartEntity entity = entities.getEntity(i);
                logger.info("Entity "+entity);
                if (entity instanceof CategoryItemEntity ){
                   CategoryItemEntity cie=(CategoryItemEntity) entity;
                   logger.info(" - area:"+cie.getArea().getBounds());
                   logger.info(" - shapeCords:"+cie.getShapeCoords());
                   logger.info(" - columnKey:"+cie.getColumnKey() +" "+cie.getColumnKey().getClass().toString());
                   logger.info(" - rowKey:"+cie.getRowKey()+" "+cie.getRowKey().getClass().toString());
                   logger.info(" - columnIndex:"+cie.getDataset().getColumnIndex( cie.getColumnKey() ));
                   logger.info(" - rowIndex:"+cie.getDataset().getRowIndex( cie.getRowKey() ));
                }                
            }
        }
        
        Rectangle dataArea=info.getPlotInfo().getDataArea().getBounds();
        logger.info("chartArea:"+info.getChartArea());
        logger.info("dataArea:"+dataArea);
        logger.info("plotArea:"+info.getPlotInfo().getPlotArea());
        
        
        
        final int categoriesCount=chart.getCategoryPlot().getCategories().size();
        StringBuilder[] buffers=new StringBuilder[categoriesCount];
        Integer[] starts=new Integer[categoriesCount];
        Integer[] ends=new Integer[categoriesCount];
        
        
        if (entities != null) {
            int count = entities.getEntityCount();
            for (int i = count - 1; i >= 0; i--) {
                ChartEntity entity = entities.getEntity(i);
                if (entity instanceof CategoryItemEntity ){
                   CategoryItemEntity cie=(CategoryItemEntity) entity;
                   int columnIndex=cie.getDataset().getColumnIndex( cie.getColumnKey() );
                   int rowIndex=cie.getDataset().getRowIndex( cie.getRowKey() );
                   
                   if (buffers[columnIndex]==null){
                       buffers[columnIndex]=new StringBuilder(cie.getColumnKey()+": ");
                       starts[columnIndex]=cie.getArea().getBounds().x;
                       ends[columnIndex]=cie.getArea().getBounds().x+cie.getArea().getBounds().width;
//                       rectangles[columnIndex]=new Rectangle(
//                           cie.getArea().getBounds().x,dataArea.y,
//                           cie.getArea().getBounds().width,dataArea.height);
                   }                   
                   buffers[columnIndex].append(" "+cie.getRowKey()+"="+cie.getDataset().getValue( rowIndex,columnIndex));
                   
                }                
            }
        }
         
        
        response.setContentType("text/plain;charset=UTF-8");
        
        PrintWriter ps=response.getWriter();
        ps.println("<map id='map' name='map'>");
        int last_start=dataArea.getBounds().x;
        
        for(int i=0; i<categoriesCount; i++){
            int new_last_start=
                (i<categoriesCount-1)?
                    (ends[i]+starts[i+1])/2
                    :(ends[i]+dataArea.x+dataArea.width)/2;
            
            ps.print(" <area shape='rect' coords='"
                     +last_start+","+dataArea.y+","
                     +new_last_start+","
                     +(dataArea.y+dataArea.height)+"' ");
           ps.print(" title='"+ImageMapUtilities.htmlEscape(buffers[i].toString())+"'");
           ps.println(" alt='' nohref='nohref' />");
           
           last_start=new_last_start;
            
        }
        ps.println("</map>");
        //<area shape="poly" coords="498,27,497,30,495,30,492,30,492,27,492,25,495,24,497,25,498,27,498,27" title="(2*valueDouble, 2009-08-29 19:10:00.0) = 0,678" alt="" nohref="nohref"/>

        
        

        //response.getWriter()
        //info.getPlotInfo().getDataArea();
        
//        ChartUtil.generateClickableMap(
//                request,
//                response,
//                buildChart(chartParams),
//                CHART_WIDTH,
//                CHART_HEIGHT);
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
    public void doTrend(final StaplerRequest request, final StaplerResponse response) throws IOException {
        //AbstractBuild<?,?> lastBuild = this.getLastFinishedBuild();
        //CcccBuildAction lastAction = lastBuild.getAction(CcccBuildAction.class);
        Chart c=getChartByName( request.getParameter("chart" ) );
        try
        {
            ChartUtil.generateGraph(
                    request,
                    response,
                    buildChart(c),
                    c.width,
                    c.height);
        }
        catch ( ClassNotFoundException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch ( SQLException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private JFreeChart buildChart(Chart c) throws ClassNotFoundException, SQLException
    {
        //Connection connection=Connection.
     //   JDBCCategoryDataset dataset=new JDBCCategoryDataset("jdbc:mysql://localhost/test1","com.mysql.jdbc.Driver","root","");
        
        JDBCCategoryDataset dataset=new JDBCCategoryDataset(c.getJDBCConnection().url,c.getJDBCConnection().getDriver(),c.getJDBCConnection().user,c.getJDBCConnection().passwd);
        dataset.executeQuery(c.sqlQuery);
        //dataset.
        JFreeChart chart=ChartFactory.createLineChart( c.title, c.categoryAxisLabel,c.valuesAxisLabel, dataset, 
                                                              PlotOrientation.VERTICAL, true, true, false );
        final CategoryPlot plot = chart.getCategoryPlot();
        
//        LineAndShapeRenderer ar = new LineAndShapeRenderer() {
            
            
//            @Override
//            public String generateURL(CategoryDataset dataset, int row, int column) {
//                NumberOnlyBuildLabel label = (NumberOnlyBuildLabel) dataset.getColumnKey(column);
//                return relPath+label.build.getNumber()+"/testReport/";
//            }

            
            
//            @Override
//            public String generateToolTip(CategoryDataset dataset, int row, int column) {
//                NumberOnlyBuildLabel label = (NumberOnlyBuildLabel) dataset.getColumnKey(column);
//                AbstractTestResultAction a = label.build.getAction(AbstractTestResultAction.class);
//                switch (row) {
//                    case 0:
//                        return String.valueOf(Messages.AbstractTestResultAction_fail(a.getFailCount()));
//                    case 1:
//                        return String.valueOf(Messages.AbstractTestResultAction_skip(a.getSkipCount()));
//                    default:
//                        return String.valueOf(Messages.AbstractTestResultAction_test(a.getTotalCount()));
//                }
//            }
//        };
//        plot.setRenderer(ar);
        

        CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
        plot.setDomainAxis(domainAxis);
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        domainAxis.setMaximumCategoryLabelWidthRatio( 0.4f );
        domainAxis.setLowerMargin(0.0);
        domainAxis.setUpperMargin(0.0);
        domainAxis.setCategoryMargin(0.0);

        //final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
//        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        plot.setInsets(new RectangleInsets(0,0,0,5.0));
        
        //chart.
        return chart;
    }
    
}

