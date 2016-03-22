package intelligentBoxClient.ss.components;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

/**
 * Created by yaohx on 3/22/2016.
 */
@Component
public class StopListener implements ApplicationListener<ContextClosedEvent> {

    @Override
    public void onApplicationEvent(final ContextClosedEvent event) {
        //System.out.println("Stopped: " + event);
    }
}