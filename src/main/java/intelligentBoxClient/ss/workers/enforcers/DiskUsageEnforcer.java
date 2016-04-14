package intelligentBoxClient.ss.workers.enforcers;

import intelligentBoxClient.ss.bootstrapper.IConfiguration;
import intelligentBoxClient.ss.dao.IDirectoryDbContext;
import intelligentBoxClient.ss.dao.pojo.DirectoryEntity;
import intelligentBoxClient.ss.dao.pojo.LruEntity;
import intelligentBoxClient.ss.utils.Consts;
import intelligentBoxClient.ss.utils.IUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

/**
 * Created by Leo on 4/9/16.
 */
@Service
public class DiskUsageEnforcer implements IDiskUsageEnforcer{


    private final static int SUCCESS = 0;
    private final static int FAIL = -1;
    private final static int SKIP = 1;

    private IDirectoryDbContext _directoryDbContext;
    private IConfiguration _configuration;
    private Log logger;
    private IUtils _utils;

    @Autowired
    public DiskUsageEnforcer(IDirectoryDbContext directoryDbContext,
                             IConfiguration configuration,
                             IUtils utils){
        _utils = utils;
        logger = LogFactory.getLog(this.getClass());
        _directoryDbContext = directoryDbContext;
        _configuration = configuration;
    }

    public void enforce() {
        long maxDiskUsage = _configuration.getMaxLocalSize();
        long diskUsage;

        boolean isError = true;
        boolean inTransaction = false;
        boolean needClean = true;

        int maxRetryTransactionTimes = 100;
        int triedTransactionTimes = 0;
        while (needClean) {
            try {
                inTransaction = _directoryDbContext.beginTransaction();

                if (!inTransaction){
                    logger.warn("Transaction is failed to begin. Attempted times [" + triedTransactionTimes + "]");
                    if (triedTransactionTimes >= maxRetryTransactionTimes){
                        needClean = false;
                        logger.warn("Too many attempts to begin a transaction. Skip this round");
                    }
                } else {
                    diskUsage = getDiskUsage();
                    if (diskUsage <= maxDiskUsage) {
                        needClean = false;
                        isError = false;
                        continue;
                    }

                    LruEntity toClean = _directoryDbContext.peekLru(false);
                    while (toClean != null) {
                        if (toClean.getCurr().equals(Consts.TAIL)) {
                            logger.warn("No more files can be cleaned in LRU queue. Perhaps all local files are in use or are modified.");
                            isError = false;
                            needClean = false;
                            break;
                        }
                        int retClean = cleanLocalFile(toClean.getCurr());
                        if (retClean == FAIL) {
                            logger.error("Failed to delete [" + toClean.getCurr() + "]");
                        } else if (retClean == SUCCESS) {
                            _directoryDbContext.removeLru(toClean.getCurr(), false);
                            isError = false;
                            break;
                        }

                        toClean = _directoryDbContext.findLru(toClean.getNext(), false);
                    }
                }
            } finally {
                if (inTransaction) {
                    if (isError) {
                        _directoryDbContext.rollbackTransaction();
                    } else {
                        _directoryDbContext.commitTransaction();
                    }
                }
            }

            if (needClean) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    logger.warn("Failed to sleep");
                }
            }
        }

    }


    private long getDiskUsage(){
        try {
            return _directoryDbContext.queryDiskUsage();
        } catch (SQLException e) {
            logger.error("Failed to get disk usage.",e);
            return 0;
        }
    }

    private int cleanLocalFile(String path){
        try {
            logger.debug("Cleaning file [" + path + "]");
            DirectoryEntity directoryEntity = _directoryDbContext.querySingleEntry(path);
            if (directoryEntity == null || !directoryEntity.isLocal()){
                return SUCCESS; //Not local
            }

            if (isInUse(directoryEntity)){
                logger.debug("Skipped cleaning file [" + path + "] because it is in use.");
                return SKIP;
            }

            if (directoryEntity.isModified()){
                logger.debug("Skipped cleaning file [" + path + "] because it is modified.");
                return SKIP;
            }

            String filePath = _configuration.getDataFolderPath() + path;
            Path filePathOutput = Paths.get(filePath);

            Files.deleteIfExists(filePathOutput);
            directoryEntity.setLocal(false);
            _directoryDbContext.updateEntry(directoryEntity);
            logger.debug("Cleaned file [" + path + "]");
            return SUCCESS;
        } catch (SQLException e) {
            logger.error("Failed to clean [" + path + "].", e);
            return FAIL;
        } catch (IOException e) {
            logger.error("Failed to clean [" + path + "].", e);
            return FAIL;
        }
    }

    private boolean isInUse(DirectoryEntity entity){
        return entity.isLocked() || entity.getInUseCount() > 0;
    }

}
