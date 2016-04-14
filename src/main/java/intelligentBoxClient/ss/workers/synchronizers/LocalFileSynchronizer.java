package intelligentBoxClient.ss.workers.synchronizers;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import intelligentBoxClient.ss.bootstrapper.IConfiguration;
import intelligentBoxClient.ss.dao.IDirectoryDbContext;
import intelligentBoxClient.ss.dao.pojo.DirectoryEntity;
import intelligentBoxClient.ss.dropbox.IDropboxClient;
import intelligentBoxClient.ss.utils.Consts;
import intelligentBoxClient.ss.utils.IUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Leo on 4/3/16.
 */
@Service(value="LocalFileSynchronizer")
public class LocalFileSynchronizer extends FileSynchronizer{

    private IDirectoryDbContext _directoryDbContext;
    private IConfiguration _configuration;
    private IDropboxClient _dropboxClient;
    private Log logger;

    @Autowired
    public LocalFileSynchronizer(IDirectoryDbContext directoryDbContext,
                                 IConfiguration configuration,
                                 IDropboxClient dropboxClient,
                                 IUtils utils){
        super(utils);
        logger = LogFactory.getLog(this.getClass());
        _directoryDbContext = directoryDbContext;
        _configuration = configuration;
        _dropboxClient = dropboxClient;
    }

    /*
     *  Upload/Deleted according to local changed files when the local file is modified
     *
     *  If the local file is locked, skip the file for this time and retry it next time.
     */
    @Override
    public void synchronize() {
        List<DirectoryEntity> changes = getLocalChanges();
        for (DirectoryEntity change : changes){
            synchronizeChange(change);
        }
    }

    private List<DirectoryEntity> getLocalChanges(){
        boolean inTransaction = false;
        List<DirectoryEntity> result = new ArrayList<>();
        try {
            inTransaction = _directoryDbContext.beginTransaction();

            if (!inTransaction){
                logger.warn("Skipped getLocalChanges because transaction is failed to begin.");
            } else {
                result = _directoryDbContext.queryChangedEntries();
            }
        } catch (SQLException e) {
            logger.warn("Failed to get local changes", e);
            return new ArrayList<DirectoryEntity>();
        } finally {
            if(inTransaction){
                _directoryDbContext.rollbackTransaction(); //Nothing is changed.
            }
        }

        return result;
    }

    private void synchronizeChange(DirectoryEntity change){
        if (isInUse(change)){
            logger.debug("Skip synchronizing [" + change.getFullPath() + "] to dropbox because it is in use.");
            return;
        }
        if (change.isDeleted()){
            deleteEntry(change);
        } else {
            addEntry(change);
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            logger.warn("Failed to sleep.");
        }

    }

    private void deleteEntry(DirectoryEntity entity){

        boolean inTransaction = false;
        boolean isError = false;
        try {
            logger.debug("Deleting [" + entity.getFullPath() + "]");
            inTransaction = _directoryDbContext.beginTransaction();

            if (!inTransaction) {
                isError = true;
                logger.warn("Skipped deleting [" + entity.getFullPath() + "] because transaction is failed to begin.");
            } else {
                DirectoryEntity latestEntity = _directoryDbContext.querySingleEntry(entity.getFullPath());
                if (isInUse(latestEntity)) {
                    logger.debug("Skip deleting [" + entity.getFullPath() + "] because it is in use.");
                    return;
                }

                _dropboxClient.deleteFile(entity.getFullPath());

                _directoryDbContext.deleteEntry(entity.getFullPath());

                logger.debug("Deleted [" + entity.getFullPath() + "]");
            }
        } catch (SQLException e) {
            logger.error("Failed to delete file [" + entity.getFullPath() + "]", e);
            isError = true;
            return;
        } catch (DbxException e) {
            logger.warn("Failed to delete file [" + entity.getFullPath() + "]", e);
            isError = true;
            return;
        }
        finally {
            if(inTransaction){
                if (isError) {
                    _directoryDbContext.rollbackTransaction();
                }
                else {
                    _directoryDbContext.commitTransaction();
                }
            }
        }
    }

    private void addEntry(DirectoryEntity entity){
        boolean inTransaction = false;
        boolean isError = false;
        try {
            logger.debug("Adding [" + entity.getFullPath() + "]");
            inTransaction = _directoryDbContext.beginTransaction();
            if (!inTransaction) {
                isError = true;
                logger.warn("Skipped adding [" + entity.getFullPath() + "] because transaction is failed to begin.");
            } else {
                DirectoryEntity latestEntity = _directoryDbContext.querySingleEntry(entity.getFullPath());
                if (isInUse(latestEntity)) {
                    logger.debug("Skipped adding [" + entity.getFullPath() + "] because it is in use.");
                } else {

                    if (latestEntity.getType() == Consts.FILE) {
                        FileMetadata metadata = _dropboxClient.uploadFile(entity.getFullPath(), _configuration.getDataFolderPath() + entity.getFullPath());

                        latestEntity.setSize(metadata.getSize());
                        latestEntity.setModified(false);
                        latestEntity.setRevision(metadata.getRev());

                        _directoryDbContext.updateEntry(latestEntity);

                        logger.debug("Uploaded file [" + entity.getFullPath() + "]");
                    } else {
                        if (_dropboxClient.createFolder(entity.getFullPath())) {
                            latestEntity.setModified(false);
                            _directoryDbContext.updateEntry(latestEntity);
                            logger.debug("Created folder [" + entity.getFullPath() + "]");
                        } else {
                            isError = true;
                            logger.error("Failed to created folder [" + entity.getFullPath() + "]");
                        }
                    }
                }
            }

        } catch (SQLException e) {
            logger.error("Failed to add [" + entity.getFullPath() + "]", e);
            isError = true;
        } catch (DbxException e) {
            logger.warn("Failed to add [" + entity.getFullPath() + "]", e);
            isError = true;
        }
        finally {
            if(inTransaction){
                if (isError) {
                    _directoryDbContext.rollbackTransaction();
                }
                else {
                    _directoryDbContext.commitTransaction();
                }
            }
        }
    }


}
