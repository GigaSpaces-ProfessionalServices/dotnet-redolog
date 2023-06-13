package org.example;

import com.gigaspaces.utils.RedologFlushNotifier;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Path;
import java.util.List;

public class RedologFlushWorker implements RedologFlushNotifier {


    @Override
    public void notifyOnFlush(String fullSpaceName, String spaceName, long redologsize, Path redologPath) {
        System.out.println("==========================================");
        System.out.println("notifyOnFlush was called with: fullSpaceName=" + fullSpaceName + " spaceName=" + spaceName + " redologSize=" + redologsize + " redologPath=" + redologPath.toFile().getAbsolutePath());
        System.out.println("scriptLocation : " + System.getProperty("scriptLocation"));
        System.out.println("redologBackupPath : " + System.getProperty("redologBackupPath"));
        //String[] args = {"RemoteCopyfiles","--scriptLocation=C:\\GigaSpaces\\smart-cache.net-16.2.1-x64\\NET v4.0\\automation-redo-log\\ ","--spaceName=dataExampleSpace","--redologBackupPath=C:\\GigaSpaces\\smart-cache.net-16.2.1-x64\\NET v4.0\\backup\\work\\redo-log ","--sourceBaseDirCopyPath=C:\\GigaSpaces\\smart-cache.net-16.2.1-x64\\NET v4.0\\Work\\redo-log\\ "};
        String[] args = {"RemoteCopyfiles", "--scriptLocation=" + System.getProperty("scriptLocation"), "--spaceName=" + System.getProperty("spaceName"), "--redologBackupPath=" + System.getProperty("redologBackupPath"), "--sourceBaseDirCopyPath=" + System.getProperty("sourceBaseDirCopyPath"), "--hostFileName=" + System.getProperty("hostFileName")};
        RemoteCommandExecutor.main(args);
    }

    public static void main(String[] arg1s) {
        String[] args = {"RemoteCopyfiles","--scriptLocation=C:\\GigaSpaces\\smart-cache.net-16.2.1-x64\\NET v4.0\\automation-redo-log\\ ","--spaceName=dataExampleSpace","--redologBackupPath=C:\\GigaSpaces\\smart-cache.net-16.2.1-x64\\NET v4.0\\backup\\work\\redo-log ","--sourceBaseDirCopyPath=C:\\GigaSpaces\\smart-cache.net-16.2.1-x64\\NET v4.0\\Work\\redo-log\\ ", "--hostFileName=spaceHosts.txt"};
        //String[] args = {"RemoteCopyfiles", "--scriptLocation=" + System.getProperty("scriptLocation"), "--spaceName=" + System.getProperty("spaceName"), "--redologBackupPath=" + System.getProperty("redologBackupPath"), "--sourceBaseDirCopyPath=" + System.getProperty("sourceBaseDirCopyPath"), "--hostFileName=" + System.getProperty("hostFileName")};
        RemoteCommandExecutor.main(args);
    }

}
