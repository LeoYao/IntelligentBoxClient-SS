package intelligentBoxClient.ss.controllers;

import intelligentBoxClient.ss.messages.Notification;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by yaohx on 3/24/2016.
 */
@RestController
public class NotificationController {

    private static Log logger = LogFactory.getLog(NotificationController.class);

    @RequestMapping(value="/notify", method = RequestMethod.POST)
    public void notify(@RequestBody Notification notification)
    {
        logger.info(notification.getAccountId());
    }
}
