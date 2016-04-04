package intelligentBoxClient.ss.workers.synchronizers;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;
import intelligentBoxClient.ss.bootstrapper.IConfiguration;
import intelligentBoxClient.ss.dao.IDirectoryDbContext;
import intelligentBoxClient.ss.dao.pojo.DirectoryEntity;
import intelligentBoxClient.ss.dropbox.IDropboxClient;
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
public class LocalFileSynchronizer implements IFileSynchronizer{

    private IDirectoryDbContext _directoryDbContext;
    private IConfiguration _configuration;
    private IDropboxClient _dropboxClient;
    private Log logger;

    @Autowired
    public LocalFileSynchronizer(IDirectoryDbContext directoryDbContext,
                                 IConfiguration configuration,
                                 IDropboxClient dropboxClient){
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
        try {
            return _directoryDbContext.queryChanges();
        } catch (SQLException e) {
            logger.warn("Failed to get local changes", e);
            return new ArrayList<DirectoryEntity>();
        }
    }

    private void synchronizeChange(DirectoryEntity change){
        List<DirectoryEntity> changes = getLocalChanges();

        if (isInUse(change)){
            logger.debug("Skip synchronizing [" + change.getFullPath() + "] to dropbox because it is in use.");
            return;
        }
        if (change.isDeleted()){
            deleteFile(change);
        } else {
            uploadFile(change);
        }

    }

    private void deleteFile(DirectoryEntity entity){

        boolean inTransaction = false;
        boolean isError = false;
        try {
            logger.debug("Deleting [" + entity.getFullPath() + "]");
            inTransaction = _directoryDbContext.beginTransaction();
            DirectoryEntity latestEntity = _directoryDbContext.querySingleFile(entity.getFullPath());
            if (isInUse(latestEntity))
            {
                logger.debug("Skip deleting [" + entity.getFullPath() + "] because it is in use.");
                return;
            }

            _dropboxClient.deleteFile(entity.getFullPath());

            _directoryDbContext.deleteFile(entity.getFullPath());

            logger.debug("Deleted [" + entity.getFullPath() + "]");

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

    private void uploadFile(DirectoryEntity entity){
        boolean inTransaction = false;
        boolean isError = false;
        try {
            logger.debug("Uploading [" + entity.getFullPath() + "]");
            inTransaction = _directoryDbContext.beginTransaction();
            DirectoryEntity latestEntity = _directoryDbContext.querySingleFile(entity.getFullPath());
            if (isInUse(latestEntity))
            {
                logger.debug("Skip uploading [" + entity.getFullPath() + "] because it is in use.");
                return;
            }

            FileMetadata metadata = _dropboxClient.uploadFile(entity.getFullPath(), _configuration.getDataFolderPath() + entity.getFullPath());

            latestEntity.setSize(metadata.getSize());
            latestEntity.setModified(false);
            latestEntity.setRevision(metadata.getRev());

            _directoryDbContext.updateFile(latestEntity);

            logger.debug("Uploaded [" + entity.getFullPath() + "]");

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

    private boolean isInUse(DirectoryEntity entity){
        return entity.isLocked() || entity.getInUseCount() > 0;
    }
}
