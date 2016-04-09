package intelligentBoxClient.ss.workers.enforcers;

import intelligentBoxClient.ss.bootstrapper.IConfiguration;
import intelligentBoxClient.ss.dao.IDirectoryDbContext;
import intelligentBoxClient.ss.utils.IUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public void enforce() {
        long maxDiskUsage = _configuration.getMaxLocalSize();

        boolean toClean = true;

        long diskUsage = Long.MAX_VALUE;

        boolean isError = true;

        while (toClean) {
            boolean inTransaction = false;
            try {
                inTransaction = _directoryDbContext.beginTransaction();
                diskUsage = getDiskUsage();

                if (diskUsage <= maxDiskUsage) {
                    isError = false;
                    break;
                }
            } finally {
                if (inTransaction) {
                    if (isError){

                    }
                }
            }
        }

    }

}
