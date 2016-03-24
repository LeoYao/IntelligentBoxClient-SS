package intelligentBoxClient.ss.bootstrapper;

import com.dropbox.core.DbxException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Created by yaohx on 3/24/2016.
 */
@Service
public class Bootstrapper {

    private IRegistrator _registrator;

    @Autowired
    public Bootstrapper(IRegistrator registrator)
    {
        _registrator = registrator;
    }

    public void startup() throws IOException, DbxException {
        _registrator.register();
    }
}
