package intelligentBoxClient.ss.dao.pojo;

import java.sql.Timestamp;

/**
 * Created by yaohx on 3/28/2016.
 */
public class DirectoryEntity {

    public static final int FOLDER = 1;
    public static final int FILE = 2;

    public static final int YES = 1;
    public static final int NO = 0;

    private String _fullPath;
    private String _parentFolderFullPath;
    private String _entryName;
    private int _type;
    private long _size;
    private Timestamp _mtime;
    private Timestamp _atime;
    private int _isLocked;
    private int _isModified;
    private int _isLocal;
    private long _inUseCount;


    public String getFullPath() {
        return _fullPath;
    }

    public void setFullPath(String fullPath) {
        this._fullPath = fullPath;
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

    public Timestamp getAtime() {
        return _atime;
    }

    public void setAtime(Timestamp atime) {
        this._atime = atime;
    }

    public int isLocked() {
        return _isLocked;
    }

    public void setLocked(int isLocked) {
        this._isLocked = isLocked;
    }

    public int isModified() {
        return _isModified;
    }

    public void setModified(int isModified) {
        this._isModified = isModified;
    }

    public int isLocal() {
        return _isLocal;
    }

    public void setLocal(int isLocal) {
        this._isLocal = isLocal;
    }

    public long getInUseCount() {
        return _inUseCount;
    }

    public void setInUseCount(long inUseCount) {
        this._inUseCount = inUseCount;
    }
}
