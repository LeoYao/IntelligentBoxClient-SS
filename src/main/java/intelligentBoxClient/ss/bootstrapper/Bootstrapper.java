package intelligentBoxClient.ss.bootstrapper;

import com.dropbox.core.DbxException;
import intelligentBoxClient.ss.workers.ISychronizationWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Created by yaohx on 3/24/2016.
 */
@Service
public class Bootstrapper {

    private IRegistrator _registrator;
    private ISychronizationWorker _sychronizationWorker;

    @Autowired
    public Bootstrapper(IRegistrator registrator, ISychronizationWorker sychronizationWorker)
    {
        _registrator = registrator;
        _sychronizationWorker = sychronizationWorker;
    }

    public boolean startup() {

        return _registrator.register() && _sychronizationWorker.start();
    }

    public boolean shutdown()
    {
        boolean result = true;
        result &= _registrator.unregister();
        _sychronizationWorker.stop();
        return result;
    }
}
