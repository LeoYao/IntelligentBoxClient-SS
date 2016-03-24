package intelligentBoxClient.ss.bootstrapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by yaohx on 3/24/2016.
 */
@Component
public class Configuration {

    @Value("${cnsUrl}")
    private String _cnsUrl;

    @Value("${accessKey}")
    private String _accessKey;

    @Value("${server.port}")
    private int _serverPort;

    @Value("${dev:false}")
    private boolean _dev;

    public String getCnsUrl(){
        return _cnsUrl;
    }

    public String getAccessKey(){
        return _accessKey;
    }

    public int getServerPort(){
        return _serverPort;
    }

    public boolean getDevFlag()
    {
        return _dev;
    }
}
