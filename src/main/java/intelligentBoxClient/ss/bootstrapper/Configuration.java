package intelligentBoxClient.ss.bootstrapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by yaohx on 3/24/2016.
 */
@Component
public class Configuration {

    @Value("${cnsUrl}")
    private String _cnsUrl;

    @Value("${server.port}")
    private int _serverPort;

    @Value("${dev:false}")
    private boolean _dev;

    @Value("${root_path}")
    private String _rootPath;

    @Value("${sqlite_dir_db_file_name}")
    private String _dirDbFilename;

    @Value("${sqlite_notif_db_file_name}")
    private String _notifDbFilename;

    @Value("${dropbox_token_file_name}")
    private String _dbxTokenFileName;

    @Value("${dropbox_cursor_file_name}")
    private String _dbxCursorFileName;

    public String getCnsUrl(){
        return _cnsUrl;
    }

    public int getServerPort(){
        return _serverPort;
    }

    public boolean getDevFlag()
    {
        return _dev;
    }

    public String getRootPath() {
        return _rootPath;
    }

    public String getDirDbFileName()
    {
        return _dirDbFilename;
    }

    public String getDirDbFilePath()
    {
        return _rootPath + "metadata/" + _dirDbFilename;
    }

    public String getNotifDbFilePath()
    {
        return _rootPath + "metadata/" + _notifDbFilename;
    }

    public String getDbxTokenFilePath()
    {
        return _rootPath + "metadata/" + _dbxTokenFileName;
    }

    public String getDbxCursorFilePath()
    {
        return _rootPath + "metadata/" + _dbxCursorFileName;
    }
}
