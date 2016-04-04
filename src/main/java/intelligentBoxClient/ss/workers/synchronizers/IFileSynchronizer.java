package intelligentBoxClient.ss.workers.synchronizers;

import intelligentBoxClient.ss.dao.pojo.RemoteChangeEntity;

/**
 * Created by Leo on 4/3/16.
 */
public interface IFileSynchronizer {
    /*
     *  Download remote changed files when
     *  (1) the file has existed in local storage
     *  (2) the local file is not modified
     *
     *  If the local file is locked but not yet modified,
     *  skip the file for this time and retry it next time.
     */
    void synchronize();
}
