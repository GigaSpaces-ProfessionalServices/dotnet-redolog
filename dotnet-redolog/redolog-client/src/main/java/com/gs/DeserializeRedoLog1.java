package com.gs;

import com.gigaspaces.client.mutators.SpaceEntryMutator;
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
import org.openspaces.core.transaction.manager.DistributedJiniTxManagerConfigurer;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class DeserializeRedoLog1 {

    static String spaceName = "redolog";
    static String containerName = "redolog_container1";
    Path directory = SystemLocations.singleton().work("redo-log").resolve(spaceName);
    Path codeMapFile = directory.resolve(containerName + "_code_map");

    BufferedWriter out;

    public DeserializeRedoLog1() throws Exception {
        PlatformTransactionManager ptm = new DistributedJiniTxManagerConfigurer().transactionManager();
    }

    public static void main(String[] args) throws Exception{
        String fileName = "myRedoLog";
        if (args != null && args.length ==3){
            spaceName = args[0];
            containerName= args[1];
            fileName = args[2];
        }



        DeserializeRedoLog1 deserializeRedoLog1 = new DeserializeRedoLog1();
        deserializeRedoLog1.out =  new BufferedWriter(new FileWriter(fileName));
        System.out.println("Verify home is correct should be set as vm arg:" + SystemLocations.singleton().home());
        System.out.println("Target redoLog file:" + new File(fileName).getAbsolutePath());


        deserializeRedoLog1.readCodeMap();
        deserializeRedoLog1.process();
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
                    //IOUtils.
                    System.out.println("===");
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void process() throws Exception {
        System.out.println("READ REDO-LOG FILE");
        Path path = SystemLocations.singleton().work("redo-log/" + spaceName);
        String dbName = "sqlite_storage_redo_log_" + containerName;
        System.out.println("Path:" +path + " dbName:"+ dbName);

        DBSwapRedoLogFileConfig<IReplicationOrderedPacket> config =
                new DBSwapRedoLogFileConfig<>(spaceName, containerName, 0);
        config.setKeepDatabaseFile(true);

        SqliteRedoLogFileStorage<IReplicationOrderedPacket> redoLogFile = new SqliteRedoLogFileStorage<>(config);
        StorageReadOnlyIterator<IReplicationOrderedPacket> readOnlyIterator = redoLogFile.readOnlyIterator(0);
        while (readOnlyIterator.hasNext()) {
            IReplicationOrderedPacket packet = readOnlyIterator.next();
            processPacket(packet.getData());
        }
        out.flush();
        out.close();

    }


    protected void processSingleEntryData(IReplicationPacketEntryData data) throws Exception{
        boolean writePacket = data instanceof WriteReplicationPacketData?true:false;
        boolean updatePacket = data instanceof UpdateReplicationPacketData?true:false;
        if (writePacket || updatePacket){
            IEntryData entryData = writePacket?getWriteData(data):getUpdateData(data);
            Object[] values1 = entryData.getFixedPropertiesValues();
            Map<String, Object> values2 = entryData.getDynamicProperties();
            Record record = new Record();
            record.opr = "write"; record.type = data.getTypeName(); record.uid = data.getUid();
            StringBuffer fixedBuffer = new StringBuffer(20);
            Arrays.stream(values1).iterator().forEachRemaining(value -> fixedBuffer.append(value + " "));
            record.fixedProps = fixedBuffer.toString();
            if (values2 != null){
                StringBuffer dynamicProps = new StringBuffer(40);
                values2.forEach((key,value) -> dynamicProps.append("key:" + key + " value:" + value +" "));
                record.dynamicProps=dynamicProps.toString();
            }

            appendRecord(record);
        }
        else if (data instanceof RemoveByUIDReplicationPacketData ){
            RemoveByUIDReplicationPacketData replicationPacketEntryData = (RemoveByUIDReplicationPacketData)data;
            Record record = new Record();
            record.opr = "remove"; record.uid=replicationPacketEntryData.getUid(); record.type=data.getTypeName();
            appendRecord(record);
        }
        else if (data instanceof RemoveReplicationPacketData ){
            RemoveReplicationPacketData replicationPacketEntryData = (RemoveReplicationPacketData)data;
            Record record = new Record();
            record.opr = "remove"; record.uid=replicationPacketEntryData.getUid(); record.type=data.getTypeName();
            appendRecord(record);
        }
        else if (data instanceof ChangeReplicationPacketData){
            ChangeReplicationPacketData changeReplicationPacketData = (ChangeReplicationPacketData)data;
            Collection<SpaceEntryMutator> mutators = changeReplicationPacketData.getCustomContent();
            Record record = new Record();
            record.type = data.getTypeName(); record.opr="change"; record.changes=mutators.toString();
            appendRecord(record);
        }
    }

    protected IEntryData getUpdateData(IReplicationPacketEntryData data){
        UpdateReplicationPacketData writeReplicationPacketData = (UpdateReplicationPacketData)data;
        return writeReplicationPacketData.getMainEntryData();
    }

    protected IEntryData getWriteData(IReplicationPacketEntryData data){
        WriteReplicationPacketData writeReplicationPacketData = (WriteReplicationPacketData)data;
        return writeReplicationPacketData.getMainEntryData();
    }

    protected void processTransactionData(IReplicationPacketData data) throws Exception {
        if (data instanceof TransactionOnePhaseReplicationPacketData){
            TransactionOnePhaseReplicationPacketData transactionalPacket = (TransactionOnePhaseReplicationPacketData)data;
            Iterator<IReplicationTransactionalPacketEntryData> iterator = transactionalPacket.iterator();
            while (iterator.hasNext()){
                IReplicationTransactionalPacketEntryData entryData = iterator.next();
                processSingleEntryData(entryData);
            }
        }
    }

    public void processPacket(IReplicationPacketData data) throws Exception {
        if (data.isSingleEntryData())
            processSingleEntryData((AbstractReplicationPacketSingleEntryData)data.getSingleEntryData());
        else {
            processTransactionData(data);
        }
    }

    public  void appendRecord(Record record) throws IOException{
        out.append(record.toStringBuffer().toString());
        out.newLine();
    }



    public static class Record {
        public static String FIELD_SEPARATOR ="#";
        String opr;
        String type;
        String fixedProps;
        String dynamicProps;
        String uid;
        String changes;
        public StringBuffer toStringBuffer(){
            StringBuffer stringBuffer = new StringBuffer(200);
            stringBuffer.append(opr);
            stringBuffer.append(FIELD_SEPARATOR);
            stringBuffer.append(type);
            stringBuffer.append(FIELD_SEPARATOR);
            stringBuffer.append(fixedProps);
            stringBuffer.append(FIELD_SEPARATOR);
            stringBuffer.append(dynamicProps);
            stringBuffer.append(FIELD_SEPARATOR);
            stringBuffer.append(uid);
            stringBuffer.append(FIELD_SEPARATOR);
            stringBuffer.append(changes);
            return stringBuffer;
        }
    }
}