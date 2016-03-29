package intelligentBoxClient.ss.dao;

import intelligentBoxClient.ss.dao.pojo.DirectoryEntity;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by yaohx on 3/29/2016.
 */
public interface IDirectoryDbContext extends ISqliteContext {
    DirectoryEntity querySingleFile(String fullPath) throws SQLException;
    List<DirectoryEntity> queryFiles(String parentFolderFullPath) throws SQLException;
    int updateFile(DirectoryEntity entry) throws SQLException;
    int insertFile(DirectoryEntity entry) throws SQLException;
    int deleteFile(String fullPath) throws SQLException;
}
