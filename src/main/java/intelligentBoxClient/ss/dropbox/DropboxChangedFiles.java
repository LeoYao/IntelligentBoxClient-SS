package intelligentBoxClient.ss.dropbox;

import com.dropbox.core.v2.files.DeletedMetadata;
import com.dropbox.core.v2.files.Metadata;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by yaohx on 3/29/2016.
 */
public class DropboxChangedFiles {
    private List<Metadata> _changedMetadatas = new LinkedList<>();
    private List<DeletedMetadata> _deletedMetadatas = new LinkedList<>();

    public void addChangedMetadata(Metadata metadata) {
        _changedMetadatas.add(metadata);
    }

    public List<Metadata> getChangedMetadatas() {
        return new ArrayList<>(_changedMetadatas);
    }

    public void addDeletedMetadata(DeletedMetadata metadata) {
        _deletedMetadatas.add(metadata);
    }

    public List<DeletedMetadata> getDeletedMetadatas() {
        return new ArrayList<>(_deletedMetadatas);
    }
}
