package org.apache.cassandra.client;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.util.*;
import java.util.Map.Entry;

import org.apache.cassandra.db.RowPosition;
import org.apache.cassandra.dht.*;
import org.apache.cassandra.hadoop.ConfigHelper;
import org.apache.cassandra.thrift.*;
import org.apache.cassandra.thrift.Cassandra.AsyncClient.batch_mutate_call;
import org.apache.cassandra.thrift.Cassandra.AsyncClient.get_range_slices_by_time_call;
import org.apache.cassandra.thrift.Cassandra.AsyncClient.get_range_slices_call;
import org.apache.cassandra.thrift.Cassandra.AsyncClient.multiget_count_by_time_call;
import org.apache.cassandra.thrift.Cassandra.AsyncClient.multiget_count_call;
import org.apache.cassandra.thrift.Cassandra.AsyncClient.multiget_slice_by_time_call;
import org.apache.cassandra.thrift.Cassandra.AsyncClient.multiget_slice_call;
import org.apache.cassandra.thrift.Cassandra.AsyncClient.set_keyspace_call;
import org.apache.cassandra.thrift.Cassandra.AsyncClient.transactional_batch_mutate_cohort_call;
import org.apache.cassandra.thrift.Cassandra.AsyncClient.transactional_batch_mutate_coordinator_call;
import org.apache.cassandra.utils.*;
import org.apache.cassandra.utils.ColumnOrSuperColumnHelper.EvtAndLvt;
import org.apache.hadoop.conf.Configuration;
import org.apache.thrift.TException;
import org.apache.thrift.async.TAsyncClientManager;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.*;

/**
 * explicit client library
 *
 * @author dagnely
 *
 */
public class ExplicitClientLibrary extends ClientLibrary{


    //private final Logger logger = LoggerFactory.getLogger(ClientLibrary.class);

    public ExplicitClientLibrary(Map<String, Integer> localServerIPAndPorts, String keyspace, ConsistencyLevel consistencyLevel)
            throws Exception
    {
        // if (logger.isTraceEnabled()) {
        //     logger.trace("ClientLibrary(localServerIPAndPorts = {}, keyspace = {}, consistencyLevel = {})", new Object[]{localServerIPAndPorts, keyspace, consistencyLevel});
        //}

        super(localServerIPAndPorts, keyspace, consistencyLevel);
    }

    public void insert(ByteBuffer key, ColumnParent column_parent, Column column, HashSet<Dep> deps)
            throws InvalidRequestException, UnavailableException, TimedOutException, TException
    {
        //if (logger.isTraceEnabled()) {
        //    logger.trace("insert(key = {}, column_parent = {}, column = {})", new Object[]{printKey(key),column_parent, column});
        //}

        //Set the timestamp (version) to 0 so the accepting datacenter sets it
        column.timestamp = 0;
        WriteResult result = findClient(key).insert(key, column_parent, column, consistencyLevel, deps, LamportClock.sendTimestamp());
        LamportClock.updateTime(result.lts);
        clientContext.addDep(new Dep(key, result.version));
    }

    public void batch_mutate(Map<ByteBuffer,Map<String,List<Mutation>>> mutation_map, HashSet<Dep> deps)
            throws Exception
    {
        //if (logger.isTraceEnabled()) {
        //    logger.trace("batch_mutate(mutation_map = {})", new Object[]{mutation_map});
        //}

        //mutation_map: key -> columnFamily -> list<mutation>, mutation is a ColumnOrSuperColumn insert or a delete
        // 0 out all timestamps
        for (Map<String, List<Mutation>> cfToMutations : mutation_map.values()) {
            for (List<Mutation> mutations : cfToMutations.values()) {
                for (Mutation mutation : mutations) {
                    if (mutation.isSetColumn_or_supercolumn()) {
                        ColumnOrSuperColumnHelper.updateTimestamp(mutation.column_or_supercolumn, 0);
                    } else {
                        assert mutation.isSetDeletion();
                        mutation.deletion.timestamp = 0L;
                    }
                }
            }
        }

        //split it into a set of batch_mutations, one for each server in the cluster
        Map<Cassandra.AsyncClient, Map<ByteBuffer,Map<String,List<Mutation>>>> asyncClientToMutations = new HashMap<Cassandra.AsyncClient, Map<ByteBuffer,Map<String,List<Mutation>>>>();
        for (Entry<ByteBuffer, Map<String,List<Mutation>>> entry : mutation_map.entrySet()) {
            ByteBuffer key = entry.getKey();
            Map<String,List<Mutation>> mutations = entry.getValue();

            Cassandra.AsyncClient asyncClient = findAsyncClient(key);
            if (!asyncClientToMutations.containsKey(asyncClient)) {
                asyncClientToMutations.put(asyncClient, new HashMap<ByteBuffer,Map<String,List<Mutation>>>());
            }
            asyncClientToMutations.get(asyncClient).put(key, mutations);
        }

        //We need to split up based key because even if keys are colocated on the same server here,
        //we can't guarentee they'll be colocated on the same server in other datacenters
        Queue<BlockingQueueCallback<batch_mutate_call>> callbacks = new LinkedList<BlockingQueueCallback<batch_mutate_call>>();
        for (Entry<Cassandra.AsyncClient, Map<ByteBuffer,Map<String,List<Mutation>>>> entry : asyncClientToMutations.entrySet()) {
            Cassandra.AsyncClient asyncClient = entry.getKey();
            Map<ByteBuffer,Map<String,List<Mutation>>> mutations = entry.getValue();

            BlockingQueueCallback<batch_mutate_call> callback = new BlockingQueueCallback<batch_mutate_call>();
            callbacks.add(callback);
            asyncClient.batch_mutate(mutations, consistencyLevel, deps, LamportClock.sendTimestamp(), callback);
        }

        for (BlockingQueueCallback<batch_mutate_call> callback : callbacks) {
            BatchMutateResult result = callback.getResponseNoInterruption().getResult();
            LamportClock.updateTime(result.lts);
            clientContext.addDeps(result.deps);
        }
    }

    public void transactional_batch_mutate(Map<ByteBuffer,Map<String,List<Mutation>>> mutation_map, HashSet<Dep> deps)
            throws Exception
    {
        //if (logger.isTraceEnabled()) {
        //    logger.trace("batch_mutate(mutation_map = {})", new Object[]{mutation_map});
        //}

        //mutation_map: key -> columnFamily -> list<mutation>, mutation is a ColumnOrSuperColumn insert or a delete
        // 0 out all timestamps
        for (Map<String, List<Mutation>> cfToMutations : mutation_map.values()) {
            for (List<Mutation> mutations : cfToMutations.values()) {
                for (Mutation mutation : mutations) {
                    if (mutation.isSetColumn_or_supercolumn()) {
                        ColumnOrSuperColumnHelper.updateTimestamp(mutation.column_or_supercolumn, 0);
                    } else {
                        assert mutation.isSetDeletion();
                        mutation.deletion.timestamp = 0L;
                    }
                }
            }
        }

        //split it into a set of batch_mutations, one for each server in the cluster
        Map<Cassandra.AsyncClient, Map<ByteBuffer,Map<String,List<Mutation>>>> asyncClientToMutations = new HashMap<Cassandra.AsyncClient, Map<ByteBuffer,Map<String,List<Mutation>>>>();
        for (Entry<ByteBuffer, Map<String,List<Mutation>>> entry : mutation_map.entrySet()) {
            ByteBuffer key = entry.getKey();
            Map<String,List<Mutation>> mutations = entry.getValue();

            Cassandra.AsyncClient asyncClient = findAsyncClient(key);
            if (!asyncClientToMutations.containsKey(asyncClient)) {
                asyncClientToMutations.put(asyncClient, new HashMap<ByteBuffer,Map<String,List<Mutation>>>());
            }
            asyncClientToMutations.get(asyncClient).put(key, mutations);
        }

        //pick a random key from a random participant to be the coordinator
        ByteBuffer coordinatorKey = null;
        int coordinatorIndex = (int) (Math.random() * asyncClientToMutations.size());
        int asyncClientIndex = 0;
        for (Map<ByteBuffer,Map<String,List<Mutation>>> keyToMutations : asyncClientToMutations.values()) {
            if (asyncClientIndex == coordinatorIndex) {
                Set<ByteBuffer> keys = keyToMutations.keySet();
                int coordinatorKeyIndex = (int) (Math.random() * keys.size());
                int keyIndex = 0;
                for (ByteBuffer key : keys) {
                    if (keyIndex == coordinatorKeyIndex) {
                        coordinatorKey = key;
                        break;
                    }
                    keyIndex++;
                }
                break;
            }
            asyncClientIndex++;
        }
        assert coordinatorKey != null;

        //get a unique transactionId
        long transactionId = getTransactionId();

        //We need to split up based key because even if keys are colocated on the same server here,
        //we can't guarentee they'll be colocated on the same server in other datacenters
        BlockingQueueCallback<transactional_batch_mutate_coordinator_call> coordinatorCallback = null;
        Queue<BlockingQueueCallback<transactional_batch_mutate_cohort_call>> cohortCallbacks = new LinkedList<BlockingQueueCallback<transactional_batch_mutate_cohort_call>>();
        for (Entry<Cassandra.AsyncClient, Map<ByteBuffer,Map<String,List<Mutation>>>> entry : asyncClientToMutations.entrySet()) {
            Cassandra.AsyncClient asyncClient = entry.getKey();
            Map<ByteBuffer,Map<String,List<Mutation>>> mutations = entry.getValue();

            if (mutations.containsKey(coordinatorKey)) {
                coordinatorCallback = new BlockingQueueCallback<transactional_batch_mutate_coordinator_call>();
                Set<ByteBuffer> allKeys = mutation_map.keySet();
                asyncClient.transactional_batch_mutate_coordinator(mutations, consistencyLevel, deps, coordinatorKey, allKeys, transactionId, LamportClock.sendTimestamp(), coordinatorCallback);
            } else {
                BlockingQueueCallback<transactional_batch_mutate_cohort_call> callback = new BlockingQueueCallback<transactional_batch_mutate_cohort_call>();
                asyncClient.transactional_batch_mutate_cohort(mutations, coordinatorKey, transactionId, LamportClock.sendTimestamp(), callback);
                cohortCallbacks.add(callback);
            }
        }

        BatchMutateResult result = coordinatorCallback.getResponseNoInterruption().getResult();
        LamportClock.updateTime(result.lts);
        clientContext.addDeps(result.deps);

        // Also wait for cohorts so we can safely reuse these connections
        for (BlockingQueueCallback<transactional_batch_mutate_cohort_call> callback : cohortCallbacks) {
            short cohortResult = callback.getResponseNoInterruption().getResult();
            assert cohortResult == 0;
        }
    }

    /*
    private static AtomicInteger transactionHighBits = new AtomicInteger(0);
    private long getTransactionId()
    {
        //want transactionIds that distinguish ongoing transactions
        //top 16 bits, are an incrementing value from this node
        //next 32 bits, are this node's ip address
        //last 16 bits, are this node's port

        long top16 = transactionHighBits.incrementAndGet();
        long next32;
        if (DatabaseDescriptor.getBroadcastAddress() == null) {
          //Embedded Server
            next32 = 0;
        } else {
            next32 = ByteBuffer.wrap(DatabaseDescriptor.getBroadcastAddress().getAddress()).getInt();
        }
        long last16 = DatabaseDescriptor.getRpcPort();

        return (top16 << 48) + (next32 << 16) + last16;
    }
    */

    public void remove(ByteBuffer key, ColumnPath column_path, HashSet<Dep> deps)
            throws InvalidRequestException, UnavailableException, TimedOutException, TException
    {
        //if (logger.isTraceEnabled()) {
        //    logger.trace("remove(key = {}, column_path = {})", new Object[]{printKey(key),column_path});
        //}

        long timestamp = 0;
        WriteResult result = findClient(key).remove(key, column_path, timestamp, consistencyLevel, deps, LamportClock.sendTimestamp());
        LamportClock.updateTime(result.lts);
        clientContext.addDep(new Dep(key, result.version));
    }

    public void truncate(String cfname, HashSet<Dep> deps)
            throws InvalidRequestException, UnavailableException, TimedOutException, TException
    {
        //COPS2 WL TODO: Decide what to do with truncate
        //truncate is a strongly consistent operation in cassandra
        //option one is simply to modify to wait for all deps to be satisfied everywhere before truncating
        //option two is to just truncate now and let ops pending to that CF do nothing (other than count for dep_checks)
        // option two sounds better to me

        //TODO: Set the timestamp (version) to 0 so the accepting datacenter sets it
        long returnTime = getAnyClient().truncate(cfname, deps, LamportClock.sendTimestamp());
        LamportClock.updateTime(returnTime);
        clientContext.addDep(ClientContext.NOT_YET_SUPPORTED);
    }

    public void add(ByteBuffer key, ColumnParent column_parent, CounterColumn column, HashSet<Dep> deps)
            throws InvalidRequestException, UnavailableException, TimedOutException, TException
    {
        //if (logger.isTraceEnabled()) {
        //    logger.trace("add(key = {}, column_parent = {}, column = {})", new Object[]{printKey(key),column_parent, column});
        //}

        WriteResult result = findClient(key).add(key, column_parent, column, consistencyLevel, deps, LamportClock.sendTimestamp());
        LamportClock.updateTime(result.lts);
        clientContext.addDep(new Dep(key, result.version));
    }

    public void remove_counter(ByteBuffer key, ColumnPath path, HashSet<Dep> deps)
            throws InvalidRequestException, UnavailableException, TimedOutException, TException
    {
        remove_counter(key, path, false, deps);
    }

    public void remove_counter_safe(ByteBuffer key, ColumnPath path, HashSet<Dep> deps)
            throws InvalidRequestException, UnavailableException, TimedOutException, TException
    {
        remove_counter(key, path, true, deps);
    }

    private void remove_counter(ByteBuffer key, ColumnPath path, boolean safe, HashSet<Dep> deps)
            throws InvalidRequestException, UnavailableException, TimedOutException, TException
    {
        //if (logger.isTraceEnabled()) {
        //    logger.trace("remove_counter(key = {}, path = {})", new Object[]{printKey(key),path});
        //}

        //clients must wait until their remove counter has propagated everywhere before reissuing adds to it, so to be "safe" you delete with ALL consistency
        WriteResult result = findClient(key).remove_counter(key, path, safe ? ConsistencyLevel.ALL : consistencyLevel, deps, LamportClock.sendTimestamp());
        LamportClock.updateTime(result.lts);
        clientContext.addDep(new Dep(key, result.version));
    }
}
