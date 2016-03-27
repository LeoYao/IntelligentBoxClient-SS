package intelligentBoxClient.ss.listeners;

import intelligentBoxClient.ss.bootstrapper.Bootstrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Created by yaohx on 3/24/2016.
 */
@Component
public class RefreshListener implements ApplicationListener<ContextRefreshedEvent> {

    private static Log logger = LogFactory.getLog(RefreshListener.class);

    @Override
    public void onApplicationEvent(final ContextRefreshedEvent event) {
        ApplicationContext appCtx = event.getApplicationContext();
        Bootstrapper bootstrapper = appCtx.getBean(Bootstrapper.class);

        if (!bootstrapper.startup()) {
            logger.fatal("Failed to start up.");
            SpringApplication.exit(appCtx, new AbortExitCodeGenerator());
        }
    }

    class AbortExitCodeGenerator implements ExitCodeGenerator
    {
        @Override
        public int getExitCode() {
            return -1;
        }
    }
}
