package intelligentBoxClient.ss.dropbox.utils;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.Metadata;
import intelligentBoxClient.ss.bootstrapper.IConfiguration;

/**
 * Created by Leo on 4/3/16.
 */

public class Deleter extends RetryClientOperation<Metadata> {

    private String _remotePath;

    public Deleter(IConfiguration configuration, DbxClientV2 client, String remotePath) {
        super(configuration, client);
        _remotePath = remotePath;
    }

    @Override
    protected Metadata run() throws DbxException {
        return _client.files().delete(_remotePath);
    }
}
