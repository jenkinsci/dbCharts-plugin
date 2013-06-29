package hudson.plugins.dbcharts;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;

import org.sqlite.JDBC;


public class SqliteJDBCConnection extends JDBCConnection implements Describable<SqliteJDBCConnection>{
    
    //private static final long serialVersionUID = -7258724749798334056L;   
   
    
    @DataBoundConstructor
    public SqliteJDBCConnection( String name, String url, String user, String passwd )
    {
        super(name,url,user,passwd);

    }
    
    public SqliteJDBCConnection( JSONObject o )
    {
        super(o);
    }

    public Descriptor<SqliteJDBCConnection> getDescriptor()
    {
        return Hudson.getInstance().getDescriptorByType( SqliteJDBCConnectionDescriptor.class );
    }
    
    @Override
    public String getDriver()
    {
        return JDBC.class.getCanonicalName();
    }
    
    @Extension
    public static final class SqliteJDBCConnectionDescriptor extends JDBCConnectionDescriptor<SqliteJDBCConnection> {

        @Override
        public String getDisplayName()
        {
            return "Sqlite JDBC connection ("+JDBC.class.getCanonicalName()+")";
        }
    }    
    
}
