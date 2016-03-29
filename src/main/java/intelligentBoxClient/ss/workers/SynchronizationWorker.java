package intelligentBoxClient.ss.workers;

import intelligentBoxClient.ss.dao.IDirectoryDbContext;
import intelligentBoxClient.ss.dao.INotificationDbContext;
import intelligentBoxClient.ss.dropbox.IDropboxClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    @Autowired
    public SynchronizationWorker(IDirectoryDbContext directoryDbContext,
                                 INotificationDbContext notificationDbContext,
                                 IDropboxClient dropboxClient){
        _directoryDbContext = directoryDbContext;
        _notificationDbContext = notificationDbContext;
        _dropboxClient = dropboxClient;
    }

    @Override
    public void run() {

        try {
            while (true) {
                logger.debug("Running....");
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

    private void downloadRemoteChanges()
    {
        try {
            if (_notificationDbContext.isRemoteChanged()) {

            }
        }
        catch (Exception e)
        {
            logger.warn(e);
        }
    }
}
