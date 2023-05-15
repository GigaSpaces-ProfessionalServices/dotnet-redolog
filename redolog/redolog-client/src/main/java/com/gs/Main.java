package com.gs;

import java.util.Arrays;

public class Main {

    public static void printUsage() {
        System.out.println("This program is used to call other programs related to restoring from redo logs.");
        System.out.println("The following options are accepted:");
        System.out.println("1. FlushRedoLogToDisk <followed by arguments to be passed to FlushRedoLogToDisk>");
        System.out.println(" Or ");
        System.out.println("2. DeserializeRedoLog <followed by arguments to be passed to DeserializeRedoLog>");
    }
    public static void main(String[] args) {
        try {
            if (args == null || args.length == 0) {
                printUsage();
                System.exit(-1);
            }
            if ("FlushRedoLogToDisk".equalsIgnoreCase(args[0]) ) {
                String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
                System.out.println("FlushRedoLogToDisk being called...");
                FlushRedoLogToDisk.main(newArgs);
            }
            else if ("DeserializeRedoLog".equalsIgnoreCase(args[0])) {
                String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
                System.out.println("DeserializeRedoLog being called...");
                DeserializeRedoLog.main(newArgs);
            } else {
                printUsage();
                System.exit(-1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
