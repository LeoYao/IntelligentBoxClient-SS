package intelligentBoxClient.ss.dropbox.utils;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.UploadUploader;
import intelligentBoxClient.ss.bootstrapper.IConfiguration;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardOpenOption.*;
/**
 * Created by yaohx on 3/31/2016.
 */
public class Download extends RetryClientOperation<FileMetadata> {

    private String _localPath;
    private String _remotePath;

    public Download(IConfiguration configuration, DbxClientV2 client, String localPath, String remotePath) {
        super(configuration, client);
        _localPath = localPath;
        _remotePath = remotePath;
    }

    @Override
    protected FileMetadata run() throws DbxException {
        FileMetadata result = null;
        DbxDownloader<FileMetadata> downloader = null;
        OutputStream out = null;
        try {
            downloader = _client.files().download(_remotePath);
            Path localOutput = Paths.get(_localPath);
            out = new BufferedOutputStream(
                    Files.newOutputStream(localOutput, CREATE, TRUNCATE_EXISTING));
            result = downloader.download(out);
        } catch (IOException e) {
            logger.error(e);
            return null;
        } finally {
            if (out != null){
                try {
                    out.close();
                } catch (IOException e) {
                    logger.warn("Failed to close output stream of [" + _localPath + "].", e);
                }
            }
            if (downloader != null) {
                downloader.close();
            }
        }
        return result;
    }
}
