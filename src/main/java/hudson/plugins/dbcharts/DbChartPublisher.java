package hudson.plugins.dbcharts;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.plugins.dbcharts.CustomJDBCConnection.CustomJDBCConnectionDescriptor;
import hudson.plugins.dbcharts.MySQLJDBCConnection.MySQLJDBCConnectionDescriptor;
import hudson.plugins.dbcharts.PostgresqlJDBCConnection.PostgresqlJDBCConnectionDescriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;

import java.io.IOException;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public class DbChartPublisher extends Recorder
{
    static final Logger logger=Logger.getLogger( DbChartPublisher.class.getCanonicalName() );
    private final List<Chart> charts;
    
    public DbChartPublisher()
    {
        charts=new LinkedList<Chart>();
    }
    
    @DataBoundConstructor
    public DbChartPublisher(List<Chart> charts)
    {
        this.charts=charts;
    }
    
    public List<Chart> getCharts()
    {
        return charts;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Action getProjectAction( AbstractProject project )
    {
        logger.info("DbChartPublisher.getProjectAction called");
        return new DbChartAction(project,this);
    }
    
    public BuildStepMonitor getRequiredMonitorService()
    {
        return BuildStepMonitor.NONE;
    }    
    
    @Override
    public DescriptorImpl getDescriptor()
    {
        logger.info("DbChartPublisher.getDescriptor() called");
        return (DescriptorImpl)super.getDescriptor();
    }   
    
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher>{
        private static final Logger logger=Logger.getLogger( DescriptorImpl.class.getCanonicalName() );
        public List<JDBCConnection> connections;

        public DescriptorImpl()
        {
            logger.fine("DescriptorImpl constructed");
            load();
        }
        
        public List<JDBCConnection> getConnections()
        {
            return connections;
        }

       
        public List<String> getDrivers(){
                        Enumeration<Driver> e=DriverManager.getDrivers();
            List<String> res=new LinkedList<String>();
            while(e.hasMoreElements()){
                res.add( e.nextElement().getClass().getCanonicalName() );
            }
            logger.fine( "DescriptorImpl.getDrivers returned:"+Arrays.toString( res.toArray()) );
            return res;
        }
        
        
        @SuppressWarnings("unchecked")
        @Override
        public boolean isApplicable( Class<? extends AbstractProject> jobType )
        {         
            return true;
        }

        @Override
        public String getDisplayName()
        {
            return "dbCharts configuration";
        }
       
        @Override
        public boolean configure( StaplerRequest req, JSONObject json )
            throws hudson.model.Descriptor.FormException
        {
            logger.fine( "DescriptorImpl.configure:"+json );
            connections = new LinkedList<JDBCConnection>();
            Object ob=json.get( "connections"  );
            try{
                if (ob instanceof JSONArray)
                {
                    for(Object o:(JSONArray)ob){
                        JDBCConnection c=(JDBCConnection)Class.forName( ((JSONObject)o).getString("stapler-class"))
                            .getConstructor( JSONObject.class )
                            .newInstance(o);
                        connections.add( c);
                    }
                } else if (ob!=null) {
                    JDBCConnection c=(JDBCConnection)Class.forName( ((JSONObject)ob).getString("stapler-class"))
                        .getConstructor( JSONObject.class )
                        .newInstance(ob);
                    connections.add( c);
                }
            }catch(Exception e){
                throw new FormException( "Failed to save dbCharts connections", e, "connections" );
            }
            save();
            return super.configure( req, json );
        }
                
        
        public FormValidation doCheckName(@QueryParameter String value) throws IOException, ServletException {
            if(value.length()==0)
                return FormValidation.error("Please set a name");
            if(value.length()<4)
                return FormValidation.warning("Isn't the name too short?");
            return FormValidation.ok();
        }
        
        
        public List<Descriptor> getJDBCConnectionDescriptors(){
            List<Descriptor> res=new LinkedList<Descriptor>();
            Descriptor<?> custom=Hudson.getInstance().getDescriptorByType( CustomJDBCConnectionDescriptor.class );
            Descriptor<?> mysql=Hudson.getInstance().getDescriptorByType( MySQLJDBCConnectionDescriptor.class );
            Descriptor<?> pgsql=Hudson.getInstance().getDescriptorByType( PostgresqlJDBCConnectionDescriptor.class );
            res.add(mysql);
            res.add( pgsql );
            res.add(custom);            
            logger.fine("getJDBCConnectionDescriptors(): custom="+custom+"; mysql="+mysql);
            return res;
        }

        
    }
    

    
}
