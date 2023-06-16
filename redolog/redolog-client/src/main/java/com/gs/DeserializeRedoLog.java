package com.gs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
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
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeserializeRedoLog {

    static Logger log;

    String spaceName;
    String containerName;
    String outputFileName;
    Path directory;
    Path codeMapFile;

    private static final String redoLogSubpath = "redo-log";

    Map<String, String[]> fieldNamesMap = new HashMap<>();

    SequenceWriter sequenceWriter;

    //BufferedWriter out;

    public DeserializeRedoLog() throws Exception {
        PlatformTransactionManager ptm = new DistributedJiniTxManagerConfigurer().transactionManager();
    }


    /*
        This code is needed in order to deserialize packets internally
     */
    public void readCodeMap() {
        directory = SystemLocations.singleton().work(redoLogSubpath).resolve(spaceName);
        codeMapFile = directory.resolve(containerName + "_code_map");
        log.log(Level.INFO, "READ CLASS CODES FROM: " + codeMapFile.getFileName());

        String codeMapFilePath = codeMapFile.toFile().getAbsolutePath();

        log.log(Level.INFO, "The full path of the code map file is: " + codeMapFilePath);

        if (codeMapFile.toFile().exists()) {
            try (FileInputStream fis = new FileInputStream(codeMapFile.toFile())) {
                try (ObjectInputStream ois = new ObjectInputStream(fis)) {
                    IOUtils.readCodeMaps(ois);
                    //IOUtils.
                    log.log(Level.INFO, "===");
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            log.log(Level.WARNING, String.format("The code map file %s does not exist, skipping processing", codeMapFilePath));
        }
    }

    public void process() throws Exception {

        log.log(Level.INFO, "READ REDO-LOG FILE");
        Path path = SystemLocations.singleton().work("redo-log/" + spaceName);
        String dbName = "sqlite_storage_redo_log_" + containerName;
        log.log(Level.INFO, String.format("The full path to the sqlite redo log database file is: %s%s%s", path, File.separator, dbName));
        log.log(Level.INFO, "Beginning processing...");

        DBSwapRedoLogFileConfig<IReplicationOrderedPacket> config =
                new DBSwapRedoLogFileConfig<>(spaceName, containerName, 0);
        config.setKeepDatabaseFile(true);
        int count = 0;
        SqliteRedoLogFileStorage<IReplicationOrderedPacket> redoLogFile = new SqliteRedoLogFileStorage<>(config);
        StorageReadOnlyIterator<IReplicationOrderedPacket> readOnlyIterator = redoLogFile.readOnlyIterator(0);
        while (readOnlyIterator.hasNext()) {
            IReplicationOrderedPacket packet = readOnlyIterator.next();
            processPacket(packet.getData());
            count ++;
            if (count % 100 == 0 ) {
                log.log(Level.INFO, String.format("Processed %d records", count));
            }
        }
        log.log(Level.INFO, "End processing.");
        log.log(Level.INFO, String.format("Total number of records: %d", count));
        //out.flush();
        //out.close();

    }
    private String[] getFieldMap(String typeName) throws Exception {
        if(fieldNamesMap.containsKey(typeName)) {
            return fieldNamesMap.get(typeName);
        }

        Class clazz = Class.forName(typeName);

        // TODO: getDeclaredFields works for simplest POJO, need to test more complex class structures
        Field[] fields = clazz.getDeclaredFields();
        String[] fieldNames = new String[fields.length];

        for (int i=0; i < fields.length; i++) {

            fieldNames[i] = fields[i].getName();
        }
        Arrays.sort(fieldNames);
        fieldNamesMap.put(typeName, fieldNames);
        return fieldNames;
    }

    protected List<Object> createFixedProps(String typeName, Object[] fixedPropertiesValues) throws ClassNotFoundException, Exception {
/*
        String[] fields = getFieldMap(typeName);

        if( fields.length != fixedPropertiesValues.length) {
            String error = String.format("There is mismatch in the number of fields %d in the class %s and the number of values received %d",
                    fields.length, typeName, fixedPropertiesValues.length);
            throw new Exception(error);
        }

 */
        List<Object> list = new ArrayList<>();

        // assuming fields is sorted and in same order as values
        for(int i=0; i < fixedPropertiesValues.length; i++) {
            //Map.Entry<String, Object> entry = Map.entry(fields[i], fixedPropertiesValues[i]);

            list.add(fixedPropertiesValues[i]);
        }

        //Arrays.stream(fixedPropertiesValues).iterator().forEachRemaining(value -> map.append(value + " "));
        return list;
    }

    protected void processSingleEntryData(IReplicationPacketEntryData data) throws Exception {
        boolean writePacket = data instanceof WriteReplicationPacketData?true:false;
        boolean updatePacket = data instanceof UpdateReplicationPacketData?true:false;
        if (writePacket || updatePacket){
            IEntryData entryData = writePacket?getWriteData(data):getUpdateData(data);
            //ITypeDesc typeDescriptor = entryData.getSpaceTypeDescriptor();
            Object[] fixedPropertiesValues = entryData.getFixedPropertiesValues();
            Map<String, Object> dynamicPropertiesValues = entryData.getDynamicProperties();
            String typeName = data.getTypeName();
            Record record = new Record(Record.Operation.write, typeName, data.getUid());

            record.fixedProps = createFixedProps(typeName, fixedPropertiesValues);
            if (dynamicPropertiesValues != null){
                record.setDynamicProps(dynamicPropertiesValues);
            }

            appendRecord(record);
        }
        else if (data instanceof RemoveByUIDReplicationPacketData ){
            RemoveByUIDReplicationPacketData replicationPacketEntryData = (RemoveByUIDReplicationPacketData)data;
            Record record = new Record(Record.Operation.remove, data.getTypeName(), replicationPacketEntryData.getUid());
            appendRecord(record);
        }
        else if (data instanceof RemoveReplicationPacketData ){
            RemoveReplicationPacketData replicationPacketEntryData = (RemoveReplicationPacketData)data;
            Record record = new Record(Record.Operation.remove, data.getTypeName(), replicationPacketEntryData.getUid());
            appendRecord(record);
        }
        else if (data instanceof ChangeReplicationPacketData){
            ChangeReplicationPacketData changeReplicationPacketData = (ChangeReplicationPacketData)data;
            Collection<SpaceEntryMutator> mutators = changeReplicationPacketData.getCustomContent();
            Record record = new Record(Record.Operation.change, data.getTypeName(), changeReplicationPacketData.getUid());
            record.setChanges(mutators.toString());
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
        sequenceWriter.write(record);
        //out.append(record.toStringBuffer().toString());
        //out.newLine();


    }
    public void initializeMapper() throws IOException {
        File file = new File(outputFileName);
        if(file.exists()){
            file.delete();
        }

        FileWriter fileWriter = new FileWriter(file, true);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        sequenceWriter = mapper.writer().writeValuesAsArray(fileWriter);

    }
    public void close() throws Exception {
        sequenceWriter.close();
    }
/*
    public void writeYaml(String outputFileName) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        mapper.writeValue(new File(outputFileName), records);
    }
*/


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
                else if (sUpper.startsWith("--containerName".toUpperCase())) {
                    String[] sArray = s.split("=", 2);
                    containerName = sArray[1];
                }
                else if (sUpper.startsWith("--outputFileName".toUpperCase())) {
                    String[] sArray = s.split("=", 2);
                    outputFileName = sArray[1];
                }
                else {
                    say("Please enter valid arguments.");
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

    private void checkArgs() {
        log.log(Level.INFO, "Verify home is correct (should be set as vm arg -Dcom.gs.home): " + SystemLocations.singleton().home());

        File f = new File(outputFileName);
        log.log(Level.INFO, "Output filename (yaml) is: " + f.getAbsolutePath());
        if (f.exists() ) {
            log.log(Level.WARNING, "Output filename already exists. Output will be appended to this file.");
        }
    }
    public static void printUsage() {
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("This program reads the contents of the GigaSpaces redo log and writes it into readable format (yaml).%n"));
        sb.append(String.format("The following arguments are accepted:%n"));
        sb.append(String.format("  --spaceName=<space name>%n"));
        sb.append(String.format("    The name of the space to connect to. This argument is required.%n"));
        sb.append(String.format("  --containerName=<container name>%n"));
        sb.append(String.format("    The container name is the name of the partition. This argument is required.%n"));
        sb.append(String.format("  --outputFileName=</path/to/outputfile.yaml>%n"));
        sb.append(String.format("    The output filename is where the yaml is written. By default it is \"myredolog.yaml\".%n"));
        sb.append(String.format("  --help%n"));
        sb.append(String.format("    Display this help message and exit.%n"));
        sb.append(String.format("%n"));
        sb.append(String.format("In addition, the following Java System Properties should be set:%n"));
        sb.append(String.format("  -Dcom.gs.home=<the backup location where you have copied the work/redo-log directories%n"));
        sb.append(String.format("  -Dlog_properties_file=</path/to/overridden/logging.properties>. This is optional.%n"));
        say(sb.toString());
    }

    private static void say(String s) {
        System.out.println(s);

        log.log(Level.INFO, s);
    }

    public static void main(String[] args) {
        try {


            LoggerUtil.initialize();

            log = LoggerUtil.getLogger(DeserializeRedoLog.class.getName());

            log.log(Level.INFO, "Begin DeserializeRedoLog...");

            DeserializeRedoLog deserializeRedoLog = new DeserializeRedoLog();
            deserializeRedoLog.processArgs(args);

            //deserializeRedoLog.out =  new BufferedWriter(new FileWriter(fileName));
            deserializeRedoLog.checkArgs();


            deserializeRedoLog.readCodeMap();
            deserializeRedoLog.initializeMapper();
            deserializeRedoLog.process();
            deserializeRedoLog.close();

            log.log(Level.INFO, "DeserializeRedoLog done.");
            System.exit(0);
        }
        catch (Exception e) {
            log.log(Level.SEVERE, "Error in DeserializeRedoLog", e);
            System.exit(-1);
        }
    }
}
