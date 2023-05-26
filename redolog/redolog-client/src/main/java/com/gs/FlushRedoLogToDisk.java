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
import java.util.logging.Level;
import java.util.logging.Logger;

public class FlushRedoLogToDisk {

    static Logger log = Logger.getLogger(FlushRedoLogToDisk.class.getName());

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
                    say(String.format("Please enter valid arguments."));
                    printUsage();
                    System.exit(-1);
                }
                i++;
            }
        } catch( Exception e ) {
            log.log(Level.SEVERE, "Error processing command line arguments.", e);
            printUsage();
            System.exit(-1);
        }
    }

    public static void printUsage() {
        StringBuffer sb = new StringBuffer();

        sb.append(String.format("This program flushes the redo log to disk.%n"));
        sb.append(String.format("The following arguments are accepted:%n"));
        sb.append(String.format("  --spaceName=<space name>%n"));
        sb.append(String.format("    The name of the space to connect to. This argument is required.%n"));
        sb.append(String.format("  --help%n"));
        sb.append(String.format("    Display this help message and exit.%n"));
        sb.append(String.format("%n"));
        sb.append(String.format("In addition, the following Java System Properties should be set:%n"));
        sb.append(String.format("  -Dcom.gs.jini_lus.groups=<lookup group>%n"));
        sb.append(String.format("  -Dcom.gs.jini_lus.locators=<lookup locators%n"));
        say(sb.toString());
    }

    private static void say(String s) {
        System.out.println(s);

        log.log(Level.INFO, s);
    }

    public void executeTask() throws Exception {
        GigaSpace gs = new GigaSpaceConfigurer(new SpaceProxyConfigurer(spaceName)).gigaSpace();
        AsyncFuture<Integer> execute = gs.execute(new FlushRedoLogTask(spaceName));
        
        Integer totalFlushedPackets = execute.get(60, TimeUnit.SECONDS);
        log.log(Level.INFO, "totalFlushedPackets = " + totalFlushedPackets);
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
            log.log(Level.WARNING, "Exception occurred while flushing redo log to disk", e);
            System.exit(-1);
        }
        System.exit(0);
    }
}
