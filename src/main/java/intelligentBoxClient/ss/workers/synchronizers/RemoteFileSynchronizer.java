package intelligentBoxClient.ss.workers.synchronizers;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.DeletedMetadata;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.Metadata;
import intelligentBoxClient.ss.bootstrapper.IConfiguration;
import intelligentBoxClient.ss.dao.IDirectoryDbContext;
import intelligentBoxClient.ss.dao.INotificationDbContext;
import intelligentBoxClient.ss.dao.pojo.DirectoryEntity;
import intelligentBoxClient.ss.dao.pojo.RemoteChangeEntity;
import intelligentBoxClient.ss.dropbox.IDropboxClient;
import intelligentBoxClient.ss.utils.Consts;
import intelligentBoxClient.ss.utils.IUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Created by Leo on 4/3/16.
 */
@Service(value="RemoteFileSynchronizer")
public class RemoteFileSynchronizer extends FileSynchronizer {

    private IDirectoryDbContext _directoryDbContext;
    private INotificationDbContext _notificationDbContext;
    private IConfiguration _configuration;
    private IDropboxClient _dropboxClient;
    private Log logger;
    @Autowired
    public RemoteFileSynchronizer(IDirectoryDbContext directoryDbContext,
                                  INotificationDbContext notificationDbContext,
                                  IConfiguration configuration,
                                  IDropboxClient dropboxClient,
                                  IUtils utils){
        super(utils);
        logger = LogFactory.getLog(this.getClass());
        _directoryDbContext = directoryDbContext;
        _notificationDbContext = notificationDbContext;
        _configuration = configuration;
        _dropboxClient = dropboxClient;
    }

    /*
     *  Download/Deleted according to remote changed files when
     *    (1) the file has existed in local storage
     *    (2) the local file is not modified
     *
     *  If the local file is locked but not yet modified,
     *  skip the file for this time and retry it next time.
     */
    @Override
    public void synchronize(){

        try {
            List<RemoteChangeEntity> remoteChanges = getRemoteChanges();

            for (RemoteChangeEntity remoteChange : remoteChanges) {
                synchronizeChange(remoteChange);
            }
        } catch (SQLException ex) {
            logger.warn(ex);
        }
    }

    private void synchronizeChange(RemoteChangeEntity remoteChange){

        if (remoteChange.isDeleted()){
            deleteEntry(remoteChange);
        } else {
            addEntry(remoteChange);
        }
    }

    private List<RemoteChangeEntity> getRemoteChanges() throws SQLException {
        List<RemoteChangeEntity> existingRemoteChanges = _notificationDbContext.queryAllPendingRemoteChanges();
        Map<String, RemoteChangeEntity> existingRemoteChangeMap = new HashMap<>();
        for (RemoteChangeEntity entity : existingRemoteChanges) {
            existingRemoteChangeMap.put(entity.getFullPath(), entity);
        }

        if (_notificationDbContext.isRemoteChanged()) {
            List<Metadata> changedMetadata = null;
            try {
                changedMetadata = _dropboxClient.getChanges();
            } catch (DbxException e) {
                logger.error(e);
                return existingRemoteChanges;
            }

            for (Metadata metadata : changedMetadata) {
                String fullPath = metadata.getPathLower();
                RemoteChangeEntity remoteChangeEntity = null;
                if (metadata instanceof DeletedMetadata) {
                    DeletedMetadata deletedMetadata = (DeletedMetadata)metadata;
                    remoteChangeEntity = new RemoteChangeEntity(deletedMetadata);
                } else if (metadata instanceof FileMetadata){
                    FileMetadata fileMetadata = (FileMetadata)metadata;
                    remoteChangeEntity = new RemoteChangeEntity(fileMetadata);
                } else if (metadata instanceof FolderMetadata) {
                    FolderMetadata folderMetadata = (FolderMetadata)metadata;
                    remoteChangeEntity = new RemoteChangeEntity(folderMetadata);
                }
                else {
                    continue;//ignore exceptions
                }
                if (existingRemoteChangeMap.containsKey(fullPath)) {
                    if (existingRemoteChangeMap.get(fullPath).isDeleted() != remoteChangeEntity.isDeleted()) {
                        _notificationDbContext.updatePendingRemoteChanges(remoteChangeEntity);
                    }
                } else {
                    _notificationDbContext.insertPendingRemoteChanges(remoteChangeEntity);
                }
                existingRemoteChangeMap.put(fullPath, remoteChangeEntity);
            }
        }

        existingRemoteChanges = new ArrayList<>(existingRemoteChangeMap.values());
        Collections.sort(existingRemoteChanges, RemoteChangeEntity.DEFAULT_COMPARATOR);
        return existingRemoteChanges;
    }

    private void addEntry(RemoteChangeEntity remoteChange){
        logger.debug("Adding [" + remoteChange.getFullPath() + "].");

        boolean isError = false;
        boolean inTransaction = false;
        boolean isNotifInTransaction = false;

        try {
            String parentPath = _utils.extractParentFolderPath(remoteChange.getFullPath(), remoteChange.getEntryName());
            inTransaction = _directoryDbContext.beginTransaction();
            isNotifInTransaction = _notificationDbContext.beginTransaction();
            DirectoryEntity parentDirectoryEntity = _directoryDbContext.querySingleEntry(parentPath);

            if (parentDirectoryEntity == null || !parentDirectoryEntity.isLocal()){
                //No need to synchronize file if its parent folder is not local
                _notificationDbContext.deletePendingRemoteChanges(remoteChange.getFullPath());
                logger.debug("Skipped adding [" + remoteChange.getFullPath() + "], because parent folder is not local.");
                return;
            }

            DirectoryEntity directoryEntity = _directoryDbContext.querySingleEntry(remoteChange.getFullPath());
            if (directoryEntity == null) {
                directoryEntity = getDirectoryEntity(remoteChange);
                _directoryDbContext.insertEntry(directoryEntity);
                _notificationDbContext.deletePendingRemoteChanges(remoteChange.getFullPath());
                logger.debug("Added metadata of [" + remoteChange.getFullPath() + "] .");

            } else if (directoryEntity.getRevision().equals(remoteChange.getRevision())){
                //No need to download if the revision is not changed
                _notificationDbContext.deletePendingRemoteChanges(remoteChange.getFullPath());
                logger.debug("Skipped adding [" + remoteChange.getFullPath() + "], because the revision is same.");

            } else if (!directoryEntity.isLocal()){
                directoryEntity = getDirectoryEntity(remoteChange, directoryEntity);
                _directoryDbContext.updateEntry(directoryEntity);
                _notificationDbContext.deletePendingRemoteChanges(remoteChange.getFullPath());
                logger.debug("Synchronized metadata of [" + remoteChange.getFullPath() + "].");
            } else {
                if (isInUse(directoryEntity)) {
                    if (directoryEntity.isModified()) {
                        //No need to download if the local file has been modified
                        _notificationDbContext.deletePendingRemoteChanges(remoteChange.getFullPath());
                        logger.debug("Skipped downloading [" + remoteChange.getFullPath() + "], because the local file has been modified.");
                    } else {
                        logger.debug("Skipped downloading [" + remoteChange.getFullPath() + "], because the local file is in use. Another attempt will be made next time.");
                    }

                } else if (remoteChange.getType() != Consts.FILE) {
                    _notificationDbContext.deletePendingRemoteChanges(remoteChange.getFullPath());
                    logger.debug("Skipped downloading [" + remoteChange.getFullPath() + "], because it is a folder.");
                } else {
                    //Do not block other transaction while downloading files
                    _directoryDbContext.rollbackTransaction();
                    inTransaction = false;

                    String tmpPath = _configuration.getTmpFolderPath() + remoteChange.getEntryName();
                    FileMetadata metadata = downloadFile(remoteChange.getFullPath(), tmpPath);

                    Path tmpInput = Paths.get(tmpPath);

                    if (metadata != null) {
                        String localPath = _configuration.getDataFolderPath() + remoteChange.getFullPath();

                        inTransaction = _directoryDbContext.beginTransaction();
                        directoryEntity = _directoryDbContext.querySingleEntry(remoteChange.getFullPath());

                        if (directoryEntity != null && directoryEntity.isLocal() && !directoryEntity.isLocked()) {
                            Path localOutput = Paths.get(localPath);
                            Files.move(tmpInput, localOutput, REPLACE_EXISTING);
                            directoryEntity = getDirectoryEntity(metadata, directoryEntity);
                            _directoryDbContext.updateEntry(directoryEntity);
                            _notificationDbContext.deletePendingRemoteChanges(remoteChange.getFullPath());
                            logger.debug("Downloaded [" + remoteChange.getFullPath() + "].");
                        } else {
                            Files.deleteIfExists(tmpInput);
                            logger.debug("Skipped synchronizing [" + remoteChange.getFullPath() + "], because something changed during downloading. Another attempt will be made next time.");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.warn("Failed to add [" + remoteChange.getFullPath() + "].", e);
            isError = true;
            return;
        } catch (IOException e) {
            logger.warn("Failed to add [" + remoteChange.getFullPath() + "].", e);
            isError = true;
        } finally {
            if (inTransaction) {
                if (isError){
                    _directoryDbContext.rollbackTransaction();
                } else {
                    _directoryDbContext.commitTransaction();
                }
            }

            if (isNotifInTransaction){
                if (isError){
                    _notificationDbContext.rollbackTransaction();
                } else {
                    _notificationDbContext.commitTransaction();
                }
            }
        }
    }

    private void deleteEntry(RemoteChangeEntity remoteChange){
        logger.debug("Deleting [" + remoteChange.getFullPath() + "].");

        String localPath = _configuration.getDataFolderPath() + remoteChange.getFullPath();
        boolean isError = false;
        boolean inTransaction = false;
        boolean isNotifInTransaction = false;

        try {
            isNotifInTransaction = _notificationDbContext.beginTransaction();
            inTransaction = _directoryDbContext.beginTransaction();
            DirectoryEntity directoryEntity = _directoryDbContext.querySingleEntry(remoteChange.getFullPath());
            if (directoryEntity == null){
                _notificationDbContext.deletePendingRemoteChanges(remoteChange.getFullPath());
                logger.debug("Skipped deleting [" + remoteChange.getFullPath() + "] because the file is not aware locally.");
            }
            else if (!directoryEntity.isLocal()){
                _notificationDbContext.deletePendingRemoteChanges(remoteChange.getFullPath());
                _directoryDbContext.deleteEntry(remoteChange.getFullPath());
                logger.debug("Deleted metadata of [" + remoteChange.getFullPath() + "].");
            }
            else if (isInUse(directoryEntity)){
                if (directoryEntity.isModified()) {
                    _notificationDbContext.deletePendingRemoteChanges(remoteChange.getFullPath());
                    logger.debug("Skipped deleting [" + remoteChange.getFullPath() + "] because the local file is modified.");
                } else {
                    logger.debug("Skipped deleting [" + remoteChange.getFullPath() + "] because the local file is in use. Another attempt will be made next time.");
                }
            } else { //!directoryEntity.isLocked() && !directoryEntity.isModified() && directoryEntity.isLocal())
                try {
                    Path localOutput = Paths.get(localPath);
                    Files.deleteIfExists(localOutput);
                    _notificationDbContext.deletePendingRemoteChanges(remoteChange.getFullPath());
                    _directoryDbContext.deleteEntry(remoteChange.getFullPath());
                    logger.debug("Deleted [" + remoteChange.getFullPath() + "].");
                } catch (DirectoryNotEmptyException ex){
                    //there must be something just or added or modified in the folder, so need to recreate the folder on dropbox
                    directoryEntity.setModified(true);
                    _directoryDbContext.updateEntry(directoryEntity);
                    _notificationDbContext.deletePendingRemoteChanges(remoteChange.getFullPath());
                    logger.debug("Skipped deleting [" + remoteChange.getFullPath() + "], because there is still some change inside of it.");
                }

            }
        } catch (IOException e) {
            logger.warn("Failed to delete [" + remoteChange.getFullPath() + "].", e);
            isError = true;
        } catch (SQLException e) {
            logger.warn("Failed to delete [" + remoteChange.getFullPath() + "].", e);
            isError = true;
        }
        finally {
            if (inTransaction) {
                if (isError){
                    _directoryDbContext.rollbackTransaction();
                } else {
                    _directoryDbContext.commitTransaction();
                }
            }

            if (isNotifInTransaction){
                if (isError){
                    _notificationDbContext.rollbackTransaction();
                } else {
                    _notificationDbContext.commitTransaction();
                }
            }
        }
    }

    private FileMetadata downloadFile(String remotePath, String localPath){

        FileMetadata metadata = null;

        try {
            metadata = _dropboxClient.downloadFile(remotePath, localPath);
        } catch (DbxException e) {
            logger.warn("Failed to download [" + remotePath + "]", e);
        }

        return metadata;
    }
}
