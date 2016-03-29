package intelligentBoxClient.ss.dao;

import intelligentBoxClient.ss.dao.pojo.DirectoryEntity;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by Leo on 3/27/16.
 */
public interface ISqliteContext {
    boolean open(String dbFile);
    boolean close();
    void beginTransaction(int maxRetryTimes, int retryInterval) throws SQLException, InterruptedException;
    void commitTransaction() throws SQLException;
    void rollbackTransaction() throws SQLException;
}
