package intelligentBoxClient.ss.dropbox.utils;

import com.dropbox.core.v2.DbxClientV2;
import intelligentBoxClient.ss.bootstrapper.IConfiguration;

/**
 * Created by yaohx on 3/31/2016.
 */
public abstract class RetryClientOperation<T> extends RetryOperation<T> {

    protected DbxClientV2 _client;

    public RetryClientOperation(IConfiguration configuration, DbxClientV2 client) {
        super(configuration);
        _client = client;
    }
}
