package intelligentBoxClient.ss.dropbox.utils;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.Metadata;
import intelligentBoxClient.ss.bootstrapper.IConfiguration;

/**
 * Created by yaohx on 3/31/2016.
 */
public class GetMetadata extends RetryClientOperation<Metadata> {

    private String _path;

    public GetMetadata(IConfiguration configuration, DbxClientV2 client, String path) {
        super(configuration, client);
        _path = path;
    }

    @Override
    protected Metadata run() throws DbxException {
        return _client.files().getMetadata(_path);
    }
}
