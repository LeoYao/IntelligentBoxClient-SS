package intelligentBoxClient.ss.dao;

import intelligentBoxClient.ss.dao.pojo.RemoteChangeEntity;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by yaohx on 3/29/2016.
 */
public interface INotificationDbContext extends ISqliteContext{
    /*
     * Check if there is remote changing. Calling the method will reset the flag in the same atomic operation.
     */
    boolean isRemoteChanged() throws SQLException;
    void setRemoteChanged() throws SQLException;
    List<RemoteChangeEntity> queryAllPendingRemoteChanges() throws SQLException;
    RemoteChangeEntity queryPendingRemoteChanges(String fullPath) throws SQLException;
    int insertPendingRemoteChanges(RemoteChangeEntity remoteChangeEntity) throws SQLException;
    int deletePendingRemoteChanges(String fullPath) throws SQLException;
    int updatePendingRemoteChanges(RemoteChangeEntity entity) throws SQLException;
}
