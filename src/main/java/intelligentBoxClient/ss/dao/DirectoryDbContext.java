package intelligentBoxClient.ss.dao;

import intelligentBoxClient.ss.bootstrapper.IConfiguration;
import intelligentBoxClient.ss.dao.pojo.DirectoryEntity;
import intelligentBoxClient.ss.dao.pojo.LruEntity;
import intelligentBoxClient.ss.utils.Consts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by yaohx on 3/29/2016.
 */
@Repository
public class DirectoryDbContext extends SqliteContext implements IDirectoryDbContext {

    public static final String HEAD = ".head";
    public static final String TAIL = ".tail";

    private PreparedStatement _queryFileStatement;
    private PreparedStatement _traverseDirectoryStatement;
    private PreparedStatement _insertStatement;
    private PreparedStatement _updateStatement;
    private PreparedStatement _queryChangesStatement;
    private PreparedStatement _deleteStatement;
    private PreparedStatement _queryDiskUsageStatement;

    private PreparedStatement _selectLruStatement;
    private PreparedStatement _updateLruStatement;
    private PreparedStatement _insertLruStatement;
    private PreparedStatement _deleteLruStatement;

    @Autowired
    public DirectoryDbContext(IConfiguration configuration){
        super(configuration);
    }

    @Override
    public synchronized DirectoryEntity querySingleEntry(String fullPath) throws SQLException {
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

    @Override
    public synchronized List<DirectoryEntity> queryEntries(String parentFolderFullPath) throws SQLException {
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

    @Override
    public synchronized List<DirectoryEntity> queryChangedEntries() throws SQLException {
        List<DirectoryEntity> results = new LinkedList<DirectoryEntity>();
        ResultSet rs = null;

        try {
            rs = _queryChangesStatement.executeQuery();
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

    @Override
    public synchronized long queryDiskUsage() throws SQLException{
        long result = 0;
        ResultSet rs = null;

        try{
            rs = _queryDiskUsageStatement.executeQuery();
            if (rs.next()){
                result = rs.getLong(1);
            }
        } finally {
            if (rs != null && !rs.isClosed()) {
                rs.close();
            }
        }

        return result;
    }

    @Override
    public synchronized int updateEntry(DirectoryEntity entity) throws SQLException {
        int affectedRowCnt = 0;

        _updateStatement.setString(1, entity.getParentFolderFullPath());
        _updateStatement.setString(2, entity.getEntryName());
        _updateStatement.setString(3, entity.getOldFullPath());
        _updateStatement.setInt(4, entity.getType());
        _updateStatement.setLong(5, entity.getSize());
        _updateStatement.setLong(6, getEpochTime(entity.getMtime()));
        _updateStatement.setLong(7, getEpochTime(entity.getAtime()));
        _updateStatement.setInt(8, entity.isLocked() ? Consts.YES : Consts.NO);
        _updateStatement.setInt(9, entity.isModified() ? Consts.YES : Consts.NO);
        _updateStatement.setInt(10, entity.isLocal() ? Consts.YES : Consts.NO);
        _updateStatement.setInt(11, entity.isDeleted() ? Consts.YES : Consts.NO);
        _updateStatement.setLong(12, entity.getInUseCount());
        _updateStatement.setString(13, entity.getRevision());
        _updateStatement.setString(14, entity.getFullPath());

        affectedRowCnt = _updateStatement.executeUpdate();
        return affectedRowCnt;
    }

    @Override
    public synchronized int insertEntry(DirectoryEntity entity) throws SQLException {
        int affectedRowCnt = 0;

        _insertStatement.setString(1, entity.getFullPath());
        _insertStatement.setString(2, entity.getParentFolderFullPath());
        _insertStatement.setString(3, entity.getEntryName());
        _insertStatement.setString(4, entity.getOldFullPath());
        _insertStatement.setInt(5, entity.getType());
        _insertStatement.setLong(6, entity.getSize());
        _insertStatement.setLong(7, getEpochTime(entity.getMtime()));
        _insertStatement.setLong(8, getEpochTime(entity.getAtime()));
        _insertStatement.setInt(9, entity.isLocked() ? Consts.YES : Consts.NO);
        _insertStatement.setInt(10, entity.isModified() ? Consts.YES : Consts.NO);
        _insertStatement.setInt(11, entity.isLocal() ? Consts.YES : Consts.NO);
        _insertStatement.setInt(12, entity.isDeleted() ? Consts.YES : Consts.NO);
        _insertStatement.setLong(13, entity.getInUseCount());
        _insertStatement.setString(14, entity.getRevision());

        entity.setMtime(roundTimestamp(entity.getMtime()));
        entity.setAtime(roundTimestamp(entity.getAtime()));

        affectedRowCnt = _insertStatement.executeUpdate();
        return affectedRowCnt;
    }

    @Override
    public synchronized int deleteEntry(String fullPath) throws SQLException {
        int affectedRowCnt = 0;

        _deleteStatement.setString(1, fullPath);
        affectedRowCnt = _deleteStatement.executeUpdate();

        return affectedRowCnt;
    }

    @Override
    public synchronized LruEntity popLru(boolean createTransaction){
        LruEntity result = null;
        boolean inTransaction = false;
        boolean inError = true;
        try {
            if (createTransaction) {
                inTransaction = beginTransaction();
            }

            LruEntity header = queryLru(HEAD);
            if (header == null || header.getNext().equals(TAIL)) {
                return null;
            }

            result = queryLru(header.getNext());
            header.setNext(result.getNext());
            LruEntity next = queryLru(result.getNext());
            next.setPrev(HEAD);

            updateLru(header);
            updateLru(next);
            deleteLru(result.getCurr());
            inError = false;
        } catch (SQLException e){
            logger.error("Falied to pop.", e);
            result = null;
        }
        finally {
            if (inTransaction){
                if(inError){
                    rollbackTransaction();
                } else {
                    commitTransaction();
                }
            }
        }

        return result;
    }

    @Override
    public synchronized LruEntity pushLru(String path, boolean createTransaction) {
        LruEntity result = null;
        boolean inTransaction = false;
        boolean inError = true;

        try{
            if (createTransaction) {
                inTransaction = beginTransaction();
            }

            result = queryLru(path);
            if (result == null){
                result = new LruEntity();
                result.setCurr(path);
                insertLru(result);
            } else {
                LruEntity prev = queryLru(result.getPrev());
                LruEntity next = queryLru(result.getNext());
                prev.setNext(next.getCurr());
                next.setPrev(prev.getCurr());
                updateLru(prev);
                updateLru(next);
            }

            LruEntity tail = queryLru(TAIL);
            LruEntity prevTail = queryLru(tail.getPrev());
            result.setNext(TAIL);
            result.setPrev(prevTail.getCurr());
            tail.setPrev(result.getCurr());
            prevTail.setNext(result.getCurr());

            updateLru(result);
            updateLru(tail);
            updateLru(prevTail);

            inError = false;

        } catch (SQLException e){
            logger.error("Falied to pop.", e);
            result = null;
        }
        finally {
            if (inTransaction){
                if(inError){
                    rollbackTransaction();
                } else {
                    commitTransaction();
                }
            }
        }

        return result;
    }


    @Override
    public synchronized boolean removeLru(String path, boolean createTransaction){
        LruEntity toRemove = null;
        boolean inTransaction = false;
        boolean inError = true;

        try{
            if (createTransaction){
                inTransaction = beginTransaction();
            }
            toRemove = queryLru(path);
            if (toRemove != null) {
                LruEntity prev = queryLru(toRemove.getPrev());
                LruEntity next = queryLru(toRemove.getNext());
                prev.setNext(next.getCurr());
                next.setPrev(prev.getCurr());
                updateLru(prev);
                updateLru(next);
                deleteLru(toRemove.getCurr());
            }
            inError = false;
        } catch (SQLException e){
            logger.error("Falied to pop.", e);
        }
        finally {
            if (inTransaction){
                if(inError){
                    rollbackTransaction();
                } else {
                    commitTransaction();
                }
            }
        }

        return !inError;

    }

    @Override
    protected boolean prepareStatements(){

        try {
            _queryFileStatement =
                    _connection.prepareStatement("SELECT   full_path\n" +
                            "       , parent_folder_full_path\n" +
                            "       , entry_name\n" +
                            "       , old_full_path\n" +
                            "       , type\n" +
                            "       , size\n" +
                            "       , mtime\n" +
                            "       , atime\n" +
                            "       , is_locked\n" +
                            "       , is_modified\n" +
                            "       , is_local\n" +
                            "       , is_deleted\n" +
                            "       , in_use_count\n" +
                            "       , revision\n" +
                            "  FROM directory\n" +
                            " WHERE full_path = ?;");

            _traverseDirectoryStatement =
                    _connection.prepareStatement("SELECT   full_path\n" +
                            "       , parent_folder_full_path\n" +
                            "       , entry_name\n" +
                            "       , old_full_path\n" +
                            "       , type\n" +
                            "       , size\n" +
                            "       , mtime\n" +
                            "       , atime\n" +
                            "       , is_locked\n" +
                            "       , is_modified\n" +
                            "       , is_local\n" +
                            "       , is_deleted\n" +
                            "       , in_use_count\n" +
                            "       , revision\n" +
                            "  FROM directory\n" +
                            " WHERE parent_folder_full_path = ? \n" +
                            "  ORDER BY full_path ASC;");

            _queryChangesStatement =
                    _connection.prepareStatement(
                            "SELECT   full_path\n" +
                            "       , parent_folder_full_path\n" +
                            "       , entry_name\n" +
                            "       , old_full_path\n" +
                            "       , type\n" +
                            "       , size\n" +
                            "       , mtime\n" +
                            "       , atime\n" +
                            "       , is_locked\n" +
                            "       , is_modified\n" +
                            "       , is_local\n" +
                            "       , is_deleted\n" +
                            "       , in_use_count\n" +
                            "       , revision\n" +
                            "  FROM directory\n" +
                            " WHERE is_modified = 1\n" +
                            " ORDER BY full_path DESC;");

            _queryDiskUsageStatement =
                    _connection.prepareStatement(
                            "SELECT SUM(size)\n" +
                            "  FROM directory\n" +
                            " WHERE is_local = 1 AND type = 2\n" +
                            " ORDER BY full_path DESC;");

            _insertStatement = _connection.prepareStatement(
                            "INSERT INTO directory\n" +
                            "( full_path\n" +
                            ", parent_folder_full_path\n" +
                            ", entry_name\n" +
                            ", old_full_path\n" +
                            ", type\n" +
                            ", size\n" +
                            ", mtime\n" +
                            ", atime\n" +
                            ", is_locked\n" +
                            ", is_modified\n" +
                            ", is_local\n" +
                            ", is_deleted\n" +
                            ", in_use_count\n" +
                            ", revision)\n" +
                            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?);");

            _updateStatement = _connection.prepareStatement(
                            "UPDATE directory\n" +
                            "SET parent_folder_full_path = ?\n" +
                            ", entry_name = ?\n" +
                            ", old_full_path = ?\n" +
                            ", type = ?\n" +
                            ", size = ?\n" +
                            ", mtime = ?\n" +
                            ", atime = ?\n" +
                            ", is_locked = ?\n" +
                            ", is_modified = ?\n" +
                            ", is_local = ?\n" +
                            ", is_deleted = ?\n" +
                            ", in_use_count = ?\n" +
                            ", revision = ?\n" +
                            "WHERE full_path = ?;");

            _deleteStatement = _connection.prepareStatement("DELETE FROM directory WHERE full_path = ?;");

            _selectLruStatement =
                    _connection.prepareStatement("SELECT   curr\n" +
                            "       , prev\n" +
                            "       , next\n" +
                            "  FROM lru_queue\n" +
                            " WHERE curr = ?;");

            _insertLruStatement = _connection.prepareStatement
                    ("INSERT INTO lru_queue\n" +
                            "( curr\n" +
                            ", prev\n" +
                            ", next)\n" +
                            "VALUES (?,?,?);");

            _updateLruStatement = _connection.prepareStatement
                    ("UPDATE lru_queue\n" +
                            "SET prev = ?\n" +
                            ", next = ?\n" +
                            "WHERE curr = ?;");

            _deleteLruStatement = _connection.prepareStatement("DELETE FROM lru_queue WHERE curr = ?;");
        }
        catch (SQLException e)
        {
            logger.error("Failed to prepare statements.", e);
            return false;
        }

        return true;

    }

    @Override
    protected boolean disposeStatements() {
        try {
            if (_queryFileStatement != null && !_queryFileStatement.isClosed()) {
                _queryFileStatement.close();
            }
            if (_traverseDirectoryStatement != null && !_traverseDirectoryStatement.isClosed()) {
                _traverseDirectoryStatement.close();
            }
            if (_queryChangesStatement != null && !_queryChangesStatement.isClosed()) {
                _queryChangesStatement.close();
            }
            if (_insertStatement != null && !_insertStatement.isClosed()) {
                _insertStatement.close();
            }
            if (_updateStatement != null && !_updateStatement.isClosed()) {
                _updateStatement.close();
            }
            if (_deleteStatement != null && !_deleteStatement.isClosed()) {
                _deleteStatement.close();
            }
            if (_queryDiskUsageStatement != null && !_queryDiskUsageStatement.isClosed()){
                _queryDiskUsageStatement.close();
            }

            if (_selectLruStatement != null && !_selectLruStatement.isClosed()) {
                _selectLruStatement.close();
            }
            if (_insertLruStatement != null && !_insertLruStatement.isClosed()) {
                _insertLruStatement.close();
            }
            if (_updateLruStatement != null && !_updateLruStatement.isClosed()) {
                _updateLruStatement.close();
            }
            if (_deleteLruStatement != null && !_deleteLruStatement.isClosed()) {
                _deleteLruStatement.close();
            }
        }
        catch (SQLException e)
        {
            logger.error("Failed to close statements.", e);
            return false;
        }

        return true;
    }

    @Override
    protected boolean initDb()
    {
        try {
            createDirectoryTable();
            createLruTable();
        }
        catch (SQLException e)
        {
            logger.error("Failed to initialized DB.", e);
            return false;
        }

        return true;
    }

    private DirectoryEntity populateDirectoryEntity(ResultSet rs) throws SQLException {
        DirectoryEntity entity = new DirectoryEntity();
        entity.setFullPath(rs.getString("full_path"));
        entity.setParentFolderFullPath(rs.getString("parent_folder_full_path"));
        entity.setEntryName(rs.getString("entry_name"));
        entity.setOldFullPath(rs.getString("old_full_path"));
        entity.setType(rs.getInt("type"));
        entity.setSize(rs.getLong("size"));
        entity.setMtime(getTimestamp(rs.getLong("mtime")));
        entity.setAtime(getTimestamp(rs.getLong("atime")));
        entity.setLocked(rs.getInt("is_locked") == Consts.YES);
        entity.setModified(rs.getInt("is_modified") == Consts.YES);
        entity.setLocal(rs.getInt("is_local") == Consts.YES);
        entity.setDeleted(rs.getInt("is_deleted") == Consts.YES);
        entity.setInUseCount(rs.getInt("in_use_count"));
        entity.setRevision(rs.getString("revision"));
        return entity;
    }


    private void createDirectoryTable() throws SQLException {
        String sql = "create table if not exists DIRECTORY \n" +
                "(full_path varchar(4000) PRIMARY KEY,\n" +
                " parent_folder_full_path varchar(4000), \n" +
                " entry_name varchar(255),\n" +
                " old_full_path varchar(4000),\n" +
                " type integer,\n" +
                " size integer,\n" +
                " mtime datetime,\n" +
                " atime datetime,\n" +
                " is_locked integer,\n" +
                " is_modified integer,\n" +
                " is_local integer,\n" +
                " is_deleted integer,\n" +
                " in_use_count integer,\n" +
                " revision varchar(100));";

        executeSql(sql);

        sql = "create index if not exists IDX_IS_MODIFIED\n" +
                "on DIRECTORY (is_modified);";
        executeSql(sql);

        sql = "create index if not exists IDX_IS_LOCAL\n" +
                "on DIRECTORY (is_local);";
        executeSql(sql);
    }

    private void createLruTable() throws SQLException {
        String sql =
                "CREATE TABLE if not exists lru_queue \n" +
                "(curr varchar(4000) PRIMARY KEY,\n" +
                " prev varchar(4000), \n" +
                " next varchar(4000));";
        executeSql(sql);

        sql = "insert or ignore into lru_queue (curr, next) values('.head', '.tail');";
        executeSql(sql);

        sql = "insert or ignore into lru_queue (curr, prev) values('.tail', '.head');";
        executeSql(sql);
    }


    private LruEntity queryLru(String curr) throws SQLException {
        _selectLruStatement.setString(1, curr);

        ResultSet rs = null;
        LruEntity result = null;
        try {
            rs = _selectLruStatement.executeQuery();
            if (rs.next()){
                result = new LruEntity();
                result.setCurr(rs.getString("curr"));
                result.setPrev(rs.getString("prev"));
                result.setNext(rs.getString("next"));
            }
        }
        finally {
            if (rs != null && !rs.isClosed()){
                rs.close();
            }
        }

        return result;
    }

    private int insertLru(LruEntity entity) throws SQLException {
        int affectedRowCnt = 0;

        _insertLruStatement.setString(1, entity.getCurr());
        _insertLruStatement.setString(2, entity.getPrev());
        _insertLruStatement.setString(3, entity.getNext());

        affectedRowCnt = _insertLruStatement.executeUpdate();

        return affectedRowCnt;
    }

    private int updateLru(LruEntity entity) throws SQLException {
        int affectedRowCnt = 0;

        _updateLruStatement.setString(1, entity.getPrev());
        _updateLruStatement.setString(2, entity.getNext());
        _updateLruStatement.setString(3, entity.getCurr());

        affectedRowCnt = _updateLruStatement.executeUpdate();

        return affectedRowCnt;
    }

    private int deleteLru(String curr) throws SQLException {
        int affectedRowCnt = 0;

        _deleteLruStatement.setString(1, curr);

        affectedRowCnt = _deleteLruStatement.executeUpdate();

        return affectedRowCnt;
    }
}
