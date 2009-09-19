package hudson.plugins.dbcharts;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;

import org.postgresql.Driver;


public class PostgresqlJDBCConnection extends JDBCConnection implements Describable<PostgresqlJDBCConnection>{
    
    private static final long serialVersionUID = -7258724749798334055L;   
   
    
    @DataBoundConstructor
    public PostgresqlJDBCConnection( String name, String url, String user, String passwd )
    {
        super(name,url,user,passwd);

    }
    
    public PostgresqlJDBCConnection( JSONObject o )
    {
        super(o);
    }

    public Descriptor<PostgresqlJDBCConnection> getDescriptor()
    {
        return Hudson.getInstance().getDescriptorByType( PostgresqlJDBCConnectionDescriptor.class );
    }
    
    @Override
    public String getDriver()
    {
        return Driver.class.getCanonicalName();
    }
    
    @Extension
    public static final class PostgresqlJDBCConnectionDescriptor extends JDBCConnectionDescriptor<PostgresqlJDBCConnection> {

        @Override
        public String getDisplayName()
        {
            return "Postgresql JDBC connection ("+Driver.class+")";
        }
    }    
    
}
