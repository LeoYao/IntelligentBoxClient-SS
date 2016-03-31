package intelligentBoxClient.ss.dao;

import intelligentBoxClient.ss.bootstrapper.IConfiguration;
import intelligentBoxClient.ss.dao.pojo.DirectoryEntity;
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

    private PreparedStatement _queryFileStatement;
    private PreparedStatement _traverseDirectoryStatement;
    private PreparedStatement _insertStatement;
    private PreparedStatement _updateStatement;
    private PreparedStatement _deleteStatement;

    @Autowired
    public DirectoryDbContext(IConfiguration configuration){
        super(configuration);
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
        _updateStatement.setInt(7, entity.isLocked() ? 1 : 0);
        _updateStatement.setInt(8, entity.isModified() ? 1 : 0);
        _updateStatement.setInt(9, entity.isLocal() ? 1 : 0);
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
        _insertStatement.setInt(8, entity.isLocked() ? 1 : 0);
        _insertStatement.setInt(9, entity.isModified() ? 1 : 0);
        _insertStatement.setInt(10, entity.isLocal() ? 1 : 0);
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

    @Override
    protected boolean prepareStatements(){

        try {
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
            if (_insertStatement != null && !_insertStatement.isClosed()) {
                _insertStatement.close();
            }
            if (_updateStatement != null && !_updateStatement.isClosed()) {
                _updateStatement.close();
            }
            if (_deleteStatement != null && !_deleteStatement.isClosed()) {
                _deleteStatement.close();
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
        entity.setType(rs.getInt("type"));
        entity.setSize(rs.getLong("size"));
        entity.setMtime(getTimestamp(rs.getLong("mtime")));
        entity.setAtime(getTimestamp(rs.getLong("atime")));
        entity.setLocked(rs.getInt("is_locked") == DirectoryEntity.YES);
        entity.setModified(rs.getInt("is_modified")  == DirectoryEntity.YES);
        entity.setLocal(rs.getInt("is_local") == DirectoryEntity.YES);
        entity.setInUseCount(rs.getInt("in_use_count"));
        return entity;
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
}
