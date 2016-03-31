package intelligentBoxClient.ss.dropbox.utils;

import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxWebAuthNoRedirect;
import com.dropbox.core.v2.DbxClientV2;
import intelligentBoxClient.ss.bootstrapper.IConfiguration;

/**
 * Created by yaohx on 3/31/2016.
 */
public class Auth extends RetryOperation<DbxAuthFinish> {

    private DbxWebAuthNoRedirect _webAuth;
    private String _code;

    public Auth(IConfiguration configuration, DbxWebAuthNoRedirect webAuth, String code) {
        super(configuration);
        _webAuth = webAuth;
        _code = code;
    }

    @Override
    protected DbxAuthFinish run() throws DbxException {
        return _webAuth.finish(_code);
    }
}
