package com.gs;

import com.gigaspaces.annotation.SupportCodeChange;
import com.gigaspaces.async.AsyncResult;
import com.gigaspaces.internal.cluster.node.impl.packets.IReplicationOrderedPacket;
import com.gigaspaces.internal.cluster.node.impl.packets.data.IReplicationPacketData;
import com.gigaspaces.internal.cluster.node.impl.packets.data.IReplicationPacketEntryData;
import com.gigaspaces.internal.cluster.node.impl.packets.data.operations.AbstractReplicationPacketSingleEntryData;
import com.gigaspaces.internal.io.IOUtils;
import com.gigaspaces.internal.server.space.redolog.DBSwapRedoLogFileConfig;
import com.gigaspaces.internal.server.space.redolog.storage.SqliteRedoLogFileStorage;
import com.gigaspaces.internal.server.space.redolog.storage.StorageReadOnlyIterator;
import com.gigaspaces.start.SystemLocations;
import com.j_spaces.core.IJSpace;
import com.j_spaces.core.admin.IInternalRemoteJSpaceAdmin;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.executor.DistributedTask;
import org.openspaces.core.executor.TaskGigaSpace;
import org.openspaces.core.space.SpaceProxyConfigurer;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@SupportCodeChange(id="1")
public  class FlushRedoLogTask implements DistributedTask<Integer, Integer> {
    @TaskGigaSpace
    private transient GigaSpace injectedGigaSpace;

    private String spaceName;

    public FlushRedoLogTask(String spaceName) {
        this.spaceName = spaceName;
    }

    @Override
    public Integer reduce(List<AsyncResult<Integer>> results) throws Exception {
        int sum = 0;
        for (AsyncResult<Integer> result : results) {
            if (result.getException() != null) {
                throw result.getException();
            }
            sum += result.getResult();
        }
        return sum;
    }



    @Override
    public Integer execute() throws Exception {
        IJSpace iJSpace = null;

        if ( injectedGigaSpace != null) {
            iJSpace = injectedGigaSpace.getSpace();
        }
        else {
            SpaceProxyConfigurer configurer = new SpaceProxyConfigurer(spaceName);
            GigaSpace gigaSpace = new GigaSpaceConfigurer(configurer).gigaSpace();
            iJSpace = gigaSpace.getSpace();
        }

        IInternalRemoteJSpaceAdmin internalAdminProxy = (IInternalRemoteJSpaceAdmin) iJSpace.getAdmin();
      //  QuiesceTestUtils.doQuiesce(injectedGigaSpace.getSpace(), "QUIESCE BEFORE FLUSH OF REDO-LOG");
        int numberOfFlushedPackets = internalAdminProxy.flushRedoLogToStorage();
       // QuiesceTestUtils.undoQuiesce(injectedGigaSpace.getSpace());
        return numberOfFlushedPackets;
    }




}