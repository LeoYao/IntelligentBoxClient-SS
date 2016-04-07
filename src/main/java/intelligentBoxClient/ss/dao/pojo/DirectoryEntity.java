package intelligentBoxClient.ss.dao.pojo;

import java.sql.Timestamp;

/**
 * Created by yaohx on 3/28/2016.
 */
public class DirectoryEntity {

    private String _fullPath;
    private String _parentFolderFullPath;
    private String _entryName;
    private String _oldFullPath;
    private int _type;
    private long _size;
    private Timestamp _mtime;
    private Timestamp _atime;
    private boolean _isLocked;
    private boolean _isModified;
    private boolean _isLocal;
    private boolean _isDeleted;
    private long _inUseCount;
    private String _revision;

    public String getFullPath() {
        return _fullPath;
    }

    public void setFullPath(String fullPath) {
        this._fullPath = fullPath;
    }

    public String getOldFullPath() {
        return _oldFullPath == null ? "" : _oldFullPath;
    }

    public void setOldFullPath(String oldFullPath) {
        this._oldFullPath = oldFullPath;
    }

    public String getParentFolderFullPath() {
        return _parentFolderFullPath;
    }

    public void setParentFolderFullPath(String parentFolderFullPath) {
        this._parentFolderFullPath = parentFolderFullPath;
    }

    public String getEntryName() {
        return _entryName;
    }

    public void setEntryName(String entryName) {
        this._entryName = entryName;
    }

    public int getType() {
        return _type;
    }

    public void setType(int type) {
        this._type = type;
    }

    public long getSize() {
        return _size;
    }

    public void setSize(long size) {
        this._size = size;
    }

    public Timestamp getMtime() {
        return _mtime;
    }

    public void setMtime(Timestamp mtime) {
        this._mtime = mtime;
    }
    public void setMtime(long mtime) {
        this._mtime = new Timestamp(mtime);
    }

    public Timestamp getAtime() {
        return _atime;
    }

    public void setAtime(Timestamp atime) {
        this._atime = atime;
    }

    public void setAtime(long atime) {
        this._atime = new Timestamp(atime);
    }

    public boolean isLocked() {
        return _isLocked;
    }

    public void setLocked(boolean isLocked) {
        this._isLocked = isLocked;
    }

    public boolean isModified() {
        return _isModified;
    }

    public void setModified(boolean isModified) {
        this._isModified = isModified;
    }

    public boolean isLocal() {
        return _isLocal;
    }

    public void setLocal(boolean isLocal) {
        this._isLocal = isLocal;
    }

    public boolean isDeleted() {
        return _isDeleted;
    }

    public void setDeleted(boolean isDeleted) {
        this._isDeleted = isDeleted;
    }

    public long getInUseCount() {
        return _inUseCount;
    }

    public void setInUseCount(long inUseCount) {
        this._inUseCount = inUseCount;
    }

    public String getRevision(){ return _revision == null ? "" : _revision; }
    public void setRevision(String revision) { _revision = revision; }
}
