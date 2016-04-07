package intelligentBoxClient.ss.utils;

import org.springframework.stereotype.Service;

/**
 * Created by Leo on 4/5/16.
 */
@Service
public class Utils implements IUtils {
    @Override
    public String extractParentFolderPath(String fullPath, String fileName) {
        int fullPathLength = fullPath.length();
        int fileNameLength = fileName.length();
        if (fileNameLength == fullPathLength) {
            //root
            return ".";
        }
        return fullPath.substring(0, fullPathLength - fileNameLength - 1);
    }
}
