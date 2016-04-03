package intelligentBoxClient.ss.workers;

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
import intelligentBoxClient.ss.workers.synchronizers.IRemoteFileSynchronizer;
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

import static java.nio.file.StandardCopyOption.*;

/**
 * Created by Leo on 3/27/16.
 */
@Service
public class SynchronizationWorker implements Runnable, ISynchronizationWorker {

    private Thread thread;
    private Log logger = LogFactory.getLog(this.getClass());
    private IDirectoryDbContext _directoryDbContext;
    private INotificationDbContext _notificationDbContext;
    private IDropboxClient _dropboxClient;
    private IConfiguration _configuration;
    private IDirectoryDbSaver _directoryDbSaver;
    private IRemoteFileSynchronizer _remoteFileSynchronizer;

    @Autowired
    public SynchronizationWorker(IDirectoryDbContext directoryDbContext,
                                 INotificationDbContext notificationDbContext,
                                 IDropboxClient dropboxClient,
                                 IDirectoryDbSaver directoryDbSaver,
                                 IConfiguration configuration,
                                 IRemoteFileSynchronizer remoteFileSynchronizer){
        _directoryDbContext = directoryDbContext;
        _notificationDbContext = notificationDbContext;
        _dropboxClient = dropboxClient;
        _configuration = configuration;
        _directoryDbSaver = directoryDbSaver;
        _remoteFileSynchronizer = remoteFileSynchronizer;
    }

    @Override
    public void run() {

        try {
            while (true) {
                synchronizeRemoteChanges();

                _dropboxClient.save();
                Thread.sleep(2000);
            }
        } catch (InterruptedException e) {
            logger.debug(e);
        }
    }

    public boolean start()
    {
        thread = new Thread(this);
        thread.start();

        return true;
    }

    public boolean stop() {
        if (thread != null && thread.isAlive())
        {
            thread.interrupt();

            try {
                thread.join();
            } catch (InterruptedException e) {
                logger.error(e);
            }
        }

        return true;
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
                    remoteChangeEntity = new RemoteChangeEntity(fullPath, name, true);
                } else if (metadata instanceof FileMetadata){ //ignore folders
                    remoteChangeEntity = new RemoteChangeEntity(fullPath, name, false);
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

    private void synchronizeRemoteChanges() {
        try {
            List<RemoteChangeEntity> remoteChanges = getRemoteChanges();

            for (RemoteChangeEntity remoteChange : remoteChanges) {
                _remoteFileSynchronizer.synchronize(remoteChange);
            }
        } catch (SQLException ex) {
            logger.warn(ex);
        }
    }

}
