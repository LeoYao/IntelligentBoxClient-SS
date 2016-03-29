package intelligentBoxClient.ss.bootstrapper;

import intelligentBoxClient.ss.dao.IDirectoryDbContext;
import intelligentBoxClient.ss.dao.INotificationDbContext;
import intelligentBoxClient.ss.dropbox.DropboxClient;
import intelligentBoxClient.ss.dropbox.IDropboxClient;
import intelligentBoxClient.ss.workers.ISynchronizationWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by yaohx on 3/24/2016.
 */
@Service
public class Bootstrapper {

    private IRegistrator _registrator;
    private ISynchronizationWorker _synchronizationWorker;
    private IDirectoryDbContext _directoryDbCtx;
    private INotificationDbContext _notificationDbCtx;
    private Configuration _config;
    private IDropboxClient _client;

    @Autowired
    public Bootstrapper(IRegistrator registrator,
                        ISynchronizationWorker sychronizationWorker,
                        IDirectoryDbContext directoryDbCtx,
                        INotificationDbContext notificationDbCtx,
                        IDropboxClient client,
                        Configuration config)
    {
        _registrator = registrator;
        _synchronizationWorker = sychronizationWorker;
        _notificationDbCtx = notificationDbCtx;
        _directoryDbCtx = directoryDbCtx;
        _config = config;
        _client = client;
    }

    public boolean startup() {

        return _client.open()
                && _directoryDbCtx.open(_config.getDirDbFilePath())
                && _notificationDbCtx.open(_config.getNotifDbFilePath())
                && _registrator.register()
                && _synchronizationWorker.start()
                ;
    }

    public boolean shutdown()
    {
        boolean result = true;
        result &= _registrator.unregister();
        _synchronizationWorker.stop();
        result &= _directoryDbCtx.close();
        result &= _notificationDbCtx.close();
        result &= _client.close();
        return result;
    }
}
