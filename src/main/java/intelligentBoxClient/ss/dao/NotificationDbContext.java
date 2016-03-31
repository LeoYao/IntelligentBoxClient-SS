package intelligentBoxClient.ss.dao;

import intelligentBoxClient.ss.bootstrapper.IConfiguration;
import intelligentBoxClient.ss.dao.pojo.RemoteChangeEntity;
import org.springframework.beans.factory.annotation.Autowired;
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
    private PreparedStatement _updatePendingDownload;

    @Autowired
    public NotificationDbContext(IConfiguration configuration){
        super(configuration);
    }

    public boolean isRemoteChanged() throws SQLException {
        boolean isChanged = false;

        ResultSet rs = null;
        boolean inTransaction = false;
        try {
            inTransaction = beginTransaction();
            rs = _queryRemoteChanged.executeQuery();
            if (rs.next()) {
                isChanged = (rs.getInt("is_changed") != 0);
            }
            if (isChanged) {
                _resetRemoteChanged.executeUpdate();
            }
            if (inTransaction) {
                commitTransaction();
            }
        }
        finally {
            if (rs != null && !rs.isClosed()) {
                rs.close();
            }

            if (inTransaction){
                rollbackTransaction();
            }
        }
        return isChanged;
    }

    public void setRemoteChanged() throws SQLException {
        _setRemoteChanged.executeUpdate();
    }

    public List<RemoteChangeEntity> queryAllPendingRemoteChanges() throws SQLException {
        List<RemoteChangeEntity> results = new LinkedList<>();
        ResultSet rs = null;

        try {
            rs = _queryAllPendingDownloads.executeQuery();
            while(rs.next())
            {
                RemoteChangeEntity entity
                        = new RemoteChangeEntity(rs.getString("full_path"),
                                                rs.getString("entry_name"),
                                                rs.getInt("is_deleted") == 1);
                results.add(entity);
            }
        }
        finally {
            if (rs != null && !rs.isClosed()){
                rs.close();
            }
        }

        return results;
    }

    public RemoteChangeEntity queryPendingRemoteChanges(String fullPath) throws SQLException {

        ResultSet rs = null;

        try {
            _queryPendingDownload.setString(1, fullPath);
            rs = _queryPendingDownload.executeQuery();
            if (rs.next())
            {
                RemoteChangeEntity entity
                        = new RemoteChangeEntity(rs.getString("full_path"),
                                                rs.getString("entry_name"),
                                                rs.getInt("is_deleted") == 1);
                return entity;
            }
            else {
                return null;
            }
        }
        finally {
            if (rs != null && !rs.isClosed()){
                rs.close();
            }
        }
    }

    public int insertPendingRemoteChanges(RemoteChangeEntity entity) throws SQLException{
        int affectedRowCount = 0;
        _insertPendingDownload.setString(1, entity.getFullPath());
        _insertPendingDownload.setString(2, entity.getEntryName());
        _insertPendingDownload.setInt(3, entity.isDeleted() ? 1 : 0);
        affectedRowCount = _insertPendingDownload.executeUpdate();
        return affectedRowCount;
    }

    public int deletePendingRemoteChanges(String fullPath) throws SQLException{
        int affectedRowCount = 0;
        _deletePendingDownload.setString(1, fullPath);
        affectedRowCount = _deletePendingDownload.executeUpdate();
        return affectedRowCount;
    }

    public int updatePendingRemoteChanges(RemoteChangeEntity entity) throws SQLException{
        int affectedRowCount = 0;
        _updatePendingDownload.setInt(1, entity.isDeleted() ? 1 : 0);
        _updatePendingDownload.setString(2, entity.getEntryName());
        _updatePendingDownload.setString(3, entity.getFullPath());
        affectedRowCount = _updatePendingDownload.executeUpdate();
        return affectedRowCount;
    }

    @Override
    protected boolean prepareStatements() {
        try {
            _queryRemoteChanged = _connection.prepareStatement("SELECT is_changed FROM REMOTE_CHANGED;");
            _resetRemoteChanged = _connection.prepareStatement("UPDATE REMOTE_CHANGED SET is_changed = 0;");
            _setRemoteChanged = _connection.prepareStatement("UPDATE REMOTE_CHANGED SET is_changed = 1;");
            _queryAllPendingDownloads
                    = _connection.prepareStatement("SELECT full_path, entry_name, is_deleted FROM PENDING_REMOTE_CHANGES;");
            _queryPendingDownload
                    = _connection.prepareStatement("SELECT full_path, entry_name, is_deleted FROM PENDING_REMOTE_CHANGES WHERE full_path = ?;");
            _insertPendingDownload
                    = _connection.prepareStatement("INSERT INTO PENDING_REMOTE_CHANGES (full_path, entry_name, is_deleted) VALUES (?, ?, ?);");
            _deletePendingDownload
                    = _connection.prepareStatement("DELETE FROM PENDING_REMOTE_CHANGES WHERE full_path = ?;");
            _updatePendingDownload
                    = _connection.prepareStatement("UPDATE PENDING_REMOTE_CHANGES SET is_deleted = ?, entry_name = ? WHERE full_path = ?;");
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
            if (_updatePendingDownload != null && !_updatePendingDownload.isClosed()) {
                _updatePendingDownload.close();
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
            createPendingRemoteChanges();
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

    private void createPendingRemoteChanges() throws SQLException {
        String sql = "create table if not exists PENDING_REMOTE_CHANGES \n" +
                "(full_path varchar(4000) PRIMARY KEY,\n" +
                "entry_name varchar(255),\n" +
                " is_deleted integer);";

        executeSql(sql);
    }
}
