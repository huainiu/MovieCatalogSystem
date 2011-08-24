package net.milanaleksic.mcs.restore;

import net.milanaleksic.mcs.ApplicationManager;
import net.milanaleksic.mcs.config.ApplicationConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.sql.*;

/**
 * User: Milan Aleksic
 * Date: 8/10/11
 * Time: 5:38 PM
 */
public abstract class AbstractRestorePointService implements InitializingBean {

    protected static final String MCS_VERSION_TAG = "/*MCS-VERSION: ";

    protected static final String SCRIPT_KATALOG_RESTORE = "KATALOG_RESTORE.sql";

    @Autowired ApplicationManager applicationManager;

    protected final Log log = LogFactory.getLog(this.getClass());

    protected ApplicationConfiguration.DatabaseConfiguration databaseConfiguration;

    private String dbDriver;
    protected String dbUrl;
    private String dbUsername;
    private String dbPassword;

    public void setDbDriver(String dbDriver) {
        this.dbDriver = dbDriver;
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    private boolean driverRegistered = false;
    protected synchronized Connection prepareDriverAndFetchConnection() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
        if (!driverRegistered) {
            driverRegistered = true;
            log.debug("Registering database driver");
            Class.forName(dbDriver).newInstance();
        }

        log.debug("Getting connection");
        return DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
    }

    protected void close(OutputStream pos) {
        if (pos != null) {
            try { pos.close(); } catch(IOException ignored) {}
        }
    }

    protected void close(InputStream is) {
        if (is != null) {
            try { is.close(); } catch(IOException ignored) {}
        }
    }

    protected void close(ResultSet rs) {
        if (rs != null)
            try {
                rs.close();
            } catch (SQLException e) {
                log.error("Failure while closing ResultSet", e);
            }
    }

    protected void close(PreparedStatement ps) {
        if (ps != null)
            try {
                ps.close();
            } catch (SQLException e) {
                log.error("Failure while closing PreparedStatement", e);
            }
    }

    protected void close(Connection conn) {
        if (conn != null)
            try {
                conn.close();
            } catch (SQLException e) {
                log.error("Failure while closing DB Connection", e);
            }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        databaseConfiguration = applicationManager.getApplicationConfiguration().getDatabaseConfiguration();
    }

}
