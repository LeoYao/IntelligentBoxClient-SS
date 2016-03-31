package intelligentBoxClient.ss.dropbox.utils;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import intelligentBoxClient.ss.bootstrapper.IConfiguration;

/**
 * Created by yaohx on 3/31/2016.
 */
public class ListFolder extends RetryClientOperation<ListFolderResult> {

    private String _cursor;
    private String _path;

    public ListFolder(IConfiguration configuration, DbxClientV2 client, String path){
        super(configuration, client);
        _path = path;
    }

    public ListFolder(IConfiguration configuration, DbxClientV2 client, String path, String cursor){
        this(configuration, client, path);
        _cursor = cursor;
    }

    @Override
    protected ListFolderResult run() throws DbxException {
        ListFolderResult result = null;
        if (_cursor == null || _cursor.trim().equals("")){
            result = _client.files().listFolder(_path);
        }
        else {
            result = _client.files().listFolderContinue(_cursor);
        }
        _cursor = result.getCursor();
        return result;
    }
}
