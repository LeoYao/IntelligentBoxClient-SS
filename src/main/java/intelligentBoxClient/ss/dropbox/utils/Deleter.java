package intelligentBoxClient.ss.dropbox.utils;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DeleteErrorException;
import com.dropbox.core.v2.files.DeletedMetadata;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.Metadata;
import intelligentBoxClient.ss.bootstrapper.IConfiguration;

/**
 * Created by Leo on 4/3/16.
 */

public class Deleter extends RetryClientOperation<Boolean> {

    private String _remotePath;

    public Deleter(IConfiguration configuration, DbxClientV2 client, String remotePath) {
        super(configuration, client);
        _remotePath = remotePath;
    }

    @Override
    protected Boolean run() throws DbxException {
        try {
            _client.files().delete(_remotePath);
            return true;
        }
        catch (DeleteErrorException ex){
            logger.warn("Failed to delete [" +_remotePath +"]. Error: " + ex.errorValue.tag(), ex);
            return true;
        }
    }
}
