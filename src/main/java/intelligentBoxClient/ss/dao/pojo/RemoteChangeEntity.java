package intelligentBoxClient.ss.dao.pojo;

/**
 * Created by yaohx on 3/31/2016.
 */
public class RemoteChangeEntity {
    private String _fullPath;
    private String _entryName;
    private boolean _isDeleted;

    public RemoteChangeEntity(String fullPath, String name, boolean isDeleted){
        _fullPath = fullPath;
        _isDeleted = isDeleted;
        _entryName = name;
    }

    public String getFullPath(){ return _fullPath; }
    public void setFullPath(String fullPath) { _fullPath = fullPath; }

    public String getEntryName(){ return _entryName; }
    public void setEntryName(String entryName) { _entryName = entryName; }

    public boolean isDeleted(){ return _isDeleted;}
    public void setDeleted(boolean isDeleted) { _isDeleted = isDeleted;}

}
