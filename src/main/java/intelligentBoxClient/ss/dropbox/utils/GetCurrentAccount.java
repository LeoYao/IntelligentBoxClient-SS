package intelligentBoxClient.ss.dropbox.utils;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.users.FullAccount;
import intelligentBoxClient.ss.bootstrapper.IConfiguration;

/**
 * Created by yaohx on 3/31/2016.
 */
public class GetCurrentAccount extends RetryClientOperation<FullAccount> {

    public GetCurrentAccount(IConfiguration configuration, DbxClientV2 client) {
        super(configuration, client);
    }

    @Override
    protected FullAccount run() throws DbxException {
        return _client.users().getCurrentAccount();
    }
}
