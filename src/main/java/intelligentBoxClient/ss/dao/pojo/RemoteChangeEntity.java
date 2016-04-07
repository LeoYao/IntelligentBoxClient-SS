package intelligentBoxClient.ss.dao.pojo;

import com.dropbox.core.v2.files.DeletedMetadata;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.Metadata;
import intelligentBoxClient.ss.utils.Consts;

import java.sql.Timestamp;

/**
 * Created by yaohx on 3/31/2016.
 */
public class RemoteChangeEntity {

    private String _fullPath;
    private String _entryName;
    private boolean _isDeleted;
    private String _revision;
    private long _size;
    private Timestamp _mtime;
    private Timestamp _atime;
    private int _type;

    public RemoteChangeEntity(){
    }

    public RemoteChangeEntity(Metadata metadata){
        initCommonVars(metadata);
    }

    public RemoteChangeEntity(DeletedMetadata metadata){
        initDeletion(metadata);
    }
    public RemoteChangeEntity(FileMetadata metadata){
        initFile(metadata);
    }

    public String getFullPath(){ return _fullPath; }
    public void setFullPath(String fullPath) { _fullPath = fullPath; }

    public String getEntryName(){ return _entryName; }
    public void setEntryName(String entryName) { _entryName = entryName; }

    public boolean isDeleted(){ return _isDeleted;}
    public void setDeleted(boolean isDeleted) { _isDeleted = isDeleted;}

    public String getRevision(){ return _revision == null ? "" : _revision; }
    public void setRevision(String revision) { _revision = revision; }

    public long getSize() {
        return _size;
    }
    public void setSize(long size) {
        this._size = size;
    }

    public Timestamp getMtime() {
        return _mtime == null ? new Timestamp(0) : _mtime ;
    }
    public void setMtime(Timestamp mtime) {
        this._mtime = mtime;
    }
    public void setMtime(long mtime) {
        this._mtime = new Timestamp(mtime);
    }

    public Timestamp getAtime() {
        return _atime == null ? new Timestamp(0) : _atime ;
    }
    public void setAtime(Timestamp atime) {
        this._atime = atime;
    }
    public void setAtime(long atime) {
        this._atime = new Timestamp(atime);
    }

    public int getType() {
        return _type;
    }
    public void setType(int type) {
        this._type = type;
    }

    protected void initCommonVars(Metadata metadata){
        _fullPath = metadata.getPathLower();
        _entryName = metadata.getName().toLowerCase();
    }

    protected void initFile(FileMetadata metadata){
        initCommonVars(metadata);
        _type = Consts.FILE;
        _isDeleted = false;
        _revision = metadata.getRev();
        _size = metadata.getSize();
        _mtime = new Timestamp(metadata.getServerModified().getTime());
        _atime = new Timestamp(metadata.getServerModified().getTime());
    }

    protected void initDeletion(DeletedMetadata metadata){
        initCommonVars(metadata);
        _isDeleted = true;
    }
}
