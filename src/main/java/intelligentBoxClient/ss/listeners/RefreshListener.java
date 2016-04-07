package intelligentBoxClient.ss.listeners;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;
import intelligentBoxClient.ss.bootstrapper.Bootstrapper;
import intelligentBoxClient.ss.bootstrapper.IConfiguration;
import intelligentBoxClient.ss.dao.IDirectoryDbContext;
import intelligentBoxClient.ss.dao.pojo.DirectoryEntity;
import intelligentBoxClient.ss.dropbox.IDropboxClient;
import intelligentBoxClient.ss.utils.Consts;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

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

        genData(appCtx);
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

        IDirectoryDbContext dirDbCtx = appCtx.getBean(IDirectoryDbContext.class);

        IConfiguration configuration = appCtx.getBean(IConfiguration.class);

        try {
            DirectoryEntity folderEntity = dirDbCtx.querySingleEntry("/testdata");
            if (folderEntity == null){
                folderEntity = new DirectoryEntity();
                folderEntity.setFullPath("/testdata");
                folderEntity.setParentFolderFullPath("/");
                folderEntity.setEntryName("testdata");
                folderEntity.setType(Consts.FOLDER);
                folderEntity.setMtime(new Timestamp(new Date().getTime()));
                folderEntity.setAtime(new Timestamp(new Date().getTime()));
                folderEntity.setLocal(true);
                dirDbCtx.insertEntry(folderEntity);
            }

            for (int i = 0; i < 3; ++i) {

                String remotePath = "/testdata/test" + i + ".txt";
                FileMetadata metadata = dbxClient.uploadFile(remotePath, configuration.getDataFolderPath() + "testdata/test" + i + ".txt");
                DirectoryEntity directoryEntity = dirDbCtx.querySingleEntry(remotePath);
                if (directoryEntity == null) {
                    directoryEntity = new DirectoryEntity(metadata);
                    dirDbCtx.insertEntry(directoryEntity);
                } else {
                    directoryEntity = new DirectoryEntity(metadata, directoryEntity);
                    dirDbCtx.updateEntry(directoryEntity);
                }
            }
        } catch (DbxException e) {
            logger.error(e);
        } catch (SQLException e) {
            logger.error(e);
        }
    }
}
