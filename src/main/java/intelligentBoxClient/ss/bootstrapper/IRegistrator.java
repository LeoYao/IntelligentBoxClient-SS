package intelligentBoxClient.ss.bootstrapper;

import com.dropbox.core.DbxException;

import java.io.IOException;

/**
 * Created by yaohx on 3/24/2016.
 */
public interface IRegistrator {

    void register() throws IOException, DbxException;
}
