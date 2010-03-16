package hudson.plugins.dbcharts;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.plugins.dbcharts.DbChartPublisher.DescriptorImpl;
import hudson.util.FormValidation;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class Chart
    implements Serializable, Describable<Chart>
{
    private static final Logger logger=Logger.getLogger( Chart.class.getName()); 
    private static final long serialVersionUID = 1L;
    
    public final String name;
    public final String title;
    public final String valuesAxisLabel;
    public final String categoryAxisLabel;
    public final Integer width;
    public final Integer height;
    public final String connectionName;
    public final String sqlQuery;
    
    @DataBoundConstructor
    public Chart( String name, String title, Integer width, Integer height, String connectionName, String sqlQuery,String valuesAxisLabel,String categoryAxisLabel)
    {
        super();
        this.name = name;
        this.title = title;
        this.width = width;
        this.height = height;
        this.connectionName = connectionName;
        this.sqlQuery = sqlQuery;
        this.valuesAxisLabel=valuesAxisLabel;
        this.categoryAxisLabel=categoryAxisLabel;
    }
    
    public Chart(JSONObject object){
        this.name = object.getString("name");
        this.title = object.getString("title");
        this.width = Integer.parseInt( object.getString("width") );
        this.height = Integer.parseInt( object.getString("height") );
        this.connectionName = object.getString("connectionName");
        this.sqlQuery = object.getString("sqlQuery");
        this.valuesAxisLabel=object.getString( "valuesAxisLabel" );
        this.categoryAxisLabel=object.getString( "categoryAxisLabel" );
    }

    public Descriptor<Chart> getDescriptor()
    {
        return Hudson.getInstance().getDescriptorByType( ChartDescriptor.class );
    }
    
    public DbChartPublisher.DescriptorImpl getPublisherDescriptor(){
        return Hudson.getInstance().getDescriptorByType(DbChartPublisher.DescriptorImpl.class);
    }
        
    public JDBCConnection getJDBCConnection(){
        DescriptorImpl di=getPublisherDescriptor();
        logger.info( "getJDBCConnection:"+di );
        for(JDBCConnection c:di.getConnections()){
            if(connectionName.equals(c.name)){
                return c;
            }
        }
        return null;
    }
    
    @Extension
    public static final class ChartDescriptor extends Descriptor<Chart>{

        @Override
        public String getDisplayName()
        {
            return "";
        }
        
        public FormValidation doCheckName(@QueryParameter String value) throws IOException, ServletException {
            if(value.length()==0)
                return FormValidation.error("Please set a name");
            if(value.length()<4)
                return FormValidation.warning("Isn't the name too short?");
            return FormValidation.ok();
        }
        
        
        public List<String> getConnectionNames(){
            List<String> res=new LinkedList<String>();
            for(JDBCConnection c: Hudson.getInstance().getDescriptorByType(DbChartPublisher.DescriptorImpl.class).connections){
                res.add( c.name );
            }
            logger.fine( "getConnectionNames: "+Arrays.toString( res.toArray()));
            return res;
        }
        
    } 

}