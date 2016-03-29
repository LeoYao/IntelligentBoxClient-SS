package intelligentBoxClient.ss.controllers;

import intelligentBoxClient.ss.dao.INotificationDbContext;
import intelligentBoxClient.ss.messages.Notification;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;

/**
 * Created by yaohx on 3/24/2016.
 */
@RestController
public class NotificationController {

    private Log logger = LogFactory.getLog(this.getClass());
    private INotificationDbContext _notificationDbContext;

    @Autowired
    public NotificationController(INotificationDbContext notificationDbContext)    {
        _notificationDbContext = notificationDbContext;
    }
    @RequestMapping(value="/notify", method = RequestMethod.POST)
    public void notify(@RequestBody Notification notification)
    {
        try {
            _notificationDbContext.setRemoteChanged();
        } catch (Exception e) {
            logger.error("Failed to set REMOTE_CHANGED table.", e);
        }
    }
}
