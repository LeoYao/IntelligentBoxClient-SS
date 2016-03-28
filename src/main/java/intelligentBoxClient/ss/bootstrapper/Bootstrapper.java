package intelligentBoxClient.ss.bootstrapper;

import com.dropbox.core.DbxException;
import intelligentBoxClient.ss.dao.ISqliteContext;
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
    private ISqliteContext _sqliteContext;
    private Configuration _config;

    @Autowired
    public Bootstrapper(IRegistrator registrator,
                        ISychronizationWorker sychronizationWorker,
                        ISqliteContext sqliteContext,
                        Configuration config)
    {
        _registrator = registrator;
        _sychronizationWorker = sychronizationWorker;
        _sqliteContext = sqliteContext;
        _config = config;
    }

    public boolean startup() {

        return _registrator.register()
                && _sychronizationWorker.start()
                && _sqliteContext.open(_config.getDirDbFilePath());
    }

    public boolean shutdown()
    {
        boolean result = true;
        result &= _registrator.unregister();
        _sychronizationWorker.stop();
        result &= _sqliteContext.close();
        return result;
    }
}
