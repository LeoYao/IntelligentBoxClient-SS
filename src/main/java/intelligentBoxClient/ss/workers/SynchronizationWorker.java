package intelligentBoxClient.ss.workers;

import intelligentBoxClient.ss.dropbox.IDropboxClient;
import intelligentBoxClient.ss.workers.enforcers.IDiskUsageEnforcer;
import intelligentBoxClient.ss.workers.synchronizers.IFileSynchronizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * Created by Leo on 3/27/16.
 */
@Service
public class SynchronizationWorker implements Runnable, ISynchronizationWorker {

    private Thread thread;
    private Log logger = LogFactory.getLog(this.getClass());
    private IDropboxClient _dropboxClient;
    private IFileSynchronizer _remoteFileSynchronizer;
    private IFileSynchronizer _localFileSynchronizer;
    private IDiskUsageEnforcer _diskUsageEnforcer;


    @Autowired
    public SynchronizationWorker(IDropboxClient dropboxClient,
                                 @Qualifier("RemoteFileSynchronizer") IFileSynchronizer remoteFileSynchronizer,
                                 @Qualifier("LocalFileSynchronizer") IFileSynchronizer localFileSynchronizer,
                                 IDiskUsageEnforcer diskUsageEnforcer){
        _dropboxClient = dropboxClient;

        _remoteFileSynchronizer = remoteFileSynchronizer;
        _localFileSynchronizer = localFileSynchronizer;
        _diskUsageEnforcer = diskUsageEnforcer;
    }

    @Override
    public void run() {

        try {
            while (true) {
                _remoteFileSynchronizer.synchronize();
                _localFileSynchronizer.synchronize();
                _diskUsageEnforcer.enforce();
                _dropboxClient.save();
                Thread.sleep(5000);
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

}
