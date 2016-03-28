package intelligentBoxClient.ss.dao;

/**
 * Created by Leo on 3/27/16.
 */
public interface ISqliteContext {
    boolean open(String dbFile);

    boolean close();
}
