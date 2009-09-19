package hudson.plugins.dbcharts;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;

import com.mysql.jdbc.Driver;


public class MySQLJDBCConnection extends JDBCConnection implements Describable<MySQLJDBCConnection>{
    
    private static final long serialVersionUID = -7258724749798334055L;   
   
    
    @DataBoundConstructor
    public MySQLJDBCConnection( String name, String url, String user, String passwd )
    {
        super(name,url,user,passwd);

    }
    
    public MySQLJDBCConnection( JSONObject o )
    {
        super(o);
    }

    public Descriptor<MySQLJDBCConnection> getDescriptor()
    {
        return Hudson.getInstance().getDescriptorByType( MySQLJDBCConnectionDescriptor.class );
    }
    
    @Override
    public String getDriver()
    {
        return Driver.class.getCanonicalName();
    }
    
    @Extension
    public static final class MySQLJDBCConnectionDescriptor extends JDBCConnectionDescriptor<MySQLJDBCConnection> {

        @Override
        public String getDisplayName()
        {
            return "MySQL JDBC connection ("+Driver.class.getCanonicalName()+")";
        }
    }    
    
}
