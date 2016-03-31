package intelligentBoxClient.ss.persistence;

import com.dropbox.core.v2.files.FileMetadata;

/**
 * Created by yaohx on 3/31/2016.
 */
public interface IDirectoryDbSaver {
    boolean save(FileMetadata metadata);
}
