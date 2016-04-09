package intelligentBoxClient.ss.listeners;

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
        genLru(appCtx);
    }

    class AbortExitCodeGenerator implements ExitCodeGenerator
    {
        @Override
        public int getExitCode() {
            return -1;
        }
    }

    public void genLru(ApplicationContext appCtx){
        IDirectoryDbContext ctx = appCtx.getBean(IDirectoryDbContext.class);

        while(ctx.popLru(true) != null);
/*
        ctx.pushLru("a", true);
        ctx.pushLru("b", true);
        ctx.pushLru("c", true);

        ctx.removeLru("b", true);
*/

        ctx.pushLru("/test0.txt", false);
        ctx.pushLru("/test1.txt", false);
        ctx.pushLru("/test2.txt", false);
    }


    public void genData(ApplicationContext appCtx){
        IDropboxClient dbxClient = appCtx.getBean(IDropboxClient.class);

        IDirectoryDbContext dirDbCtx = appCtx.getBean(IDirectoryDbContext.class);

        IConfiguration configuration = appCtx.getBean(IConfiguration.class);

        try {
            DirectoryEntity folderEntity = dirDbCtx.querySingleEntry("");
            if (folderEntity == null) {
                folderEntity = new DirectoryEntity();
                folderEntity.setFullPath("");
                folderEntity.setParentFolderFullPath(".");
                folderEntity.setEntryName("");
                folderEntity.setType(Consts.FOLDER);
                folderEntity.setMtime(new Timestamp(new Date().getTime()));
                folderEntity.setAtime(new Timestamp(new Date().getTime()));
                folderEntity.setLocal(true);
                dirDbCtx.insertEntry(folderEntity);
            }
/*
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
            }*/
        //} catch (DbxException e) {
        //    logger.error(e);
        } catch (SQLException e) {
            logger.error(e);
        }
    }
}
