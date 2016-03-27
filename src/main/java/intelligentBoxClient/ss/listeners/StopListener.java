package intelligentBoxClient.ss.listeners;

import intelligentBoxClient.ss.bootstrapper.Bootstrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

/**
 * Created by yaohx on 3/22/2016.
 */
@Component
public class StopListener implements ApplicationListener<ContextClosedEvent> {

    private static Log logger = LogFactory.getLog(RefreshListener.class);

    @Override
    public void onApplicationEvent(final ContextClosedEvent event) {
        ApplicationContext appCtx = event.getApplicationContext();
        Bootstrapper bootstrapper = appCtx.getBean(Bootstrapper.class);
        if (!bootstrapper.shutdown())
        {
            logger.warn("Failed to shutdown. Some resource may not fully clean up.");
        }
    }
}