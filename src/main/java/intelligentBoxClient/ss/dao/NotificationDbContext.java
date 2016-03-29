package intelligentBoxClient.ss.dao;

import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by yaohx on 3/29/2016.
 */
@Repository
public class NotificationDbContext extends SqliteContext implements INotificationDbContext {

    private PreparedStatement _queryRemoteChanged;
    private PreparedStatement _resetRemoteChanged;
    private PreparedStatement _setRemoteChanged;
    private PreparedStatement _queryAllPendingDownloads;
    private PreparedStatement _queryPendingDownload;
    private PreparedStatement _insertPendingDownload;
    private PreparedStatement _deletePendingDownload;

    public boolean isRemoteChanged() throws SQLException, InterruptedException {
        boolean is_changed = false;

        ResultSet rs = null;
        try {
            beginTransaction(3,500);
            rs = _queryRemoteChanged.executeQuery();
            if (rs.next()) {
                is_changed = (rs.getInt("is_changed") != 0);
            }
            if (is_changed) {
                _resetRemoteChanged.executeUpdate();
            }
            commitTransaction();
        }
        finally {
            if (rs != null && !rs.isClosed()) {
                rs.close();
            }
        }
        return is_changed;
    }

    public void setRemoteChanged() throws SQLException, InterruptedException {
        beginTransaction(3,500);
        _setRemoteChanged.executeUpdate();
        commitTransaction();
    }

    public List<String> queryAllPendingDownloads() throws SQLException {
        List<String> results = new LinkedList<>();
        ResultSet rs = null;

        try {
            rs = _queryAllPendingDownloads.executeQuery();
            while(rs.next())
            {
                results.add(rs.getString("full_path"));
            }
        }
        finally {
            if (rs != null && !rs.isClosed()){
                rs.close();
            }
        }

        return results;
    }

    public boolean isPendingDownloading(String fullPath) throws SQLException {
        boolean result = false;
        ResultSet rs = null;

        try {
            _queryPendingDownload.setString(1, fullPath);
            rs = _queryPendingDownload.executeQuery();
            result = rs.next();
        }
        finally {
            if (rs != null && !rs.isClosed()){
                rs.close();
            }
        }

        return result;
    }

    public int insertPendingDownload(String fullPath) throws SQLException{
        int affectedRowCount = 0;
        _insertPendingDownload.setString(1, fullPath);
        affectedRowCount = _insertPendingDownload.executeUpdate();
        return affectedRowCount;
    }

    public int deletePendingDownload(String fullPath) throws SQLException{
        int affectedRowCount = 0;
        _deletePendingDownload.setString(1, fullPath);
        affectedRowCount = _deletePendingDownload.executeUpdate();
        return affectedRowCount;
    }

    @Override
    protected boolean prepareStatements() {
        try {
            _queryRemoteChanged = _connection.prepareStatement("SELECT is_changed FROM REMOTE_CHANGED;");
            _resetRemoteChanged = _connection.prepareStatement("UPDATE REMOTE_CHANGED SET is_changed = 0;");
            _setRemoteChanged = _connection.prepareStatement("UPDATE REMOTE_CHANGED SET is_changed = 1;");
            _queryAllPendingDownloads = _connection.prepareStatement("SELECT full_path FROM PENDING_DOWNLOAD;");
            _queryPendingDownload
                    = _connection.prepareStatement("SELECT full_path FROM PENDING_DOWNLOAD WHERE full_path = ?;");
            _insertPendingDownload
                    = _connection.prepareStatement("INSERT INTO PENDING_DOWNLOAD (full_path) VALUES (?);");
            _deletePendingDownload
                    = _connection.prepareStatement("DELETE FROM PENDING_DOWNLOAD WHERE full_path = ?;");
        }
        catch (SQLException e) {
            logger.error("Failed to prepare statements.", e);
            return false;
        }
        return true;
    }

    @Override
    protected boolean disposeStatements() {
        try {
            if (_queryRemoteChanged != null && !_queryRemoteChanged.isClosed()) {
                _queryRemoteChanged.close();
            }
            if (_resetRemoteChanged != null && !_resetRemoteChanged.isClosed()) {
                _resetRemoteChanged.close();
            }
            if (_setRemoteChanged != null && !_setRemoteChanged.isClosed()) {
                _setRemoteChanged.close();
            }
            if (_queryAllPendingDownloads != null && !_queryAllPendingDownloads.isClosed()) {
                _queryAllPendingDownloads.close();
            }
            if (_queryPendingDownload != null && !_queryPendingDownload.isClosed()) {
                _queryPendingDownload.close();
            }
            if (_insertPendingDownload != null && !_insertPendingDownload.isClosed()) {
                _insertPendingDownload.close();
            }
            if (_deletePendingDownload != null && !_deletePendingDownload.isClosed()) {
                _deletePendingDownload.close();
            }
        }
        catch (SQLException e) {
            logger.error("Failed to dispose statements.", e);
            return false;
        }
        return true;
    }

    @Override
    protected boolean initDb() {

        try {
            createRemoteChangedTable();
            initRemoteChangedTable();
            createPendingDownload();
        } catch (SQLException e) {
            logger.error("Failed to initialized DB.", e);
            return false;
        }
        return true;
    }

    private void createRemoteChangedTable() throws SQLException {
        String sql = "create table if not exists REMOTE_CHANGED \n" +
                "(is_changed integer);";

        executeSql(sql);
    }

    private void initRemoteChangedTable() throws SQLException {
        String qSql = "SELECT is_changed FROM REMOTE_CHANGED;";
        String iSql = "INSERT INTO REMOTE_CHANGED (is_changed) VALUES (0);";
        Statement stmt = null;
        ResultSet rs;
        try {
            stmt = _connection.createStatement();
            rs = stmt.executeQuery(qSql);
            if (!rs.next())
            {
                executeSql(iSql);
            }
        } finally {
            if (stmt != null && !stmt.isClosed()) {
                stmt.close();
            }
        }
    }

    private void createPendingDownload() throws SQLException {
        String sql = "create table if not exists PENDING_DOWNLOAD \n" +
                "(full_path varchar(4000) PRIMARY KEY);";

        executeSql(sql);
    }
}
