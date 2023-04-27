package com.gs;

import com.gigaspaces.async.AsyncFuture;
import com.gigaspaces.internal.cluster.node.impl.packets.IReplicationOrderedPacket;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.SpaceProxyConfigurer;

import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FlushRedoLogToDisk {

    private String spaceName;

    public void processArgs(String[] args) {
        try {
            int i = 0;
            while (i < args.length) {
                String s = args[i];
                String sUpper = s.toUpperCase();

                if (sUpper.startsWith("--help".toUpperCase())) {
                    printUsage();
                    System.exit(0);
                }
                else if (sUpper.startsWith("--spaceName".toUpperCase())) {
                    String[] sArray = s.split("=", 2);
                    spaceName = sArray[1];
                }
                else {
                    System.out.println("Please enter valid arguments.");
                    printUsage();
                    System.exit(-1);
                }
                i++;
            }
        } catch( Exception e ) {
            e.printStackTrace();
            FlushRedoLogToDisk.printUsage();
            System.exit(-1);
        }
    }

    public static void printUsage() {
        System.out.println("This program flushes the redo log to disk.");
        System.out.println("The following arguments are accepted:");
        System.out.println("  --spaceName=<space name>");
        System.out.println("    The name of the space to connect to. This argument is required.");
        System.out.println("  --help");
        System.out.println("    Display this help message and exit.");
        System.out.println();
        System.out.println("In addition, the following Java System Properties should be set:");
        System.out.println("  -Dcom.gs.jini_lus.groups=<lookup group>");
        System.out.println("  -Dcom.gs.jini_lus.locators=<lookup locators");
    }

    public void executeTask() throws Exception {
        GigaSpace gs = new GigaSpaceConfigurer(new SpaceProxyConfigurer(spaceName)).gigaSpace();
        AsyncFuture<Integer> execute = gs.execute(new FlushRedoLogTask(spaceName));
        Integer totalFlushedPackets = execute.get(60, TimeUnit.SECONDS);
        System.out.println("totalFlushedPackets = " + totalFlushedPackets);
    }

    public static void main(String[] args) {
        try {
            FlushRedoLogToDisk flushRedoLogToDisk = new FlushRedoLogToDisk();
            flushRedoLogToDisk.processArgs(args);
            if( flushRedoLogToDisk.spaceName == null) {
                printUsage();
                System.exit(-1);
            }
            flushRedoLogToDisk.executeTask();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
