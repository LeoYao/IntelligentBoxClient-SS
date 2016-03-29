package intelligentBoxClient.ss.dao;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by yaohx on 3/29/2016.
 */
public interface INotificationDbContext extends ISqliteContext{
    /*
     * Check if there is remote changing. Calling the method will reset the flag in the same atomic operation.
     */
    boolean isRemoteChanged() throws SQLException, InterruptedException;
    void setRemoteChanged() throws SQLException, InterruptedException;
    List<String> queryAllPendingDownloads() throws SQLException;
    boolean isPendingDownloading(String fullPath) throws SQLException;
    int insertPendingDownload(String fullPath) throws SQLException;
    int deletePendingDownload(String fullPath) throws SQLException;
}
