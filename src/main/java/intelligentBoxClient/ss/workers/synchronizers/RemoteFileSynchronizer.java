package intelligentBoxClient.ss.workers.synchronizers;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.DeletedMetadata;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.Metadata;
import intelligentBoxClient.ss.bootstrapper.IConfiguration;
import intelligentBoxClient.ss.dao.IDirectoryDbContext;
import intelligentBoxClient.ss.dao.INotificationDbContext;
import intelligentBoxClient.ss.dao.pojo.DirectoryEntity;
import intelligentBoxClient.ss.dao.pojo.RemoteChangeEntity;
import intelligentBoxClient.ss.dropbox.IDropboxClient;
import intelligentBoxClient.ss.persistence.IDirectoryDbSaver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Created by Leo on 4/3/16.
 */
@Service(value="RemoteFileSynchronizer")
public class RemoteFileSynchronizer implements IFileSynchronizer {

    private IDirectoryDbContext _directoryDbContext;
    private INotificationDbContext _notificationDbContext;
    private IConfiguration _configuration;
    private IDirectoryDbSaver _directoryDbSaver;
    private IDropboxClient _dropboxClient;
    private Log logger;

    @Autowired
    public RemoteFileSynchronizer(IDirectoryDbContext directoryDbContext,
                                  INotificationDbContext notificationDbContext,
                                  IConfiguration configuration,
                                  IDirectoryDbSaver directoryDbSaver,
                                  IDropboxClient dropboxClient){
        logger = LogFactory.getLog(this.getClass());
        _directoryDbContext = directoryDbContext;
        _notificationDbContext = notificationDbContext;
        _configuration = configuration;
        _directoryDbSaver = directoryDbSaver;
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
            deleteFile(remoteChange);
        } else {
            addFile(remoteChange);
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
                String name = metadata.getName().toLowerCase();
                RemoteChangeEntity remoteChangeEntity = null;
                if (metadata instanceof DeletedMetadata) {
                    remoteChangeEntity = new RemoteChangeEntity(fullPath, name, true, "");
                } else if (metadata instanceof FileMetadata){ //ignore folders
                    FileMetadata fileMetadata = (FileMetadata)metadata;
                    remoteChangeEntity = new RemoteChangeEntity(fullPath, name, false, fileMetadata.getRev());
                } else {
                    continue;//ignore folders
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
        return existingRemoteChanges;
    }


    private void addFile(RemoteChangeEntity remoteChange){

        logger.debug("Adding [" + remoteChange.getFullPath() + "] from dropbox.");

        try {
            DirectoryEntity directoryEntity = _directoryDbContext.querySingleFile(remoteChange.getFullPath());

            if (directoryEntity == null) {
                //No need to download if the file is not local or the local file has been modified
                _notificationDbContext.deletePendingRemoteChanges(remoteChange.getFullPath());
                logger.debug("Skipped synchronizing " + remoteChange.getFullPath() + " from dropbox.");
            } else if (directoryEntity.isModified() || !directoryEntity.isLocal()
                    || directoryEntity.getRevision().equals(remoteChange.getRevision())){

            }


            if (!directoryEntity.isLocked()) {
                if (remoteChange.isDeleted()){
                    deleteFile(remoteChange);
                } else {
                    addFile(remoteChange);
                }
            } else {
                logger.debug("Skipped synchronizing " + remoteChange.getFullPath() + " from dropbox. Another attempt will be made next time.");
            }

        } catch (SQLException e) {
            logger.warn("Failed to synchronize [" + remoteChange.getFullPath() + "] from dropbox", e);
            return;
        }


        String tmpPath = _configuration.getTmpFolderPath() + remoteChange.getEntryName();
        String localPath = _configuration.getDataFolderPath() + remoteChange.getFullPath();
        FileMetadata metadata = null;

        try {
            metadata = _dropboxClient.downloadFile(remoteChange.getFullPath(), tmpPath);
        } catch (DbxException e) {
            logger.warn("Failed to download [" + remoteChange.getFullPath() + "]", e);
            return;
        }

        boolean isError = false;
        boolean inTransaction = false;
        Path tmpInput = Paths.get(tmpPath);

        try {
            inTransaction = _directoryDbContext.beginTransaction();
            DirectoryEntity latestDirectoryEntity = _directoryDbContext.querySingleFile(remoteChange.getFullPath());
            if (!latestDirectoryEntity.isLocked()
                    && !latestDirectoryEntity.isModified()
                    && latestDirectoryEntity.isLocal()) {
                Path localOutput = Paths.get(localPath);
                Files.move(tmpInput, localOutput, REPLACE_EXISTING);
                _directoryDbSaver.save(metadata);
                _notificationDbContext.deletePendingRemoteChanges(remoteChange.getFullPath());
            } else {
                if (latestDirectoryEntity.isModified() || !latestDirectoryEntity.isLocal()) {
                    _notificationDbContext.deletePendingRemoteChanges(remoteChange.getFullPath());
                }

                Files.delete(tmpInput);
            }

            logger.debug("Downloaded " + remoteChange.getFullPath() + " from dropbox.");

        } catch (IOException e) {
            logger.warn("Failed to download [" + remoteChange.getFullPath() + "] from dropbox", e);
            isError = true;
        } catch (SQLException e) {
            logger.warn("Failed to download [" + remoteChange.getFullPath() + "] from dropbox", e);
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
        }
    }

    private void deleteFile(RemoteChangeEntity remoteChange){
        logger.debug("Deleting " + remoteChange.getFullPath() + " due to remote change in dropbox.");

        String localPath = _configuration.getDataFolderPath() + remoteChange.getFullPath();
        boolean isError = false;
        boolean inTransaction = false;

        try {
            inTransaction = _directoryDbContext.beginTransaction();
            DirectoryEntity directoryEntity = _directoryDbContext.querySingleFile(remoteChange.getFullPath());
            if (!directoryEntity.isLocked()
                    && !directoryEntity.isModified()
                    && directoryEntity.isLocal()) {

                Path localOutput = Paths.get(localPath);
                Files.delete(localOutput);
                _notificationDbContext.deletePendingRemoteChanges(remoteChange.getFullPath());
                _directoryDbContext.deleteFile(remoteChange.getFullPath());
            } else {
                if (directoryEntity.isModified() || !directoryEntity.isLocal()) {
                    _notificationDbContext.deletePendingRemoteChanges(remoteChange.getFullPath());
                }
            }

            logger.debug("Deleted " + remoteChange.getFullPath() + " due to remote change in dropbox.");

        } catch (IOException e) {
            logger.warn("Failed to delete [" + remoteChange.getFullPath() + "] from dropbox", e);
            isError = true;
        } catch (SQLException e) {
            logger.warn("Failed to delete [" + remoteChange.getFullPath() + "] from dropbox", e);
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
        }
    }

}
