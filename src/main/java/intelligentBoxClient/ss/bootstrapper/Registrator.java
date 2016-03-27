package intelligentBoxClient.ss.bootstrapper;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.users.FullAccount;
import intelligentBoxClient.ss.messages.RegistrationRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

/**
 * Created by yaohx on 3/24/2016.
 */
@Service
public class Registrator implements IRegistrator {

    private static Log logger = LogFactory.getLog(Registrator.class);

    @Autowired
    private Configuration _configuration;


    public boolean register() {

        if (_configuration.getDevFlag())
        {
            return true;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            RegistrationRequest request = new RegistrationRequest();
            String accountId = getAccountId();
            String callbackUrl = getCallbackUrl();
            request.setAccountId(accountId);
            request.setCallbackUrl(callbackUrl);
            ResponseEntity<Object> response =
                    restTemplate.postForEntity("http://" + _configuration.getCnsUrl() + "/register",
                            request,
                            Object.class);
            if (HttpStatus.OK != response.getStatusCode()) {
                logger.error("Failed to register on CNS. HTTP Status Code: "
                        + response.getStatusCode());
                return false;
            }
        }
        catch (DbxException e)
        {
            logger.error(e);
            return false;
        }
        catch (IOException e)
        {
            logger.error(e);
            return false;
        }
        return true;
    }

    public boolean unregister()
    {
        if (_configuration.getDevFlag())
        {
            return true;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            RegistrationRequest request = new RegistrationRequest();
            request.setAccountId(getAccountId());
            ResponseEntity<Object> response =
                    restTemplate.postForEntity("http://" + _configuration.getCnsUrl() + "/unregister",
                            request,
                            Object.class);
            if (HttpStatus.OK != response.getStatusCode()) {
                logger.error("Failed to register on CNS. HTTP Status Code: "
                        + response.getStatusCode());
                return false;
            }
        }
         catch (DbxException e) {
             logger.error(e);
             return false;
        }

        return true;
    }

    private String getAccountId() throws DbxException {
        String accessKey = _configuration.getAccessKey();

        DbxRequestConfig config = new DbxRequestConfig("registration/getAccountId", "en_US");

        DbxClientV2 client = new DbxClientV2(config, accessKey);
        FullAccount account = client.users().getCurrentAccount();
        return account.getAccountId();
    }

    private String getCurrentHost() throws IOException {
        String url = "http://169.254.169.254/latest/meta-data/public-hostname";
        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("Accept-Charset", StandardCharsets.UTF_8.name());
        InputStream content = connection.getInputStream();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(content, StandardCharsets.UTF_8.name()));
            String host = reader.readLine();
            return host;
        }
        finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private String getCallbackUrl() throws IOException {
        return getCurrentHost() + ":" + _configuration.getServerPort();
    }
}
