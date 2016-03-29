package intelligentBoxClient.ss.dropbox;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.Metadata;

/**
 * Created by yaohx on 3/29/2016.
 */
public interface IDropboxClient {
    boolean open();
    boolean close();
    String getAccountId() throws DbxException;
    Metadata getFileMetadata(String path) throws DbxException;
}
