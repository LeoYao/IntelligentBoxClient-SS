package intelligentBoxClient.ss.persistence;

import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import intelligentBoxClient.ss.dao.IDirectoryDbContext;
import intelligentBoxClient.ss.dao.pojo.DirectoryEntity;
import intelligentBoxClient.ss.utils.Consts;
import intelligentBoxClient.ss.utils.IUtils;
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
    private IUtils _utils;

    @Autowired
    public DirectoryDbSaver(IDirectoryDbContext directoryDbContext, IUtils utils) {
        _directoryDbContext = directoryDbContext;
        logger = LogFactory.getLog(this.getClass());
        _utils = utils;
    }

    public boolean save(FileMetadata metadata) {
        boolean inTransaction = false;
        try {
            String fullPath = metadata.getPathLower();
            String name = metadata.getName().toLowerCase();
            String parentPath = _utils.extractParentFolderPath(fullPath, name);
            inTransaction = _directoryDbContext.beginTransaction();
            DirectoryEntity entity = _directoryDbContext.querySingleEntry(fullPath);
            if (entity == null) {
                entity = new DirectoryEntity();
                entity.setFullPath(fullPath);
                entity.setEntryName(name);
                entity.setOldFullPath("");
                entity.setParentFolderFullPath(parentPath);
                entity.setType(Consts.FILE);
                entity.setSize(metadata.getSize());
                entity.setMtime(metadata.getServerModified().getTime());
                entity.setAtime(metadata.getServerModified().getTime());
                entity.setLocked(false);
                entity.setLocal(true);
                entity.setModified(false);
                entity.setDeleted(false);
                entity.setInUseCount(0);
                entity.setRevision(metadata.getRev());

                _directoryDbContext.insertEntry(entity);
            } else {
                entity.setFullPath(fullPath);
                entity.setEntryName(name);
                entity.setParentFolderFullPath(parentPath);
                entity.setType(Consts.FILE);
                entity.setSize(metadata.getSize());
                entity.setMtime(metadata.getServerModified().getTime());
                entity.setAtime(metadata.getServerModified().getTime());
                entity.setRevision(metadata.getRev());

                _directoryDbContext.updateEntry(entity);
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
            DirectoryEntity entity = _directoryDbContext.querySingleEntry(metadata.getPathDisplay());
            String fullPath = metadata.getPathLower();
            String name = metadata.getName().toLowerCase();
            String parentPath = _utils.extractParentFolderPath(fullPath, name);

            if (entity == null) {
                entity = new DirectoryEntity();
                entity.setFullPath(fullPath);
                entity.setEntryName(name);
                entity.setParentFolderFullPath(parentPath);
                entity.setType(Consts.FOLDER);
                entity.setSize(0);
                long now = new Date().getTime();
                entity.setMtime(now);
                entity.setAtime(now);
                entity.setLocked(false);
                entity.setLocal(false);
                entity.setInUseCount(0);

                _directoryDbContext.insertEntry(entity);
            } else {
                entity.setFullPath(fullPath);
                entity.setEntryName(name);
                entity.setParentFolderFullPath(parentPath);
                entity.setType(Consts.FOLDER);
                entity.setSize(0);
                long now = new Date().getTime();
                entity.setMtime(now);
                entity.setAtime(now);

                _directoryDbContext.updateEntry(entity);
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

}

