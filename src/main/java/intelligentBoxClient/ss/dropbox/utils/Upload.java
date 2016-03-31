package intelligentBoxClient.ss.dropbox.utils;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.UploadUploader;
import com.sun.corba.se.spi.orbutil.fsm.Input;
import intelligentBoxClient.ss.bootstrapper.IConfiguration;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by yaohx on 3/31/2016.
 */
public class Upload extends RetryClientOperation<FileMetadata> {

    private String _localPath;
    private String _remotePath;

    public Upload(IConfiguration configuration, DbxClientV2 client, String localPath, String remotePath) {
        super(configuration, client);
        _localPath = localPath;
        _remotePath = remotePath;
    }

    @Override
    protected FileMetadata run() throws DbxException {
        FileMetadata result = null;
        UploadUploader uploader = null;
        InputStream in = null;
        try {
            uploader = _client.files().upload(_remotePath);
            Path localInput = Paths.get(_localPath);
            in = new BufferedInputStream(Files.newInputStream(localInput));
            result = uploader.uploadAndFinish(in);
        } catch (IOException e) {
            logger.error(e);
            return null;
        } finally {
            if (in != null){
                try {
                    in.close();
                } catch (IOException e) {
                    logger.warn("Failed to close input stream of [" + _localPath + "].", e);
                }
            }

            if (uploader != null) {
                uploader.close();
            }
        }
        return result;
    }
}
