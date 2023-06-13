package org.example;

import com.jcraft.jsch.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class RemoteCommandExecutor {
    String scriptLocation;
    String spaceName;
    String sourceBaseDirCopyPath;
    String redologBackupPath;
    String hostFileName;

    public static void main(String[] args) {
        RemoteCommandExecutor remoteCommandExecutor = new RemoteCommandExecutor();
        if(args.length>0){
            remoteCommandExecutor.processArgs(args);
            if ("RemoteCopyfiles".equalsIgnoreCase(args[0])) {
                remoteCommandExecutor.copyFilesToRemote();
            }
        } else {
            System.out.println("No args passes");
            printUsage();
        }
    }

    public void processArgs(String[] args) {
        try {
            int i = 1;
            while (i < args.length) {
                String s = args[i];
                System.out.println(i+" -> "+s);
                i++;
            }
            i = 1;
            while (i < args.length) {
                String s = args[i].trim();
                String sUpper = s.toUpperCase();
               // System.out.println(i+" -> "+sUpper);
                if (sUpper.startsWith("--help".toUpperCase())) {
                    printUsage();
                    System.exit(0);
                } else if (sUpper.startsWith("--sourceBaseDirCopyPath".toUpperCase())) {
                    String[] sArray = s.split("=", 2);
                    sourceBaseDirCopyPath = sArray[1];
                } else if (sUpper.startsWith("--redologBackupPath".toUpperCase())) {
                    String[] sArray = s.split("=", 2);
                    redologBackupPath = sArray[1];
                } else if (sUpper.startsWith("--scriptLocation".toUpperCase())) {
                    String[] sArray = s.split("=", 2);
                    scriptLocation = sArray[1];
                } else if (sUpper.startsWith("--spaceName".toUpperCase())) {
                    String[] sArray = s.split("=", 2);
                    spaceName = sArray[1];
                } else if (sUpper.startsWith("--hostFileName".toUpperCase())) {
                    String[] sArray = s.split("=", 2);
                    hostFileName = sArray[1];
                } else {
                    System.out.println("Please enter valid arguments.");
                    printUsage();
                    System.exit(-1);
                }
                i++;
            }
        } catch (Exception e) {
            e.printStackTrace();
            printUsage();
            System.exit(-1);
        }
    }

    public static void printUsage() {
        System.out.println("This program flushes the redo log to disk.");
        System.out.println("The following arguments are accepted:");
        System.out.println("  --spaceName=<space name>");
        System.out.println("    The name of the space to connect to. This argument is required.");

        System.out.println("  --gsHome=<xap home>");
        System.out.println("    Path of installed gigaspaces smart cache. This argument is required.");
        System.out.println("  --scriptLocation=<script location>");
        System.out.println("    Path of recovery scripts. This argument is required.");
        System.out.println("  --targetDir=<target directory to copy redo log files>");
        System.out.println("    The name of the space to connect to. This argument is required.");
        System.out.println("  --targetPathBaseDir=target directory to copy redo log files till backup");
        System.out.println("    The name of the space to connect to. This argument is required.");
        System.out.println("  --deserializeFullPath=<Deserialize yaml file full path>");
        System.out.println("    The name of the space to connect to. This argument is required.");

        System.out.println("  --help");
        System.out.println("    Display this help message and exit.");
        System.out.println();
        /*System.out.println("In addition, the following Java System Properties should be set:");
        System.out.println("  -Dcom.gs.jini_lus.groups=<lookup group>");
        System.out.println("  -Dcom.gs.jini_lus.locators=<lookup locators");*/
    }

    void copyFilesToRemote() {
        JSONParser parser = new JSONParser();
        JSONArray configArray;
        try {
            configArray = (JSONArray) parser.parse(new FileReader(scriptLocation + "\\" + hostFileName));
        } catch (Exception e) {
            System.out.println("Failed to read configuration file: " + e.getMessage());
            return;
        }
        for (Object jsonObject : configArray) {
            JSONObject hostDetail = (JSONObject) jsonObject;
            String host = (String) hostDetail.get("host");
            String user = (String) hostDetail.get("username");
            String password = (String) hostDetail.get("password");
            if(isServerAvailable(host,22,user,password,5)){
                copyFilesToRemote(host, user, password, sourceBaseDirCopyPath, redologBackupPath);
                break;
            } else {
                System.out.println("host : "+host+" is not available");
            }
        }
    }

    static void copyFilesToRemote(String host, String user, String password, String remotePath, String localPath) {
        try {
            File directory = new File(remotePath.replace("\\\\","\\"));

            if (directory.exists() && directory.isDirectory()) {
                File[] files = directory.listFiles();

                if (files != null) {
                    JSch jsch = new JSch();
                    Session session = jsch.getSession(user, host, 22);
                    session.setPassword(password);
                    session.setConfig("StrictHostKeyChecking", "no");
                    session.connect();

                    ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
                    channelSftp.connect();
                    StringBuilder currentPath = new StringBuilder();
                    for (String folder : localPath.split("\\\\")) {
                        if (!folder.isEmpty()) {
                            currentPath.append("\\").append(folder);
                            try {
                                channelSftp.cd(currentPath.toString().replace("\\", "/"));
                            } catch (SftpException e) {
                                channelSftp.mkdir(currentPath.toString().replace("\\", "/"));
                                channelSftp.cd(currentPath.toString().replace("\\", "/"));
                            }
                        }
                    }
                    System.out.println("copying from : /" +directory.getAbsolutePath().replace("\\", "/")+ ", to : /" + localPath.replace("\\", "/"));
                    copyFolder(channelSftp, "/"+directory.getAbsolutePath().replace("\\", "/"), "/"+localPath.replace("\\", "/"));
                    //channelSftp.put("/"+directory.getAbsolutePath().replace("\\", "/"), "/"+localPath.replace("\\", "/"));
                    /* for (File file : files) {
                        System.out.println("copying from : "+file.getAbsolutePath()+ ", to : "+localPath.replace("\\\\","\\"));
                        channelSftp.put(file.getAbsolutePath(), localPath.replace("\\\\","\\"));
                    }*/
                    channelSftp.disconnect();
                    session.disconnect();
                } else {
                    System.out.println("No files found in directory.");
                }
            } else {
                System.out.println("Directory does not exist or is not a directory.");
            }
        } catch (JSchException | SftpException e) {
            e.printStackTrace();
        }
    }

    private static void copyFolder(ChannelSftp channelSftp, String sourcePath, String destinationPath) throws SftpException {
        File sourceFolder = new File(sourcePath);
        File[] files = sourceFolder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    channelSftp.put("//"+file.getAbsolutePath().replace("\\", "/"), destinationPath + "/" + file.getName(), ChannelSftp.OVERWRITE);
                } else if (file.isDirectory()) {
                    String subDirectoryPath = destinationPath + "/" + file.getName();
                    try {
                        channelSftp.cd(subDirectoryPath.toString().replace("\\", "/"));
                    } catch (SftpException e) {
                        channelSftp.mkdir(subDirectoryPath.toString().replace("\\", "/"));
                        channelSftp.cd(subDirectoryPath.toString().replace("\\", "/"));
                    }
                    //channelSftp.mkdir(subDirectoryPath);
                    //channelSftp.cd(subDirectoryPath);
                    copyFolder(channelSftp, file.getAbsolutePath(), subDirectoryPath);
                    channelSftp.cd("..");
                }
            }
        }
    }

    public static boolean isServerAvailable(String host, int port, String username, String password, int maxRetries) {
        JSch jSch = new JSch();
        Session session = null;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                session = jSch.getSession(username, host, port);
                session.setPassword(password);
                session.setConfig("StrictHostKeyChecking", "no"); // Disable host key checking (optional)

                session.connect(5000); // Timeout in milliseconds

                return true; // Connection successful
            } catch (JSchException e) {
                // Connection failed, increment retry count
                retryCount++;
                System.out.println("Connection attempt failed. Retrying (" + retryCount + "/" + maxRetries + ")...");
                sleep(1000); // Wait for 1 second before retrying
            } finally {
                if (session != null) {
                    session.disconnect();
                }
            }
        }

        return false; // Max retries reached, connection failed
    }

    private static void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

