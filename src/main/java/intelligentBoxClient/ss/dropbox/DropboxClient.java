package intelligentBoxClient.ss.dropbox;

import com.dropbox.core.*;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import com.dropbox.core.v2.users.FullAccount;
import intelligentBoxClient.ss.bootstrapper.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.Locale;

/**
 * Created by yaohx on 3/29/2016.
 */
@Service
public class DropboxClient implements IDropboxClient {

    private Log logger = LogFactory.getLog(this.getClass());

    private Configuration _configuration;
    private String _cursor = null;
    private String _accessToken = null;
    private DbxClientV2 _client;

    @Autowired
    public DropboxClient(Configuration configuration)
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
        return true;
    }

    public String getAccountId() throws DbxException {
        FullAccount account = _client.users().getCurrentAccount();
        return account.getAccountId();
    }

    public Metadata getFileMetadata(String path) throws DbxException {
        Metadata metadata = _client.files().getMetadata(path);
        return metadata;
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

    private void loadAccessToken()
    {
        String tokenFileOutput = _configuration.getDbxTokenFilePath();

        Path tokenFilePath = Paths.get(tokenFileOutput);
        if(Files.exists(tokenFilePath)) {
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

    private void createAccessToken()
    {
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
        try {
            authFinish = webAuth.finish(code);
        } catch (DbxException ex) {
            logger.error("Error in DbxWebAuth.finish.", ex);
            return;
        }
        _accessToken = authFinish.getAccessToken();
        logger.info("Access token is retrieved successfully.");

        // Save access token to file.
        String tokenFileOutput = _configuration.getDbxTokenFilePath();
        Path tokenFilePath = Paths.get(tokenFileOutput);
        if(!Files.exists(tokenFilePath)) {
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
        return _cursor != null && _cursor.trim().length() > 0;
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
            ListFolderGetLatestCursorResult cursorResult = _client.files().listFolderGetLatestCursorBuilder("").withRecursive(true).withIncludeDeleted(true).start();
            _cursor = cursorResult.getCursor();
        } catch (DbxException ex) {
            logger.error("Failed to get latest cursor.", ex);
            return;
        }

        logger.info("Latest cursor is retrieved successfully.");

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
