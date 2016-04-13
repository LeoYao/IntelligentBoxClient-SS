package intelligentBoxClient.ss.bootstrapper;

import intelligentBoxClient.ss.dao.IDirectoryDbContext;
import intelligentBoxClient.ss.dao.INotificationDbContext;
import intelligentBoxClient.ss.dao.pojo.DirectoryEntity;
import intelligentBoxClient.ss.dropbox.IDropboxClient;
import intelligentBoxClient.ss.utils.Consts;
import intelligentBoxClient.ss.workers.ISynchronizationWorker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Created by yaohx on 3/24/2016.
 */
@Service
public class Bootstrapper {

    private IRegistrator _registrator;
    private ISynchronizationWorker _synchronizationWorker;
    private IDirectoryDbContext _directoryDbCtx;
    private INotificationDbContext _notificationDbCtx;
    private IConfiguration _config;
    private IDropboxClient _client;

    @Autowired
    private ApplicationContext appCtx;

    private Log logger;
    @Autowired
    public Bootstrapper(IRegistrator registrator,
                        ISynchronizationWorker sychronizationWorker,
                        IDirectoryDbContext directoryDbCtx,
                        INotificationDbContext notificationDbCtx,
                        IDropboxClient client,
                        IConfiguration config)
    {
        _registrator = registrator;
        _synchronizationWorker = sychronizationWorker;
        _notificationDbCtx = notificationDbCtx;
        _directoryDbCtx = directoryDbCtx;
        _config = config;
        _client = client;
        logger = LogFactory.getLog(this.getClass());
    }

    public boolean startup() {

        boolean result = _client.open()
                && _directoryDbCtx.open(_config.getDirDbFilePath())
                && _notificationDbCtx.open(_config.getNotifDbFilePath())
                && _registrator.register();


        if (result) {
            genData(appCtx);
            genLru(appCtx);

            result &= _synchronizationWorker.start();
        }
        return result;
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


    public void genLru(ApplicationContext appCtx){
        IDirectoryDbContext ctx = appCtx.getBean(IDirectoryDbContext.class);

        while(ctx.popLru(true) != null);
/*
        ctx.pushLru("a", true);
        ctx.pushLru("b", true);
        ctx.pushLru("c", true);

        ctx.removeLru("b", true);
*/

        ctx.pushLru("/test1.txt", false);
        ctx.pushLru("/test0.txt", false);
        ctx.pushLru("/test2.txt", false);
    }


    public void genData(ApplicationContext appCtx){
        IDropboxClient dbxClient = appCtx.getBean(IDropboxClient.class);

        IDirectoryDbContext dirDbCtx = appCtx.getBean(IDirectoryDbContext.class);

        IConfiguration configuration = appCtx.getBean(IConfiguration.class);

        try {
            DirectoryEntity folderEntity = dirDbCtx.querySingleEntry("");
            if (folderEntity == null) {
                folderEntity = new DirectoryEntity();
                folderEntity.setFullPath("");
                folderEntity.setParentFolderFullPath(".");
                folderEntity.setEntryName("");
                folderEntity.setType(Consts.FOLDER);
                folderEntity.setMtime(new Timestamp(new Date().getTime()));
                folderEntity.setAtime(new Timestamp(new Date().getTime()));
                folderEntity.setLocal(true);
                dirDbCtx.insertEntry(folderEntity);
            }
/*
            for (int i = 0; i < 3; ++i) {
                String remotePath = "/testdata/test" + i + ".txt";
                FileMetadata metadata = dbxClient.uploadFile(remotePath, configuration.getDataFolderPath() + "testdata/test" + i + ".txt");
                DirectoryEntity directoryEntity = dirDbCtx.querySingleEntry(remotePath);
                if (directoryEntity == null) {
                    directoryEntity = new DirectoryEntity(metadata);
                    dirDbCtx.insertEntry(directoryEntity);
                } else {
                    directoryEntity = new DirectoryEntity(metadata, directoryEntity);
                    dirDbCtx.updateEntry(directoryEntity);
                }
            }*/
            //} catch (DbxException e) {
            //    logger.error(e);
        } catch (SQLException e) {
            logger.error(e);
        }
    }
}
