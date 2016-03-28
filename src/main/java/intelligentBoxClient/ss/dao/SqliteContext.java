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
@Repository
public class SqliteContext implements ISqliteContext {

    private static Log logger = LogFactory.getLog(SqliteContext.class);
    private Connection _connection = null;
    private PreparedStatement _queryFileStatement;
    private PreparedStatement _traverseDirectoryStatement;
    private PreparedStatement _lockStatement;
    private PreparedStatement _insertStatement;
    private PreparedStatement _updateStatement;
    private PreparedStatement _deleteStatement;

    public boolean open(String dbFile) {
        if (logger.isDebugEnabled()) {
            logger.debug("Openning DB [" + dbFile + "].");
        }

        try {
            connect(dbFile);
            createDirectoryTable();
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

            if (_queryFileStatement != null && !_queryFileStatement.isClosed()) {
                _queryFileStatement.close();
            }

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

    public DirectoryEntity querySingleFile(String fullPath) throws SQLException {
        DirectoryEntity result = null;
        ResultSet rs = null;

        try {
            _queryFileStatement.setString(1, fullPath);
            rs = _queryFileStatement.executeQuery();

            if (rs.next()) {
                result = populateDirectoryEntity(rs);
            }
        } finally {
            if (rs != null && !rs.isClosed()) {
                rs.close();
            }
        }

        return result;
    }

    public List<DirectoryEntity> queryFiles(String parentFolderFullPath) throws SQLException {
        List<DirectoryEntity> results = new LinkedList<DirectoryEntity>();
        ResultSet rs = null;

        try {
            _queryFileStatement.setString(1, parentFolderFullPath);
            rs = _traverseDirectoryStatement.executeQuery();
            while (rs.next()) {
                results.add(populateDirectoryEntity(rs));
            }
        } finally {
            if (rs != null && !rs.isClosed()) {
                rs.close();
            }
        }

        return results;
    }

    public int updateFile(DirectoryEntity entity) throws SQLException {
        int affectedRowCnt = 0;

        _updateStatement.setString(1, entity.getParentFolderFullPath());
        _updateStatement.setString(2, entity.getEntryName());
        _updateStatement.setInt(3, entity.getType());
        _updateStatement.setLong(4, entity.getSize());
        _updateStatement.setLong(5, entity.getMtime().getTime() / 1000);
        _updateStatement.setLong(6, entity.getAtime().getTime() / 1000);
        _updateStatement.setInt(7, entity.isLocked());
        _updateStatement.setInt(8, entity.isModified());
        _updateStatement.setInt(9, entity.isLocal());
        _updateStatement.setLong(10, entity.getInUseCount());
        _updateStatement.setString(11, entity.getFullPath());

        affectedRowCnt = _updateStatement.executeUpdate();
        return affectedRowCnt;
    }

    public int insertFile(DirectoryEntity entity) throws SQLException {
        int affectedRowCnt = 0;

        _insertStatement.setString(1, entity.getFullPath());
        _insertStatement.setString(2, entity.getParentFolderFullPath());
        _insertStatement.setString(3, entity.getEntryName());
        _insertStatement.setInt(4, entity.getType());
        _insertStatement.setLong(5, entity.getSize());
        _insertStatement.setLong(6, getEpochTime(entity.getMtime()));
        _insertStatement.setLong(7, getEpochTime(entity.getAtime()));
        _insertStatement.setInt(8, entity.isLocked());
        _insertStatement.setInt(9, entity.isModified());
        _insertStatement.setInt(10, entity.isLocal());
        _insertStatement.setLong(11, entity.getInUseCount());

        entity.setMtime(roundTimestamp(entity.getMtime()));
        entity.setAtime(roundTimestamp(entity.getAtime()));

        affectedRowCnt = _insertStatement.executeUpdate();
        return affectedRowCnt;
    }

    public int deleteFile(String fullPath) throws SQLException {
        int affectedRowCnt = 0;

        _deleteStatement.setString(1, fullPath);
        affectedRowCnt = _deleteStatement.executeUpdate();

        return affectedRowCnt;
    }

    private void connect(String dbFile) throws ClassNotFoundException, SQLException {
        if (_connection == null || _connection.isClosed()) {
            Class.forName("org.sqlite.JDBC");
            _connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
        }
    }

    private void createDirectoryTable() throws SQLException {
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
                " is_local integer,\n" +
                " in_use_count integer);";

        executeSql(sql);
    }

    private void createLockTable() throws SQLException {
        String sql = "create table if not exists LOCK (dummy char(1));";

        executeSql(sql);
    }

    private void executeSql(String sql) throws SQLException {

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

    private void prepareStatements() throws SQLException {

        _queryFileStatement =
                _connection.prepareStatement("SELECT   full_path\n" +
                                            "       , parent_folder_full_path\n" +
                                            "       , entry_name\n" +
                                            "       , type\n" +
                                            "       , size\n" +
                                            "       , mtime\n" +
                                            "       , atime\n" +
                                            "       , is_locked\n" +
                                            "       , is_modified\n" +
                                            "       , is_local\n" +
                                            "       , in_use_count\n" +
                                            "  FROM directory\n" +
                                            " WHERE full_path = ?;");

        _traverseDirectoryStatement =
                _connection.prepareStatement("SELECT   full_path\n" +
                                             "       , parent_folder_full_path\n" +
                                             "       , entry_name\n" +
                                             "       , type\n" +
                                             "       , size\n" +
                                             "       , mtime\n" +
                                             "       , atime\n" +
                                             "       , is_locked\n" +
                                             "       , is_modified\n" +
                                             "       , is_local\n" +
                                             "       , in_use_count\n" +
                                             "  FROM directory\n" +
                                             " WHERE parent_folder_full_path = ?;");

        _lockStatement = _connection.prepareStatement("UPDATE lock SET dummy = 1;");

        _insertStatement = _connection.prepareStatement
                ("INSERT INTO directory\n" +
                "( full_path\n" +
                ", parent_folder_full_path\n" +
                ", entry_name\n" +
                ", type\n" +
                ", size\n" +
                ", mtime\n" +
                ", atime\n" +
                ", is_locked\n" +
                ", is_modified\n" +
                ", is_local\n" +
                ", in_use_count)\n" +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?);");

        _updateStatement = _connection.prepareStatement
                ("UPDATE directory\n" +
                        "SET parent_folder_full_path = ?\n" +
                        ", entry_name = ?\n" +
                        ", type = ?\n" +
                        ", size = ?\n" +
                        ", mtime = ?\n" +
                        ", atime = ?\n" +
                        ", is_locked = ?\n" +
                        ", is_modified = ?\n" +
                        ", is_local = ?\n" +
                        ", in_use_count = ?\n" +
                        "WHERE full_path = ?;");

        _deleteStatement = _connection.prepareStatement("DELETE FROM directory WHERE full_path = ?;");
    }

    private DirectoryEntity populateDirectoryEntity(ResultSet rs) throws SQLException {
        DirectoryEntity entity = new DirectoryEntity();
        entity.setFullPath(rs.getString("full_path"));
        entity.setParentFolderFullPath(rs.getString("parent_folder_full_path"));
        entity.setEntryName(rs.getString("entry_name"));
        entity.setType(rs.getInt("type"));
        entity.setSize(rs.getLong("size"));
        entity.setMtime(getTimestamp(rs.getLong("mtime")));
        entity.setAtime(getTimestamp(rs.getLong("atime")));
        entity.setLocked(rs.getInt("is_locked"));
        entity.setModified(rs.getInt("is_modified"));
        entity.setLocal(rs.getInt("is_local"));
        entity.setInUseCount(rs.getInt("in_use_count"));
        return entity;
    }

    private long getEpochTime(Timestamp ts)
    {
        return ts.getTime() / 1000;
    }

    private Timestamp getTimestamp(long epochTime)
    {
        return new Timestamp(epochTime * 1000);
    }

    private Timestamp roundTimestamp(Timestamp ts)
    {
        return new Timestamp(getEpochTime(ts) * 1000);
    }
}
