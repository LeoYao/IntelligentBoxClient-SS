package intelligentBoxClient.ss.bootstrapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by yaohx on 3/24/2016.
 */
@Component
public class Configuration implements IConfiguration {

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

    @Value("${db_max_retry_times}")
    private int _dbMaxRetryTimes;

    @Value("${dropbox_call_retry_times}")
    private int _dropboxCallMaxRetryTimes;

    @Value("${db_retry_interval}")
    private int _dbRetryInterval;

    @Override
    public String getCnsUrl(){
        return _cnsUrl;
    }

    @Override
    public int getServerPort(){
        return _serverPort;
    }

    @Override
    public boolean getDevFlag()
    {
        return _dev;
    }

    @Override
    public String getRootPath() {
        return _rootPath;
    }

    @Override
    public String getDirDbFileName()
    {
        return _dirDbFilename;
    }

    @Override
    public int getDbMaxRetryTimes(){
        return _dbMaxRetryTimes;
    }

    @Override
    public int getDropboxCallMaxRetryTimes(){
        return _dropboxCallMaxRetryTimes;
    }

    @Override
    public int getDbRetryInterval(){
        return _dbRetryInterval;
    }

    @Override
    public String getDirDbFilePath()
    {
        return _rootPath + "metadata/" + _dirDbFilename;
    }

    @Override
    public String getNotifDbFilePath()
    {
        return _rootPath + "metadata/" + _notifDbFilename;
    }

    @Override
    public String getDbxTokenFilePath()
    {
        return _rootPath + "metadata/" + _dbxTokenFileName;
    }

    @Override
    public String getDbxCursorFilePath()
    {
        return _rootPath + "metadata/" + _dbxCursorFileName;
    }

    @Override
    public String getDataFolderPath() {return _rootPath + "data/"; }

    @Override
    public String getMetadataFolderPath() {return _rootPath + "metadata/"; }

    @Override
    public String getTmpFolderPath() { return _rootPath + "tmp/";}
}
