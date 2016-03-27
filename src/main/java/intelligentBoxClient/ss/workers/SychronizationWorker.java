package intelligentBoxClient.ss.workers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

/**
 * Created by Leo on 3/27/16.
 */
@Service
public class SychronizationWorker implements Runnable, ISychronizationWorker {

    private Thread thread;
    private static Log logger = LogFactory.getLog(SychronizationWorker.class);
    @Override
    public void run() {

        try {
            while (true) {
                logger.debug("Running....");
                Thread.sleep(2000);

            }
        } catch (InterruptedException e) {
            logger.debug(e);
        }
    }

    public boolean start()
    {
        thread = new Thread(this);
        thread.start();

        return true;
    }

    public boolean stop() {
        if (thread != null && thread.isAlive())
        {
            thread.interrupt();

            try {
                thread.join();
            } catch (InterruptedException e) {
                logger.error(e);
            }
        }

        return true;
    }
}
