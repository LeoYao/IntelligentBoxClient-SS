package intelligentBoxClient.ss.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Repository;

import java.sql.*;

/**
 * Created by yaohx on 3/22/2016.
 */
@Repository
public class SqliteContext implements ISqliteContext {

    private static Log logger = LogFactory.getLog(SqliteContext.class);
    private Connection c = null;

    public boolean open(String dbFile)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("Openning DB [" + dbFile + "].");
        }

        try {
            connect(dbFile);
            createDirectoryTable();
            createLockTable();
        } catch (ClassNotFoundException e) {
            logger.error(e);
            return false;
        } catch (SQLException e) {
            logger.error(e);
            return false;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Opened DB [\" + dbFile + \"] successfully");
        }

        return true;
    }

    private void connect(String dbFile) throws ClassNotFoundException, SQLException {
        if (c == null || c.isClosed())
        {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
        }

    }

    public void createDirectoryTable() throws SQLException {
        String sql = "create table if not exists DIRECTORY \n" +
                "(full_path varchar(4000) PRIMARY KEY,\n" +
                " parent_folder_full_path varchar(4000), \n" +
                " entry_name varchar(255),\n" +
                " type integer,\n" +
                " size integer,\n" +
                " mtime datetime,\n" +
                " atime datetime,\n" +
                " is_locked integer,\n" +
                " is_modified integer,\n" +
                " Is_local integer,\n" +
                " In_use_count integer);";

        executeSql(sql);
    }

    public void createLockTable() throws SQLException {
        String sql = "create table if not exists LOCK (dummy char(1));";

        executeSql(sql);
    }

    private void executeSql(String sql) throws SQLException {

        if (logger.isDebugEnabled())
            logger.debug("Executing [" + sql + "]");

        Statement stmt = null;
        try {
            stmt = c.createStatement();
            stmt.execute(sql);
        }
        finally
        {
            if (stmt != null && !stmt.isClosed()) {
                stmt.close();
            }
        }

        if (logger.isDebugEnabled())
            logger.debug("Executed [" + sql + "]");
    }

    public boolean close() {

        logger.debug("Closing DB...");
        try {
            if (c != null && !c.isClosed()) {
                c.close();
            }
        } catch (SQLException e) {
            logger.error("Failed to close DB.", e);
            return false;
        }

        logger.debug("Closed Db.");
        return true;
    }

    public void query() throws SQLException {
        Statement stmt = c.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM tbl1;");
        while (rs.next()) {
            String one = rs.getString("one");
            int two = rs.getInt("two");
            System.out.println("one = " + one);
            System.out.println("two = " + two);
            System.out.println();
        }
        rs.close();
        stmt.close();
    }

    public void tx_insert() throws SQLException {
        c.setAutoCommit(false);
        //c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        Statement stmt = c.createStatement();
        stmt.execute(
                "Insert into tbl1 (one, two) values " +
                        "(\"world\", 30);");
        c.rollback();
        stmt.close();
        c.setAutoCommit(true);
    }

    public void insert() throws SQLException {
        Statement stmt = c.createStatement();
        stmt.execute(
                "Insert into tbl1 (one, two) values " +
                        "(\"world\", 40);");

        stmt.close();
    }
}
