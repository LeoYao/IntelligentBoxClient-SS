package intelligentBoxClient.ss.dropbox.utils;

import com.dropbox.core.DbxException;
import com.dropbox.core.RetryException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.Metadata;
import intelligentBoxClient.ss.bootstrapper.IConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.LinkedList;

/**
 * Created by yaohx on 3/31/2016.
 */
public abstract class RetryOperation<T> {

    protected int maxRetryTimes = 0;
    protected Log logger = null;

    public RetryOperation(IConfiguration configuration){
        maxRetryTimes = configuration.getDropboxCallMaxRetryTimes();
        logger = LogFactory.getLog(this.getClass());
    }

    public T execute() throws DbxException {
        for (int i = 0; i <= maxRetryTimes; ++i) {
            try {
                T result = run();
                return result;
            } catch (RetryException ex) {
                if (i >= maxRetryTimes) {
                    logger.error("Reached maximum retry times", ex);
                    return null;
                }
                try{
                    Thread.sleep(ex.getBackoffMillis());
                }
                catch (InterruptedException e){
                    logger.warn("Failed to sleep.", e);
                }

            }
        }
        return null;
    }

    protected abstract T run() throws DbxException;
}
