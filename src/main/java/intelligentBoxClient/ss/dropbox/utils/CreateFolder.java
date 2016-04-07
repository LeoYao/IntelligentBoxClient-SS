package intelligentBoxClient.ss.dropbox.utils;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.CreateFolderErrorException;
import com.dropbox.core.v2.files.FolderMetadata;
import intelligentBoxClient.ss.bootstrapper.IConfiguration;

/**
 * Created by yaohx on 4/7/2016.
 */
public class CreateFolder extends RetryClientOperation<Boolean> {

    private String _remotePath;

    public CreateFolder(IConfiguration configuration, DbxClientV2 client, String remotePath) {
        super(configuration, client);
        _remotePath= remotePath;
    }

    @Override
    protected Boolean run() throws DbxException {
        try {
            _client.files().createFolder(_remotePath);
            return true;
        }
        catch (CreateFolderErrorException ex){
            if (ex.errorValue.isPath() && ex.errorValue.getPathValue().isConflict()){
                logger.warn("Failed to create folder [" + _remotePath + "]. The folder already exists,", ex);
                return true;
            }else {
                throw ex;
            }
        }
    }
}
