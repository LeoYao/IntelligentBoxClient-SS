package intelligentBoxClient.ss.dao;

import intelligentBoxClient.ss.dao.pojo.DirectoryEntity;
import intelligentBoxClient.ss.dao.pojo.LruEntity;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by yaohx on 3/29/2016.
 */
public interface IDirectoryDbContext extends ISqliteContext {

    DirectoryEntity querySingleEntry(String fullPath) throws SQLException;
    List<DirectoryEntity> queryEntries(String parentFolderFullPath) throws SQLException;
    List<DirectoryEntity> queryChangedEntries() throws SQLException;
    long queryDiskUsage() throws SQLException;
    int updateEntry(DirectoryEntity entry) throws SQLException;
    int insertEntry(DirectoryEntity entry) throws SQLException;
    int deleteEntry(String fullPath) throws SQLException;

    LruEntity findLru(String curr, boolean createTransaction);
    LruEntity popLru(boolean createTransaction);
    LruEntity peekLru(boolean createTransaction);
    LruEntity pushLru(String path, boolean createTransaction);
    boolean removeLru(String path, boolean createTransaction);
}
