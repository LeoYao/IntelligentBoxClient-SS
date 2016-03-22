package intelligentBoxClient.ss;

/**
 * Created by yaohx on 3/22/2016.
 */

import java.sql.SQLException;
import java.util.Locale;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxAuthInfo;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWebAuthNoRedirect;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import com.dropbox.core.v2.users.FullAccount;
import intelligentBoxClient.ss.dao.SqliteContext;
import org.sqlite.SQLiteErrorCode;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SychronizationService {
    private static String ACCESS_TOKEN = "SP6T7Dx26-AAAAAAAAAACb1_4Pj9I2QicCPlRuQk-WEB3JVGwTApOZJcPE24ImNw";

    public static void main(String args[]) throws DbxException, IOException {
        demo();
    }

    public static void demo() throws FileNotFoundException, IOException, UploadErrorException, DbxException {
        if (ACCESS_TOKEN == null || ACCESS_TOKEN.isEmpty()) {
            authorize();
        }

        DbxRequestConfig config = new DbxRequestConfig("dropbox/java-tutorial", "en_US");
        DbxClientV2 client = new DbxClientV2(config, ACCESS_TOKEN);
        FullAccount account = client.users().getCurrentAccount();
        System.out.println(account.getAccountId());

        // Upload "test.txt" to Dropbox
        try (InputStream in = new FileInputStream("test.txt")) {
            FileMetadata metadata = client.files().uploadBuilder("/test1/test.txt")
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(in);
        }

        ListFolderGetLatestCursorResult cursorResult =
                client.files().listFolderGetLatestCursorBuilder("").withRecursive(true).withIncludeDeleted(true).start();

        String cursor = cursorResult.getCursor();

        // Get files and folder metadata from Dropbox root directory
        boolean first = true;
        ListFolderResult listFolderResult = client.files().listFolder("");
        while (first || listFolderResult.getHasMore()) {
            first = false;
            for (Metadata metadata : listFolderResult.getEntries()) {
                if (metadata instanceof FolderMetadata) {
                    FolderMetadata folderMetadata = (FolderMetadata) metadata;
                    System.out.print(folderMetadata.getPathDisplay());
                    //System.out.println("\t" + folderMetadata.());
                } else if (metadata instanceof FileMetadata) {
                    FileMetadata fileMetadata = (FileMetadata) metadata;

                    System.out.print(fileMetadata.getPathDisplay());
                    System.out.print("\t" + fileMetadata.getRev());
                } else if (metadata instanceof DeletedMetadata) {
                    DeletedMetadata deletedMetadata = (DeletedMetadata) metadata;

                    System.out.print(deletedMetadata.getPathDisplay());
                    System.out.print("\tdeleted");
                }

                System.out.println();
            }
        }

        client.files().delete("/test1/test.txt");
        System.out.println();
        first = true;
        listFolderResult = client.files().listFolderContinue(cursor);
        while (first || listFolderResult.getHasMore()) {
            for (Metadata metadata : listFolderResult.getEntries()) {
                first = false;
                if (metadata instanceof FolderMetadata) {
                    FolderMetadata folderMetadata = (FolderMetadata) metadata;
                    System.out.print(folderMetadata.getPathDisplay());
                    //System.out.println("\t" + folderMetadata.());
                } else if (metadata instanceof FileMetadata) {
                    FileMetadata fileMetadata = (FileMetadata) metadata;

                    System.out.print(fileMetadata.getPathDisplay());
                    System.out.print("\t" + fileMetadata.getRev());
                } else if (metadata instanceof DeletedMetadata) {
                    DeletedMetadata deletedMetadata = (DeletedMetadata) metadata;

                    System.out.print(deletedMetadata.getPathDisplay());
                    System.out.print("\tdeleted");
                }

                System.out.println();
            }
        }
    }

    public static void authorize() throws IOException {

        String argAuthFileOutput = "token.txt";

        DbxAppInfo appInfo = new DbxAppInfo("d9m9s1iylifpqsx", "x2pfq4vkf5bytnq");
        String userLocale = Locale.getDefault().toString();
        DbxRequestConfig requestConfig = new DbxRequestConfig("examples-authorize", userLocale);

        DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(requestConfig, appInfo);

        String authorizeUrl = webAuth.start();
        System.out.println("1. Go to " + authorizeUrl);
        System.out.println("2. Click \"Allow\" (you might have to log in first).");
        System.out.println("3. Copy the authorization code.");
        System.out.print("Enter the authorization code here: ");

        String code = new BufferedReader(new InputStreamReader(System.in)).readLine();
        if (code == null) {
            System.exit(1);
            return;
        }
        code = code.trim();

        DbxAuthFinish authFinish;
        try {
            authFinish = webAuth.finish(code);
        } catch (DbxException ex) {
            System.err.println("Error in DbxWebAuth.start: " + ex.getMessage());
            System.exit(1);
            return;
        }

        System.out.println("Authorization complete.");
        System.out.println("- User ID: " + authFinish.getUserId());
        System.out.println("- Access Token: " + authFinish.getAccessToken());

        ACCESS_TOKEN = authFinish.getAccessToken();

        // Save auth information to output file.
        DbxAuthInfo authInfo = new DbxAuthInfo(authFinish.getAccessToken(), appInfo.getHost());
        try {
            DbxAuthInfo.Writer.writeToFile(authInfo, argAuthFileOutput);
            System.out.println("Saved authorization information to \"" + argAuthFileOutput + "\".");
        } catch (IOException ex) {
            System.err.println("Error saving to <auth-file-out>: " + ex.getMessage());
            System.err.println("Dumping to stderr instead:");
            DbxAuthInfo.Writer.writeToStream(authInfo, System.err);
            System.exit(1);
            return;
        }
    }

    static void sqlite(){
        SqliteContext ctx = new SqliteContext();
        try {
            ctx.connect("test.db");
            ctx.query();
            System.out.println("------------------");
            ctx.tx_insert();
            ctx.query();
            System.out.println("------------------");
            ctx.insert();
            ctx.query();
            System.out.println("------------------");
        } catch (SQLException e) {
            if (SQLiteErrorCode.SQLITE_BUSY.code == e.getErrorCode()) {
                System.out.println("locked");
            } else {
                e.printStackTrace();
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            ctx.disconnect();
        }
    }
}