package intelligentBoxClient.ss.listeners;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;
import intelligentBoxClient.ss.bootstrapper.Bootstrapper;
import intelligentBoxClient.ss.dropbox.IDropboxClient;
import intelligentBoxClient.ss.persistence.IDirectoryDbSaver;
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

        //genData(appCtx);
    }

    class AbortExitCodeGenerator implements ExitCodeGenerator
    {
        @Override
        public int getExitCode() {
            return -1;
        }
    }

    public void genData(ApplicationContext appCtx){
        IDropboxClient dbxClient = appCtx.getBean(IDropboxClient.class);
        IDirectoryDbSaver dirDbSaver = appCtx.getBean(IDirectoryDbSaver.class);

        for (int i = 0; i < 10; ++i) {
            try {
                String remotePath = "/testData/test" + i + ".txt";
                FileMetadata metadata = dbxClient.uploadFile(remotePath, "C:\\Dev_Repos\\ss\\data\\testData\\test" + i + ".txt");
                dirDbSaver.save(metadata);
            } catch (DbxException e) {
                logger.error(e);
            }
        }
    }
}
