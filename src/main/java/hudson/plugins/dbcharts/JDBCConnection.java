package hudson.plugins.dbcharts;

import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;


public abstract class JDBCConnection /*implements Describable<JDBCConnection>, ExtensionPoint*/ implements Serializable{
    
    private static final long serialVersionUID = -7258724749798334055L;
    
    public final String name;
    public final String url;
    public final String user;
    public final String passwd;
    
    @DataBoundConstructor
    public JDBCConnection(String name, String url, String user, String passwd )
    {
        super();
        this.name = name;
        this.url = url;
        this.user = user;
        this.passwd = passwd;
    }
    
    public JDBCConnection( JSONObject o )
    {
        this.name = o.getString( "name" );
        this.url = o.getString( "url" );
        this.user = o.getString( "user" );
        this.passwd = o.getString( "passwd" );
    }
   
    public List<String> getDrivers(){
        Enumeration<Driver> e=DriverManager.getDrivers();
        List<String> res=new LinkedList<String>();
        while(e.hasMoreElements()){
            res.add( e.nextElement().getClass().getCanonicalName() );
        }
        return res;
    }
    
    public abstract String getDriver();
    
    public abstract static class JDBCConnectionDescriptor<T extends Describable<T>> extends Descriptor<T>{
                
        public FormValidation doCheckName(@QueryParameter String value) throws IOException, ServletException {
            if(value.length()==0)
                return FormValidation.error("Please set a name");
            if(value.length()<4)
                return FormValidation.warning("Isn't the name too short?");
            return FormValidation.ok();
        }      
        
        public void doTestConnection(StaplerRequest req, StaplerResponse rsp,
                                     @QueryParameter("driver") final String driver, 
                                     @QueryParameter("url") final String url,
                                     @QueryParameter("user") final String user,
                                     @QueryParameter("passwd") final String passwd
                                     ) throws IOException, ServletException {
            try
            {            
                Class<?> dc=Class.forName( driver );                
                    
                Driver d=(Driver) dc.newInstance();
                if(!d.acceptsURL( url )){
                    FormValidation.error("Driver: "+driver+" does not accept:"+url).generateResponse( req, rsp, this);                    
                }else{     
                    DriverManager.getConnection( url, user, passwd );
                    FormValidation.ok("Connection OK").generateResponse( req, rsp, this);
                }                
            }
            catch ( SQLException e )
            {
                FormValidation.error(e.getMessage()).generateResponse( req, rsp, this);
            }
            catch ( ClassNotFoundException e )
            {
               FormValidation.error("Class not found:"+e.getMessage()).generateResponse( req, rsp, this);
            }
            catch ( InstantiationException e )
            {
                FormValidation.error("Cannot create instance of:"+e.getMessage()).generateResponse( req, rsp, this);
            }
            catch ( IllegalAccessException e )
            {
                FormValidation.error(e.getMessage()).generateResponse( req, rsp, this);
            }       
        }

    }
    
    public Connection createConnection() throws SQLException{
        return DriverManager.getConnection( url, user, passwd );
    }
    
    
    
}
