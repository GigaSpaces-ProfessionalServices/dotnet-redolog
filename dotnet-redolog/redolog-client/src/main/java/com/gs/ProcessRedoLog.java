package com.gs;

import com.gigaspaces.client.mutators.SpaceEntryMutator;
import com.gigaspaces.internal.cluster.node.impl.DataTypeIntroducePacketData;
import com.gigaspaces.internal.cluster.node.impl.packets.IReplicationOrderedPacket;
import com.gigaspaces.internal.cluster.node.impl.packets.data.IReplicationPacketData;
import com.gigaspaces.internal.cluster.node.impl.packets.data.IReplicationPacketEntryData;
import com.gigaspaces.internal.cluster.node.impl.packets.data.IReplicationTransactionalPacketEntryData;
import com.gigaspaces.internal.cluster.node.impl.packets.data.operations.*;
import com.gigaspaces.internal.io.IOUtils;
import com.gigaspaces.internal.server.space.redolog.DBSwapRedoLogFileConfig;
import com.gigaspaces.internal.server.space.redolog.storage.SqliteRedoLogFileStorage;
import com.gigaspaces.internal.server.space.redolog.storage.StorageReadOnlyIterator;
import com.gigaspaces.internal.server.storage.IEntryData;
import com.gigaspaces.start.SystemLocations;
import net.jini.core.entry.UnusableEntryException;
import net.jini.core.transaction.TransactionException;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.SpaceProxyConfigurer;
import org.openspaces.core.transaction.manager.DistributedJiniTxManagerConfigurer;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public class ProcessRedoLog {

    static String spaceName = "redolog";
    static String containerName = "redolog_container1";
    Path directory = SystemLocations.singleton().work("redo-log").resolve(spaceName);
    Path codeMapFile = directory.resolve(containerName + "_code_map");

    GigaSpace targetSpace;

    SpaceReplay spaceReplay;

    public ProcessRedoLog() throws Exception {
        PlatformTransactionManager ptm = new DistributedJiniTxManagerConfigurer().transactionManager();
        targetSpace = new GigaSpaceConfigurer(new SpaceProxyConfigurer(spaceName)).transactionManager(ptm).gigaSpace();
        spaceReplay = new SpaceReplay(targetSpace);
    }

    public static void main(String[] args) throws Exception{
        if (args != null && args.length ==2){
            spaceName = args[0];
            containerName= args[1];
        }

       ProcessRedoLog processRedoLog = new ProcessRedoLog();
       System.out.println("verify home is correct should be set as vm arg:" + SystemLocations.singleton().home());

       processRedoLog.readCodeMap();
       LinkedList<IReplicationOrderedPacket> replicationOrderedPackets = processRedoLog.process();
    }

    /*
        This code is needed in order to deserialize packets internally
     */
    public void readCodeMap(){
        System.out.println("READ CLASS CODES FROM: " + codeMapFile.getFileName());

        if (codeMapFile.toFile().exists()) {
            try (FileInputStream fis = new FileInputStream(codeMapFile.toFile())) {
                try (ObjectInputStream ois = new ObjectInputStream(fis)) {
                    IOUtils.readCodeMaps(ois);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public LinkedList<IReplicationOrderedPacket> process() throws FileNotFoundException, UnusableEntryException, TransactionException, RemoteException, InterruptedException {
        System.out.println("READ REDO-LOG FILE");
        Path path = SystemLocations.singleton().work("redo-log/" + spaceName);
        String dbName = "sqlite_storage_redo_log_" + containerName;
        System.out.println("Path:" +path + " dbName:"+ dbName);

        DBSwapRedoLogFileConfig<IReplicationOrderedPacket> config =
                new DBSwapRedoLogFileConfig<>(spaceName, containerName, 0);
        config.setKeepDatabaseFile(true);

        SqliteRedoLogFileStorage<IReplicationOrderedPacket> redoLogFile = new SqliteRedoLogFileStorage<>(config);
        StorageReadOnlyIterator<IReplicationOrderedPacket> readOnlyIterator = redoLogFile.readOnlyIterator(0);
        LinkedList<IReplicationOrderedPacket> redologList = new LinkedList<>();
        while (readOnlyIterator.hasNext()) {
            IReplicationOrderedPacket packet = readOnlyIterator.next();
            processPacket(packet.getData());
            redologList.add(packet);
        }

        return redologList;
    }


    protected void processSingleEntryData(IReplicationPacketEntryData data) throws UnusableEntryException, TransactionException, RemoteException, InterruptedException {
        if (data instanceof DataTypeIntroducePacketData){
            System.out.println("Data type introduction  of type: " + data.getTypeName() );
        }
        if (data instanceof WriteReplicationPacketData){
            WriteReplicationPacketData writeReplicationPacketData = (WriteReplicationPacketData)data;
            IEntryData entryData = writeReplicationPacketData.getMainEntryData();
            spaceReplay.write(data.getTypeName(), entryData, data.getOperationId(), data.getUid());
            Object[] values = entryData.getFixedPropertiesValues();
            System.out.println("Write operation of type: " + data.getTypeName() +" properties:" );
            Arrays.stream(values).iterator().forEachRemaining(value -> System.out.printf(value + " "));
            System.out.println();
        }
        else if (data instanceof RemoveByUIDReplicationPacketData){
            RemoveByUIDReplicationPacketData replicationPacketEntryData = (RemoveByUIDReplicationPacketData)data;
            spaceReplay.remove(data.getTypeName(),replicationPacketEntryData.getUid() );
            System.out.println("Remove by uid operation of type: " + data.getTypeName() +" key:" + replicationPacketEntryData.getUid());
        }
        else if (data instanceof RemoveReplicationPacketData){
            RemoveReplicationPacketData replicationPacketEntryData = (RemoveReplicationPacketData)data;
            spaceReplay.remove(data.getTypeName(),replicationPacketEntryData.getUid() );
            System.out.println("Remove operation of type: " + data.getTypeName() +" key:" + replicationPacketEntryData.getUid());
        }
        else if (data instanceof UpdateReplicationPacketData){
            UpdateReplicationPacketData updateReplicationPacketData = (UpdateReplicationPacketData)data;
            IEntryData entryData = updateReplicationPacketData.getMainEntryData();
            spaceReplay.write(data.getTypeName(), entryData, data.getOperationId(), data.getUid());
            Object[] values = entryData.getFixedPropertiesValues();
            System.out.println("Update operation of type: " + data.getTypeName() +" properties:" );
            Arrays.stream(values).iterator().forEachRemaining(value -> System.out.printf(value + " "));
            System.out.println();
        }
        else if (data instanceof ChangeReplicationPacketData){
            ChangeReplicationPacketData changeReplicationPacketData = (ChangeReplicationPacketData)data;
            Collection<SpaceEntryMutator> mutators = changeReplicationPacketData.getCustomContent();
            spaceReplay.change(data.getTypeName(), changeReplicationPacketData.getUid(), mutators);
            System.out.println("Change operation of type: " + data.getTypeName() + " uid:" + changeReplicationPacketData.getUid() +" mutators:" + mutators);
        }
    }

    protected void processTransactionData(IReplicationPacketData data) throws UnusableEntryException, TransactionException, RemoteException, InterruptedException {
        if (data instanceof TransactionOnePhaseReplicationPacketData){
            System.out.println("START TRANSACTION");
            TransactionOnePhaseReplicationPacketData transactionalPacket = (TransactionOnePhaseReplicationPacketData)data;
            Iterator<IReplicationTransactionalPacketEntryData> iterator = transactionalPacket.iterator();
            while (iterator.hasNext()){
                IReplicationTransactionalPacketEntryData entryData = iterator.next();
                processSingleEntryData(entryData);
            }
            System.out.println("END TRANSACTION");
        }
    }

    public void processPacket(IReplicationPacketData data) throws UnusableEntryException, TransactionException, RemoteException, InterruptedException {
        if (data.isSingleEntryData())
            processSingleEntryData((AbstractReplicationPacketSingleEntryData)data.getSingleEntryData());
        else {
           processTransactionData(data);
        }
    }
}
