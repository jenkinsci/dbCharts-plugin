package hudson.plugins.dbcharts;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;


public class CustomJDBCConnection extends JDBCConnection implements Describable<CustomJDBCConnection>{
    
    private static final long serialVersionUID = -7258724749798334055L;    
    
    public final String driver;
    
    @DataBoundConstructor
    public CustomJDBCConnection( String name, String driver, String url, String user, String passwd )
    {
        super(name,url,user,passwd);
        this.driver = driver;

    }
    
    public CustomJDBCConnection( JSONObject o )
    {
        super(o);
        this.driver = o.getString( "driver" );
    }

    public Descriptor<CustomJDBCConnection> getDescriptor()
    {
        return Hudson.getInstance().getDescriptorByType( CustomJDBCConnectionDescriptor.class );
    }
    
    @Override
    public String getDriver()
    {
        return driver;
    }
    
    @Extension
    public static final class CustomJDBCConnectionDescriptor extends JDBCConnectionDescriptor<CustomJDBCConnection> {

        @Override
        public String getDisplayName()
        {
            return "Custom JDBC connection";
        }
    }    
    
}
