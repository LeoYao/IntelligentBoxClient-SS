package intelligentBoxClient.ss.dropbox;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.Metadata;

import java.util.List;

/**
 * Created by yaohx on 3/29/2016.
 */
public interface IDropboxClient {
    boolean open();
    boolean close();
    void save();
    String getAccountId() throws DbxException;
    Metadata getFileMetadata(String path) throws DbxException;
    List<Metadata> getFileMetadatas(String path) throws DbxException;
    List<Metadata> getChanges()  throws DbxException;
    FileMetadata downloadFile(String remotePath, String localPath) throws DbxException;
    FileMetadata uploadFile(String remotePath, String localPath) throws DbxException;
    boolean deleteFile(String remotePath) throws DbxException;
    boolean createFolder(String remotePath) throws DbxException;
}
