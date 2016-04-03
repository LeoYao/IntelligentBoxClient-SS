package intelligentBoxClient.ss.persistence;

import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.Metadata;
import intelligentBoxClient.ss.dao.IDirectoryDbContext;
import intelligentBoxClient.ss.dao.pojo.DirectoryEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Date;

/**
 * Created by yaohx on 3/31/2016.
 */
@Service
public class DirectoryDbSaver implements IDirectoryDbSaver {

    private IDirectoryDbContext _directoryDbContext;
    private Log logger;

    @Autowired
    public DirectoryDbSaver(IDirectoryDbContext directoryDbContext) {
        _directoryDbContext = directoryDbContext;
        logger = LogFactory.getLog(this.getClass());
    }

    public boolean save(FileMetadata metadata) {
        boolean inTransaction = false;
        try {
            String fullPath = metadata.getPathLower();
            String name = metadata.getName().toLowerCase();
            String parentPath = extractParentFolderPath(fullPath, name);
            inTransaction = _directoryDbContext.beginTransaction();
            DirectoryEntity entity = _directoryDbContext.querySingleFile(fullPath);
            if (entity == null) {
                entity = new DirectoryEntity();
                entity.setFullPath(fullPath);
                entity.setEntryName(name);
                entity.setOldFullPath("");
                entity.setParentFolderFullPath(parentPath);
                entity.setType(DirectoryEntity.FILE);
                entity.setSize(metadata.getSize());
                entity.setMtime(metadata.getServerModified().getTime());
                entity.setAtime(metadata.getServerModified().getTime());
                entity.setLocked(false);
                entity.setLocal(true);
                entity.setModified(false);
                entity.setDeleted(false);
                entity.setInUseCount(0);

                _directoryDbContext.insertFile(entity);
            } else {
                entity.setFullPath(fullPath);
                entity.setEntryName(name);
                entity.setParentFolderFullPath(parentPath);
                entity.setType(DirectoryEntity.FILE);
                entity.setSize(metadata.getSize());
                entity.setMtime(metadata.getServerModified().getTime());
                entity.setAtime(metadata.getServerModified().getTime());

                _directoryDbContext.updateFile(entity);
            }

            _directoryDbContext.commitTransaction();

        } catch (SQLException e) {
            logger.error(e);
            if (inTransaction) {
                _directoryDbContext.rollbackTransaction();
            }
            return false;
        }
        return true;
    }

    public boolean save(FolderMetadata metadata) {
        boolean inTransaction = false;
        try {
            inTransaction = _directoryDbContext.beginTransaction();
            DirectoryEntity entity = _directoryDbContext.querySingleFile(metadata.getPathDisplay());
            if (entity == null) {
                entity = new DirectoryEntity();
                entity.setFullPath(metadata.getPathDisplay());
                entity.setEntryName(metadata.getName());
                entity.setParentFolderFullPath(extractParentFolderPath(metadata.getPathDisplay(), metadata.getName()));
                entity.setType(DirectoryEntity.FOLDER);
                entity.setSize(0);
                long now = new Date().getTime();
                entity.setMtime(now);
                entity.setAtime(now);
                entity.setLocked(false);
                entity.setLocal(false);
                entity.setInUseCount(0);

                _directoryDbContext.insertFile(entity);
            } else {
                entity.setFullPath(metadata.getPathDisplay());
                entity.setEntryName(metadata.getName());
                entity.setParentFolderFullPath(extractParentFolderPath(metadata.getPathDisplay(), metadata.getName()));
                entity.setType(DirectoryEntity.FOLDER);
                entity.setSize(0);
                long now = new Date().getTime();
                entity.setMtime(now);
                entity.setAtime(now);

                _directoryDbContext.updateFile(entity);
            }

            _directoryDbContext.commitTransaction();

        } catch (SQLException e) {
            logger.error(e);
            if (inTransaction) {
                _directoryDbContext.rollbackTransaction();
            }
            return false;
        }
        return true;
    }

    private String extractParentFolderPath(String fullPath, String fileName) {
        int fullPathLength = fullPath.length();
        int fileNameLength = fileName.length();
        if (fileNameLength == fullPathLength) {
            //root
            return ".";
        }
        return fullPath.substring(0, fullPathLength - fileNameLength - 1);
    }
}

