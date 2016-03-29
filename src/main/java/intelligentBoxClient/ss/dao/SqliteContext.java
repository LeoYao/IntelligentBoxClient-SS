package intelligentBoxClient.ss.dao;

import intelligentBoxClient.ss.dao.pojo.DirectoryEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Repository;
import org.sqlite.SQLiteErrorCode;

import java.sql.*;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by yaohx on 3/22/2016.
 */

public abstract class SqliteContext implements ISqliteContext {

    protected Log logger = LogFactory.getLog(this.getClass());

    protected Connection _connection = null;
    private PreparedStatement _lockStatement;

    public boolean open(String dbFile) {
        if (logger.isDebugEnabled()) {
            logger.debug("Openning DB [" + dbFile + "].");
        }

        try {
            connect(dbFile);
            if (!initDb())
            {
                return false;
            }
            createLockTable();
            prepareStatements();
        } catch (ClassNotFoundException e) {
            logger.error("Failed to open DB [" + dbFile + "]", e);
            return false;
        } catch (SQLException e) {
            logger.error("Failed to open DB [" + dbFile + "]", e);
            return false;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Opened DB [" + dbFile + "] successfully");
        }

        return true;
    }

    public boolean close() {

        logger.debug("Closing DB...");
        try {

            disposeStatements();

            if (_connection != null && !_connection.isClosed()) {
                _connection.close();
            }

        } catch (SQLException e) {
            logger.error("Failed to close DB.", e);
            return false;
        }

        logger.debug("Closed Db.");
        return true;
    }

    public void beginTransaction(int maxRetryTimes, int retryInterval) throws SQLException, InterruptedException {
        for (int i = 0; i <= maxRetryTimes; ++i) {
            try {
                _connection.setAutoCommit(false);
                _lockStatement.executeUpdate();
            } catch (SQLException e) {
                if (SQLiteErrorCode.SQLITE_BUSY.code != e.getErrorCode() || i >= maxRetryTimes) {
                    throw e;
                }
                Thread.sleep(retryInterval);
            }
        }
    }

    public void commitTransaction() throws SQLException {
        _connection.commit();
        _connection.setAutoCommit(true);
    }

    public void rollbackTransaction() throws SQLException {
        _connection.rollback();
        _connection.setAutoCommit(true);
    }

    private void connect(String dbFile) throws ClassNotFoundException, SQLException {
        if (_connection == null || _connection.isClosed()) {
            Class.forName("org.sqlite.JDBC");
            _connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
        }
    }

    private void createLockTable() throws SQLException {
        String sql = "create table if not exists LOCK (dummy char(1));";

        executeSql(sql);

        _lockStatement = _connection.prepareStatement("UPDATE lock SET dummy = 1;");
    }

    protected void executeSql(String sql) throws SQLException {

        if (logger.isDebugEnabled())
            logger.debug("Executing [" + sql + "]");

        Statement stmt = null;
        try {
            stmt = _connection.createStatement();
            stmt.execute(sql);
        } finally {
            if (stmt != null && !stmt.isClosed()) {
                stmt.close();
            }
        }

        if (logger.isDebugEnabled())
            logger.debug("Executed [" + sql + "]");
    }

    protected long getEpochTime(Timestamp ts)
    {
        return ts.getTime() / 1000;
    }

    protected Timestamp getTimestamp(long epochTime)
    {
        return new Timestamp(epochTime * 1000);
    }

    protected Timestamp roundTimestamp(Timestamp ts)
    {
        return new Timestamp(getEpochTime(ts) * 1000);
    }

    protected abstract boolean prepareStatements();

    protected abstract boolean disposeStatements();

    protected abstract boolean initDb();

}
