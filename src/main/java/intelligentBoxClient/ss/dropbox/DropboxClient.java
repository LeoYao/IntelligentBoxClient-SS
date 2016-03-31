package intelligentBoxClient.ss.dropbox;

import com.dropbox.core.*;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import com.dropbox.core.v2.users.FullAccount;
import intelligentBoxClient.ss.bootstrapper.IConfiguration;
import intelligentBoxClient.ss.dropbox.utils.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import static java.nio.file.StandardOpenOption.*;

/**
 * Created by yaohx on 3/29/2016.
 */
@Service
public class DropboxClient implements IDropboxClient {

    private Log logger = LogFactory.getLog(this.getClass());

    private IConfiguration _configuration;
    private String _cursor = null;
    private String _accessToken = null;
    private DbxClientV2 _client;

    @Autowired
    public DropboxClient(IConfiguration configuration)
    {
        _configuration = configuration;
    }

    @Override
    public boolean open() {
        boolean result = true;
        if (initAccessToken()) {
            result &= initClient();
            result &= initCursor();
        }
        else {
            return false;
        }

        return result;
    }

    @Override
    public boolean close() {
        save();
        return true;
    }

    @Override
    public void save(){
        saveCursor();
    }

    @Override
    public String getAccountId() throws DbxException {
        GetCurrentAccount getCurrentAccount = new GetCurrentAccount(_configuration, _client);
        FullAccount account = getCurrentAccount.execute();
        return account.getAccountId();
    }

    @Override
    public Metadata getFileMetadata(String path) throws DbxException {
        GetMetadata getMetadata = new GetMetadata(_configuration, _client, path);
        Metadata metadata = getMetadata.execute();
        return metadata;
    }

    @Override
    public List<Metadata> getFileMetadatas(String path) throws DbxException {
        List<Metadata> results = new LinkedList<>();

        boolean first = true;
        ListFolderResult listFolderResult = null;
        ListFolder listFolder = new ListFolder(_configuration, _client, path);

        while (first || (listFolderResult != null && listFolderResult.getHasMore())) {
            first = false;
            listFolderResult = listFolder.execute();
            for (Metadata metadata : listFolderResult.getEntries()) {
                results.add(metadata);
            }
        }

        return results;
    }

    @Override
    public List<Metadata> getChanges() throws DbxException {
        List<Metadata> results = new LinkedList<>();

        boolean first = true;
        ListFolderResult listFolderResult = null;
        ListFolder listFolder = new ListFolder(_configuration, _client, "", _cursor);

        while (first || (listFolderResult != null && listFolderResult.getHasMore())) {
            first = false;
            listFolderResult = listFolder.execute();
            _cursor = listFolderResult.getCursor();
            for (Metadata metadata : listFolderResult.getEntries()) {
                results.add(metadata);
            }
        }

        return results;
    }

    @Override
    public FileMetadata downloadFile(String remotePath, String localPath) throws DbxException{
        Download download = new Download(_configuration, _client, localPath, remotePath);
        FileMetadata result = download.execute();
        return result;
    }

    public FileMetadata uploadFile(String remotePath, String localPath) throws DbxException {
        Upload upload = new Upload(_configuration, _client, localPath, remotePath);
        FileMetadata result = upload.execute();
        return result;
    }

    private boolean initAccessToken() {
        loadAccessToken();
        if (_accessToken != null && _accessToken.trim().length() > 0)
        {
            return true;
        }

        createAccessToken();
        return _accessToken != null && _accessToken.trim().length() > 0;
    }

    private void loadAccessToken() {
        String tokenFileOutput = _configuration.getDbxTokenFilePath();

        Path tokenFilePath = Paths.get(tokenFileOutput);
        if (Files.exists(tokenFilePath)) {
            Charset charset = Charset.forName("US-ASCII");
            try (BufferedReader reader = Files.newBufferedReader(tokenFilePath, charset)) {
                _accessToken = reader.readLine();
            } catch (IOException ex) {
                logger.error("Failed to read token file [" + tokenFileOutput + "]", ex);
                return;
            }
        }

        logger.info("Previous persisted access token is loaded successfully.");
    }

    private void createAccessToken() {
        DbxAppInfo appInfo = new DbxAppInfo("d9m9s1iylifpqsx", "x2pfq4vkf5bytnq");

        String userLocale = Locale.getDefault().toString();
        DbxRequestConfig requestConfig = new DbxRequestConfig("authorize", userLocale);

        DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(requestConfig, appInfo);

        String authorizeUrl = webAuth.start();
        System.out.println("1. Go to " + authorizeUrl);
        System.out.println("2. Click \"Allow\" (you might have to log in first).");
        System.out.println("3. Copy the authorization code.");
        System.out.print("Enter the authorization code here: ");

        String code = null;
        try {
            code = new BufferedReader(new InputStreamReader(System.in)).readLine();
        } catch (IOException e) {
            logger.error("Failed to read authorization code.", e);
            return;
        }
        if (code == null) {
            logger.error("The entered authorization code is null.");
            return;
        }
        code = code.trim();

        DbxAuthFinish authFinish;
        Auth auth = new Auth(_configuration, webAuth, code);
        try {
            authFinish = auth.execute();
        } catch (DbxException ex) {
            logger.error("Error in DbxWebAuth.finish.", ex);
            return;
        }
        _accessToken = authFinish.getAccessToken();
        logger.info("Access token is retrieved successfully.");

        // Save access token to file.
        String tokenFileOutput = _configuration.getDbxTokenFilePath();
        Path tokenFilePath = Paths.get(tokenFileOutput);
        if (!Files.exists(tokenFilePath)) {
            try {
                Files.createFile(tokenFilePath);
            } catch (IOException ex) {
                logger.warn("Failed to create token file [" + tokenFilePath + "]", ex);
                return;
            }
        }

        Charset charset = Charset.forName("US-ASCII");
        try (BufferedWriter writer = Files.newBufferedWriter(tokenFilePath, charset)) {
            writer.write(_accessToken, 0, _accessToken.length());
        } catch (IOException ex) {
            logger.warn("Failed to save access token to [" + tokenFileOutput + "]", ex);
            return;
        }
    }

    private boolean initCursor() {

        loadCursor();
        if (_cursor != null && _cursor.trim().length() > 0)
        {
            return true;
        }

        createCursor();
        if (_cursor != null && _cursor.trim().length() > 0)
        {
            saveCursor();
            return true;
        }
        return false;
    }

    private void loadCursor() {
        String cursorFileOutput = _configuration.getDbxCursorFilePath();

        Path cursorFilePath = Paths.get(cursorFileOutput);
        if(Files.exists(cursorFilePath)) {
            Charset charset = Charset.forName("US-ASCII");
            try (BufferedReader reader = Files.newBufferedReader(cursorFilePath, charset)) {
                _cursor = reader.readLine();
            } catch (IOException ex) {
                logger.error("Failed to read cursor file [" + cursorFileOutput + "]", ex);
                return;
            }
        }

        logger.info("Previous persisted cursor is loaded successfully.");
    }

    private void createCursor() {
        try {
            GetLatestCursor getLatestCursor = new GetLatestCursor(_configuration, _client);
            ListFolderGetLatestCursorResult cursorResult = getLatestCursor.execute();
            _cursor = cursorResult.getCursor();
        } catch (DbxException ex) {
            logger.error("Failed to get latest cursor.", ex);
            return;
        }

        logger.info("Latest cursor is retrieved successfully.");
    }

    private void saveCursor(){
        // Save access token to file.
        String cursorFileOutput = _configuration.getDbxCursorFilePath();
        Path cursorFilePath = Paths.get(cursorFileOutput);
        if(!Files.exists(cursorFilePath)) {
            try {
                Files.createFile(cursorFilePath);
            } catch (IOException ex) {
                logger.warn("Failed to create cursor file [" + cursorFilePath + "]", ex);
                return;
            }
        }
        Charset charset = Charset.forName("US-ASCII");
        try (BufferedWriter writer = Files.newBufferedWriter(cursorFilePath, charset)) {
            writer.write(_cursor, 0, _cursor.length());
        } catch (IOException ex) {
            logger.warn("Failed to save cursor to [" + cursorFileOutput + "]", ex);
            return;
        }
    }

    private boolean initClient()
    {
        DbxRequestConfig config = new DbxRequestConfig("SynchronizationService", "en_US");

        _client = new DbxClientV2(config, _accessToken);
        logger.info("Client is initalized successfully.");

        return true;
    }

}
