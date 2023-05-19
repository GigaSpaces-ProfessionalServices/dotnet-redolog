package com.gs;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    static Logger log;
    public static void printUsage() {
        StringBuffer sb = new StringBuffer();

        sb.append(String.format("This program is used to call other programs related to restoring from redo logs.%n"));
        sb.append(String.format("The following options are accepted:%n"));
        sb.append(String.format("1. FlushRedoLogToDisk <followed by arguments to be passed to FlushRedoLogToDisk>%n"));
        sb.append(String.format(" Or %n"));
        sb.append(String.format("2. DeserializeRedoLog <followed by arguments to be passed to DeserializeRedoLog>%n"));
    }
    private static void say(String s) {
        System.out.println(s);

        log.log(Level.INFO, s);
    }

    public static void main(String[] args) {
        LoggerUtil.initialize();

        log = LoggerUtil.getLogger(Main.class.getName());

        try {
            if (args == null || args.length == 0) {
                printUsage();
                System.exit(-1);
            }
            if ("FlushRedoLogToDisk".equalsIgnoreCase(args[0]) ) {
                String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
                log.log(Level.INFO, "FlushRedoLogToDisk being called...");
                FlushRedoLogToDisk.main(newArgs);
            }
            else if ("DeserializeRedoLog".equalsIgnoreCase(args[0])) {
                String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
                log.log(Level.INFO, "DeserializeRedoLog being called...");
                DeserializeRedoLog.main(newArgs);
            } else {
                printUsage();
                System.exit(-1);
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error processing command line arguments", e);
            System.exit(-1);
        }
    }
}
