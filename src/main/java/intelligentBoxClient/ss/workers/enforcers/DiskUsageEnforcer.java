package intelligentBoxClient.ss.workers.enforcers;

import intelligentBoxClient.ss.bootstrapper.IConfiguration;
import intelligentBoxClient.ss.dao.IDirectoryDbContext;
import intelligentBoxClient.ss.dao.pojo.DirectoryEntity;
import intelligentBoxClient.ss.dao.pojo.LruEntity;
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

    private long getDiskUsage(){
        try {
            return _directoryDbContext.queryDiskUsage();
        } catch (SQLException e) {
            logger.error("Failed to get disk usage.",e);
            return 0;
        }
    }

    private boolean cleanLocalFile(String path){
        try {
            logger.debug("Cleaning file [" + path + "]");
            DirectoryEntity directoryEntity = _directoryDbContext.querySingleEntry(path);
            if (directoryEntity == null || !directoryEntity.isLocal()){
                return true;
            }
            String filePath = _configuration.getDataFolderPath() + path;
            Path filePathOutput = Paths.get(filePath);

            Files.deleteIfExists(filePathOutput);
            directoryEntity.setLocal(false);
            _directoryDbContext.updateEntry(directoryEntity);
            logger.debug("Cleaned file [" + path + "]");
            return true;
        } catch (SQLException e) {
            logger.error("Failed to clean [" + path + "].", e);
            return false;
        } catch (IOException e) {
            logger.error("Failed to clean [" + path + "].", e);
            return false;
        }
    }

    public void enforce() {
        long maxDiskUsage = _configuration.getMaxLocalSize();
        long diskUsage;

        while (true) {
            boolean isError = true;
            boolean inTransaction = false;
            try {
                inTransaction = _directoryDbContext.beginTransaction();
                diskUsage = getDiskUsage();

                if (diskUsage <= maxDiskUsage) {
                    isError = false;
                    break;
                }

                //Assumption: If one file is in LRU queue, it must not be in use.
                LruEntity toClean = _directoryDbContext.popLru(false);
                if (toClean == null){
                    logger.warn("No file is in LRU queue.");
                    break;
                }

                if (!cleanLocalFile(toClean.getCurr())){
                    logger.error("Failed to delete [" + toClean.getCurr() + "]");
                    break;
                }

                isError = false;
            } finally {
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

}
