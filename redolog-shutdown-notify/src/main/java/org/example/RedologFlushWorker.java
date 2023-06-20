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
        System.out.println("redologBackupPath : " + System.getProperty("redologBackupPath"));
        //String[] args = {"RemoteCopyfiles","--scriptLocation=C:\\GigaSpaces\\XAP.NET-16.3.0-patch-p-3-x64\\NET v4.0\\automation-redo-log\\ ","--spaceName=dataExampleSpace","--redologBackupPath=C:\\GigaSpaces\\XAP.NET-16.3.0-patch-p-3-x64\\NET v4.0\\backup\\work\\redo-log ","--sourceBaseDirCopyPath=C:\\GigaSpaces\\XAP.NET-16.3.0-patch-p-3-x64\\NET v4.0\\Work\\redo-log\\ "};
        String[] args = {"RemoteCopyfiles","--configLocation=" + System.getProperty("configLocation"), "--resourceLocation=" + System.getProperty("resourceLocation"), "--spaceName=" + System.getProperty("spaceName"), "--redologBackupPath=" + System.getProperty("redologBackupPath"), "--sourceBaseDirCopyPath=" + System.getProperty("sourceBaseDirCopyPath"), "--hostFileName=" + System.getProperty("hostFileName")};
        RemoteCommandExecutor.main(args);
    }

    public static void main(String[] arg1s) {
        String[] args = {"RemoteCopyfiles","--configLocation=C:\\GigaSpaces\\XAP.NET-16.3.0-patch-p-3-x64\\NET v4.0\\RecoveryAssistant\\config\\ ","--resourceLocation=C:\\GigaSpaces\\XAP.NET-16.3.0-patch-p-3-x64\\NET v4.0\\RecoveryAssistant\\resources\\ ","--spaceName=dataExampleSpace","--redologBackupPath=C:\\GigaSpaces\\XAP.NET-16.3.0-patch-p-3-x64\\NET v4.0\\backup\\work\\redo-log ","--sourceBaseDirCopyPath=C:\\GigaSpaces\\XAP.NET-16.3.0-patch-p-3-x64\\NET v4.0\\Work\\redo-log\\ ", "--hostFileName=spaceHosts.json"};
        //String[] args = {"RemoteCopyfiles","--scriptLocation=C:\\GigaSpaces\\XAP.NET-16.3.0-patch-p-3-x64\\NET v4.0\\automation-redo-log\\ ","--spaceName=dataExampleSpace","--redologBackupPath=C:\\GigaSpaces\\XAP.NET-16.3.0-patch-p-3-x64\\NET v4.0\\backup\\work\\redo-log ","--sourceBaseDirCopyPath=C:\\GigaSpaces\\XAP.NET-16.3.0-patch-p-3-x64\\NET v4.0\\Work\\redo-log\\ ", "--hostFileName=spaceHosts.txt"};
        //String[] args = {"RemoteCopyfiles", "--scriptLocation=" + System.getProperty("scriptLocation"), "--spaceName=" + System.getProperty("spaceName"), "--redologBackupPath=" + System.getProperty("redologBackupPath"), "--sourceBaseDirCopyPath=" + System.getProperty("sourceBaseDirCopyPath"), "--hostFileName=" + System.getProperty("hostFileName")};
        RemoteCommandExecutor.main(args);
    }

}

