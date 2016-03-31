package intelligentBoxClient.ss.bootstrapper;

/**
 * Created by yaohx on 3/31/2016.
 */
public interface IConfiguration {
    String getCnsUrl();

    int getServerPort();

    boolean getDevFlag();

    String getRootPath();

    String getDirDbFileName();

    int getDbMaxRetryTimes();

    int getDropboxCallMaxRetryTimes();

    int getDbRetryInterval();

    String getDirDbFilePath();

    String getNotifDbFilePath();

    String getDbxTokenFilePath();

    String getDbxCursorFilePath();

    String getDataFolderPath();

    String getMetadataFolderPath();

    String getTmpFolderPath();
}
