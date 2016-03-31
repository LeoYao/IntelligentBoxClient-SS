package intelligentBoxClient.ss.dropbox.utils;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderGetLatestCursorResult;
import intelligentBoxClient.ss.bootstrapper.IConfiguration;

/**
 * Created by yaohx on 3/31/2016.
 */
public class GetLatestCursor extends RetryClientOperation<ListFolderGetLatestCursorResult> {

    public GetLatestCursor(IConfiguration configuration, DbxClientV2 client) {
        super(configuration, client);
    }

    @Override
    protected ListFolderGetLatestCursorResult run() throws DbxException {
        return _client.files().listFolderGetLatestCursorBuilder("").withRecursive(true).withIncludeDeleted(true).start();
    }
}
