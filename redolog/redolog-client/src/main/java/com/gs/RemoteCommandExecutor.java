package com.gs;

import com.gs.dto.FileInformation;
import com.jcraft.jsch.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoteCommandExecutor {
    String gsHome;
    String scriptLocation;
    String spaceName;
    String targetDir;
    String targetPathBaseDir;
    String command;
    String fileNamePrefix = "deserializeRedolog_";
    String deserializeFullPath;
    String localFilePath = deserializeFullPath + "AllDeserializedFiles\\";
    final static String sqliteQuery = "select count(*) from REDO_LOGS;";
    String queryTemplatecmd = "\"" + scriptLocation + "\\sqlite3\" \"" + targetDir;
    String hostFileName = "spaceHosts.txt";

    public static void main(String[] args) {
        RemoteCommandExecutor remoteCommandExecutor = new RemoteCommandExecutor();
        if(args.length>0){
            remoteCommandExecutor.processArgs(args);
            if ("RemoteDeserializeRedoLog".equalsIgnoreCase(args[0])) {
                remoteCommandExecutor.deserializeFiles();
            } else if ("RemoteDownloadfiles".equalsIgnoreCase(args[0])) {
                remoteCommandExecutor.downloadRemoteFile();
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
                } else if (sUpper.startsWith("--gsHome".toUpperCase())) {
                    String[] sArray = s.split("=", 2);
                    gsHome = sArray[1];
                } else if (sUpper.startsWith("--scriptLocation".toUpperCase())) {
                    String[] sArray = s.split("=", 2);
                    scriptLocation = sArray[1];
                    queryTemplatecmd = "\"" + scriptLocation + "\\sqlite3\" \"" + targetDir;
                } else if (sUpper.startsWith("--targetDir".toUpperCase())) {
                    String[] sArray = s.split("=", 2);
                    targetDir = sArray[1];
                    command = "dir \"" + targetDir.replace("\\\\","\\") + "\" /b";
                    queryTemplatecmd = "\"" + scriptLocation + "\\sqlite3\" \"" + targetDir;
                } else if (sUpper.startsWith("--targetPathBaseDir".toUpperCase())) {
                    String[] sArray = s.split("=", 2);
                    targetPathBaseDir = sArray[1];
                } else if (sUpper.startsWith("--deserializeFullPath".toUpperCase())) {
                    String[] sArray = s.split("=", 2);
                    deserializeFullPath = sArray[1];
                    localFilePath = deserializeFullPath + "AllDeserializedFiles\\";
                } else if (sUpper.startsWith("--spaceName".toUpperCase())) {
                    String[] sArray = s.split("=", 2);
                    spaceName = sArray[1];
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

    static List<String> executeremoteCommandAndGetOutput(String host, String user, String password, String command) throws JSchException, IOException {
        List<String> outputList = new ArrayList<>();
        JSch jsch = new JSch();
        Session session = jsch.getSession(user, host, 22);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        try {
            channel.setCommand(command);
            channel.setInputStream(null);
            BufferedReader in = new BufferedReader(new InputStreamReader(channel.getInputStream()));

            channel.connect();
            String line;
            while ((line = in.readLine()) != null) {
                if (!line.endsWith("-shm") && !line.endsWith("-wal")) {
                    System.out.println(line);
                    outputList.add(line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }

        channel.disconnect();
        session.disconnect();
        return outputList;
    }

    void deserializeFiles() {
 /*       String gsHome = "C:\\GigaSpaces\\smart-cache.net-16.2.1-x64\\NET v4.0\\";
        String scriptLocation = gsHome + "automation-redo-log\\";
        String spaceName = "dataExampleSpace";
        String targetDir = gsHome + "backup\\work\\redo-log\\" + spaceName;

        // String command = "dir C:\\";
        String command = "dir \"" + targetDir + "\" /b";
        String sqliteQuery = "select count(*) from REDO_LOGS;";
        String queryTemplatecmd = "\"" + scriptLocation + "\\sqlite3\" \"" + targetDir;
        String hostFileName = "spaceHosts.txt";*/
        JSONParser parser = new JSONParser();
        JSONArray configArray;
        List<String> fileNames = new ArrayList<>();
        try {
            configArray = (JSONArray) parser.parse(new FileReader(scriptLocation + "\\" + hostFileName));
        } catch (Exception e) {
            System.out.println("Failed to read configuration file: " + e.getMessage());
            return;
        }
        Map<String, FileInformation> containerFileInformation = new HashMap<>();
        Map<String, List<String>> hostcontainerFileMap = new HashMap<>();
        for (Object jsonObject : configArray) {
            JSONObject hostDetail = (JSONObject) jsonObject;
            String host = (String) hostDetail.get("host");
            String user = (String) hostDetail.get("username");
            String password = (String) hostDetail.get("password");

            try {
               /* String ppp = "C:\\GigaSpaces\\smart-cache.net-16.2.1-x64\\NET v4.0\\backup\\work\\redo-log\\dataExampleSpace";
                String cmd1 = "dir \"" + ppp + "\" /b";
                executeremoteCommandAndGetOutput(host, user, password, (cmd1));
                executeremoteCommandAndGetOutput(host, user, password, (cmd1));*/
                List<String> fileNamesTmp = executeremoteCommandAndGetOutput(host, user, password, command);
                for (String fileName : fileNamesTmp) {
                    fileNames.add(fileName);
                    if (!hostcontainerFileMap.containsKey(host)){
                        hostcontainerFileMap.put(host,new ArrayList<>());
                    }
                    hostcontainerFileMap.get(host).add(fileName);
                    if(fileName.endsWith("_map")){
                        continue;
                    }
                    String queryFormed = "";
                    queryFormed = queryFormed + queryTemplatecmd + "\\" + fileName + "\" \"" + sqliteQuery + "\"";
                    List<String> fileNamesQryCountTmp = executeremoteCommandAndGetOutput(host, user, password, queryFormed);
                    long rowcount = 0;
                    if (!fileNamesQryCountTmp.isEmpty()) {
                        rowcount = Long.parseLong(fileNamesQryCountTmp.get(0));
                    }
                    containerFileInformation.put(fileName, new FileInformation(host, user, password, fileName, rowcount));
                }

                // System.out.println(fileNamesQryCount);
            } catch (JSchException | IOException e) {
                e.printStackTrace();
            }
        }
        for (String filename : fileNames) {
            if (!filename.endsWith("_1") && !filename.endsWith("_map")) {
                FileInformation file1 = containerFileInformation.get(filename);
                FileInformation file2 = containerFileInformation.get(filename + "_1");
                FileInformation fileInformation = file1;
                retryFlush(spaceName);
                if (!hostcontainerFileMap.get(file1.getHostname()).contains(filename.substring(filename.indexOf(spaceName))+"_code_map")) {
                    fileInformation = file2;
                }
                if (file2 != null && file2.getSize() > file1.getSize() && hostcontainerFileMap.get(file2.getHostname()).contains(filename.substring(filename.indexOf(spaceName))+"_code_map") ) {
                    fileInformation = file2;
                }
                /*
                * dataExampleSpace
                  dataExampleSpace_container1_1
                  "C:\GigaSpaces\smart-cache.net-16.2.1-x64\NET v4.0\backup\deserializeRedolog"
                * */
                String containerfileNamePartial = filename.substring(filename.indexOf(spaceName + "_container"));
                String deserializeFullPath = this.deserializeFullPath +"\\"+fileNamePrefix+ containerfileNamePartial + ".txt";
                //String deserializeCommand = "java -Dcom.gs.home=\"C:\\GigaSpaces\\smart-cache.net-16.2.1-x64\\NET v4.0\\backup\" -jar \"" + scriptLocation + "redolog-client-1.0-SNAPSHOT-jar-with-dependencies.jar\" DeserializeRedoLog " + spaceName + " " + containerfileNamePartial + " \"" + deserializeFullPath + "\"";

                String deserializeCommand = "java -Dcom.gs.home=\"" + targetPathBaseDir + "\" -jar \"" + scriptLocation + "redolog-client-1.0-SNAPSHOT-jar-with-dependencies.jar\" DeserializeRedoLog --spaceName=" + spaceName + " --containerName=" + containerfileNamePartial + " --outputFileName=\"" + deserializeFullPath + "\"";
                System.out.println(fileInformation.getHostname() + " => " + deserializeCommand);
                try {
                    executeremoteCommandAndGetOutput(fileInformation.getHostname(), fileInformation.getUsername(), fileInformation.getPassword(), deserializeCommand);
                } catch (JSchException | IOException e) {
                    e.printStackTrace();
                    System.out.println(e);
                }
            }

        }
        //System.out.println(fileNames);
        // System.out.println(containerFileInformation);
    }

    private void retryFlush(String spaceName) {

    }

    void downloadRemoteFile() {

/*        String gsHome = "C:\\GigaSpaces\\smart-cache.net-16.2.1-x64\\NET v4.0\\";
        String scriptLocation = gsHome + "automation-redo-log\\";
        String deserializeFullPath = "C:\\GigaSpaces\\smart-cache.net-16.2.1-x64\\NET v4.0\\backup\\";
        String fileNamePrefix = "deserializeRedolog_";
        String localFilePath = deserializeFullPath + "AllDeserializedFiles\\";
        String hostFileName = "spaceHosts.txt";*/
        JSONParser parser = new JSONParser();
        JSONArray configArray;
        List<String> fileNames = new ArrayList<>();
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
            //  downloadRemoteFile(host, user, password, "\""+deserializeFullPath+"\"", "\""+localFilePath+"\"",fileNamePrefix);
            downloadRemoteFile(host, user, password, deserializeFullPath, localFilePath, fileNamePrefix);
        }
    }

    static void downloadRemoteFile(String host, String user, String password, String remotePath, String localPath, String fileNamePrefix) {
        try {
            String command = "dir \"" + remotePath.replace("\\\\","\\") + "\" /b";
            List<String> files = executeremoteCommandAndGetOutput(host, user, password, command);
            JSch jsch = new JSch();
            Session session = jsch.getSession(user, host, 22);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            for (String file : files) {
                if (file.startsWith(fileNamePrefix) && file.endsWith(".txt"))
                    channelSftp.get("/" + remotePath.replace("\\", "/") + file, localPath.replace("\\\\","\\"));
            }
            channelSftp.disconnect();
            session.disconnect();
        } catch (JSchException | IOException | SftpException e) {
            e.printStackTrace();
        }
    }
}

