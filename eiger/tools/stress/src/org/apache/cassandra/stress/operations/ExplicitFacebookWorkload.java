package org.apache.cassandra.stress.operations;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;

import org.apache.cassandra.client.ExplicitClientLibrary;
import org.apache.cassandra.db.ColumnFamilyType;
import org.apache.cassandra.stress.Session;
import org.apache.cassandra.stress.Stress;
import org.apache.cassandra.stress.util.Operation;
import org.apache.cassandra.thrift.*;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.ColumnOrSuperColumnHelper;
import org.apache.cassandra.utils.FBUtilities;

public class ExplicitFacebookWorkload extends Operation
{
    private static List<ByteBuffer> values;
    private static ArrayList<Integer> columnCountList;

    public ExplicitFacebookWorkload(Session session, int index)
    {
        super(session, index);
    }

    @Override
    public void run(Cassandra.Client client) throws IOException
    {
        throw new RuntimeException("Dynamic Workload must be run with COPS client library");
    }

    @Override
    public void run(ExplicitClientLibrary clientLibrary) throws IOException
    {
        //do all random tosses here
        double opTypeToss = Stress.randomizer.nextDouble();
        if (opTypeToss <= .002) {
            double transactionToss = Stress.randomizer.nextDouble();
            boolean transaction = (transactionToss <= session.getWrite_transaction_fraction());
            //FB workload:
            //write N columns
            //write 1 key at a time
            //no write trans (i.e., write_trans_frac should be 0)

            write(clientLibrary, 1, transaction);
        } else {
            //String[] tables={"Walls","Profiles","Pictures","Groups","Profiles","Conversations","Settings"};
            String[] tables={"Walls","Pictures","Groups","Conversations",};
            String[] superColumNames={"commentsOnWall","comments","commentsOnGroup","messages"};
            //String[] superColumNames={"commentsOnWall","albums","comments","commentsOnGroup","conversations","messages",null};
            int chosen=Stress.randomizer.nextInt(tables.length);
            read2(clientLibrary, 1,tables[chosen],superColumNames[chosen]);
        }
    }

    //This is a copy of MultiGetter.run with columnsPerKey and keysPerRead being used instead of the session parameters
    public void read(ExplicitClientLibrary clientLibrary, int keysPerRead) throws IOException
    {
        // We grab all columns for the key, they have been set there by the populator / writes
        // TODO ensure/make writes blow away all old columns
        SlicePredicate nColumnsPredicate = new SlicePredicate().setSlice_range(new SliceRange().setStart(ByteBufferUtil.EMPTY_BYTE_BUFFER)
                .setFinish(ByteBufferUtil.EMPTY_BYTE_BUFFER)
                .setReversed(false)
                .setCount(1024));

        String[] tables={"Comments","Albums","Pictures","Walls","Groups","Profiles","Conversations","Settings","Messages"};
        Map<ByteBuffer,List<ColumnOrSuperColumn>> results;

        int columnCount = 0;
        long bytesCount = 0;
        int table=Stress.randomizer.nextInt(tables.length);
        
        ColumnParent parent = new ColumnParent("Super1");

        List<ByteBuffer> keys = generateKeys(keysPerRead,tables[table]);
        long startNano = System.nanoTime();

        boolean success = false;
        String exceptionMessage = null;

        for (int t = 0; t < session.getRetryTimes(); t++)
        {
            if (success)
                break;

            try
            {
                columnCount = 0;
                bytesCount = 0;

                results = clientLibrary.transactional_multiget_slice(keys, parent, nColumnsPredicate);

                success = (results.size() == keysPerRead);
                if (!success)
                    exceptionMessage = "Wrong number of keys: " + results.size() + " instead of " + keysPerRead;

                //String allReads = "Read ";
                for (Entry<ByteBuffer, List<ColumnOrSuperColumn>> entry : results.entrySet())
                {
                    ByteBuffer key = entry.getKey();
                    List<ColumnOrSuperColumn> columns = entry.getValue();

                    columnCount += columns.size();
                    success = (columns.size() > 0);
                    if (!success) {
                        exceptionMessage = "No columns returned for " + ByteBufferUtil.string(key);
                        break;
                    }

                    int keyByteTotal = 0;
                    for (ColumnOrSuperColumn cosc : columns) {
                        keyByteTotal += ColumnOrSuperColumnHelper.findLength(cosc);
                    }
                    bytesCount += keyByteTotal;

                    //allReads += ByteBufferUtil.string(key) + " = " + columns.size() + " cols = " + keyByteTotal + "B, ";
                }
                //allReads = allReads.substring(0, allReads.length() - 2);
                //System.out.println(allReads);
            }
            catch (Exception e)
            {
                exceptionMessage = getExceptionMessage(e);
                success = false;
            }


            if (!success)
            {
                List<String> raw_keys = new ArrayList<String>();
                for (ByteBuffer key : keys) {
                    raw_keys.add(ByteBufferUtil.string(key));
                }
                error(String.format("Operation [%d] retried %d times - error on calling multiget_slice for keys %s %s%n",
                        index,
                        session.getRetryTimes(),
                        raw_keys,
                        (exceptionMessage == null) ? "" : "(" + exceptionMessage + ")"));
            }

            session.operations.getAndIncrement();
            session.keys.getAndAdd(keys.size());
            session.columnCount.getAndAdd(columnCount);
            session.bytes.getAndAdd(bytesCount);
            long latencyNano = System.nanoTime() - startNano;
            session.latency.getAndAdd(latencyNano/1000000);
            session.latencies.add(latencyNano/1000);
        }
    }

    public void read2(ExplicitClientLibrary clientLibrary, int keysPerRead, String tableName, String superColumnName) throws IOException
    {
        // We grab all columns for the key, they have been set there by the populator / writes
        // TODO ensure/make writes blow away all old columns
        SlicePredicate nColumnsPredicate = new SlicePredicate().setSlice_range(new SliceRange().setStart(ByteBufferUtil.EMPTY_BYTE_BUFFER)
                .setFinish(ByteBufferUtil.EMPTY_BYTE_BUFFER)
                .setReversed(false)
                .setCount(1024));

        Map<ByteBuffer,List<ColumnOrSuperColumn>> results;

        int columnCount = 0;
        long bytesCount = 0;

        ColumnParent super1 = new ColumnParent("Super1");
        ColumnParent standard1 = new ColumnParent("Standard1");

        List<ByteBuffer> keys = generateKeys(keysPerRead,tableName);
        long startNano = System.nanoTime();

        boolean success = false;
        String exceptionMessage = null;

        for (int t = 0; t < session.getRetryTimes(); t++)
        {
            if (success)
                break;

            try
            {
                columnCount = 0;
                bytesCount = 0;
                ArrayList<ByteBuffer> columnNames=new ArrayList<ByteBuffer>();
                if(superColumnName != null) {
                    columnNames.add(ByteBufferUtil.bytes(superColumnName));
                }
                SlicePredicate pred=new SlicePredicate().setColumn_names(columnNames);

                results = clientLibrary.transactional_multiget_slice(keys, super1, pred);
                if(superColumnName != null) {
                    for (ByteBuffer key : keys) {
                        ColumnOrSuperColumn resultsKeys = results.get(key).get(0);
                        List<Column> resultsIds = resultsKeys.getSuper_column().getColumns();
                        List<ByteBuffer> resultsStrings = new ArrayList<>(resultsIds.size());
                        for (Column id : resultsIds) {
                            resultsStrings.add(id.bufferForName());
                        }
                        Map<ByteBuffer,List<ColumnOrSuperColumn>> results2=clientLibrary.transactional_multiget_slice(resultsStrings, standard1, nColumnsPredicate);
                        for (Entry<ByteBuffer, List<ColumnOrSuperColumn>> entry : results2.entrySet())
                        {
                            ByteBuffer key2 = entry.getKey();
                            List<ColumnOrSuperColumn> columns2 = entry.getValue();

                            columnCount += columns2.size();

                            int keyByteTotal = 0;
                            for (ColumnOrSuperColumn cosc : columns2) {
                                keyByteTotal += ColumnOrSuperColumnHelper.findLength(cosc);
                            }
                            bytesCount += keyByteTotal;

                            //allReads += ByteBufferUtil.string(key) + " = " + columns.size() + " cols = " + keyByteTotal + "B, ";
                        }
                        session.operations.getAndIncrement();
                        session.keys.getAndAdd(resultsStrings.size());
                    }
                }

                success = (results.size() == keysPerRead);
                if (!success)
                    exceptionMessage = "Wrong number of keys: " + results.size() + " instead of " + keysPerRead;

                //String allReads = "Read ";
                for (Entry<ByteBuffer, List<ColumnOrSuperColumn>> entry : results.entrySet())
                {
                    ByteBuffer key = entry.getKey();
                    List<ColumnOrSuperColumn> columns = entry.getValue();

                    columnCount += columns.size();
                    success = (columns.size() > 0);
                    if (!success) {
                        exceptionMessage = "No columns returned for " + ByteBufferUtil.string(key);
                        break;
                    }

                    int keyByteTotal = 0;
                    for (ColumnOrSuperColumn cosc : columns) {
                        keyByteTotal += ColumnOrSuperColumnHelper.findLength(cosc);
                    }
                    bytesCount += keyByteTotal;

                    //allReads += ByteBufferUtil.string(key) + " = " + columns.size() + " cols = " + keyByteTotal + "B, ";
                }
                //allReads = allReads.substring(0, allReads.length() - 2);
                //System.out.println(allReads);
            }
            catch (Exception e)
            {
                exceptionMessage = getExceptionMessage(e);
                success = false;
            }


            if (!success)
            {
                List<String> raw_keys = new ArrayList<String>();
                for (ByteBuffer key : keys) {
                    raw_keys.add(ByteBufferUtil.string(key));
                }
                error(String.format("Operation [%d] retried %d times - error on calling multiget_slice for keys %s %s%n",
                        index,
                        session.getRetryTimes(),
                        raw_keys,
                        (exceptionMessage == null) ? "" : "(" + exceptionMessage + ")"));
            }

            session.operations.getAndIncrement();
            session.keys.getAndAdd(keys.size());
            session.columnCount.getAndAdd(columnCount);
            session.bytes.getAndAdd(bytesCount);
            long latencyNano = System.nanoTime() - startNano;
            session.latency.getAndAdd(latencyNano/1000000);
            session.latencies.add(latencyNano/1000);
        }
    }

    private List<ByteBuffer> generateKeys(int numKeys) throws IOException
    {
        List<ByteBuffer> keys = new ArrayList<ByteBuffer>();

        for (int i = 0; i < numKeys*10 && keys.size() < numKeys; i++)
        {
            // We don't want to repeat keys within a mutate or a slice
            // TODO make more efficient
            ByteBuffer newKey = ByteBuffer.wrap(generateKey());
            if (!keys.contains(newKey)) {
                keys.add(newKey);
            }
        }

        if (keys.size() != numKeys) {
            error("Could not generate enough unique keys, " + keys.size() + " instead of " + numKeys);
        }

        return keys;
    }

    private List<ByteBuffer> generateKeys(int numKeys,String table) throws IOException
    {
        List<ByteBuffer> keys = new ArrayList<ByteBuffer>();

        for (int i = 0; i < numKeys*10 && keys.size() < numKeys; i++)
        {
            // We don't want to repeat keys within a mutate or a slice
            // TODO make more efficient
            ByteBuffer newKey = ByteBuffer.wrap(generateKey(table));
            if (!keys.contains(newKey)) {
                keys.add(newKey);
            }
        }

        if (keys.size() != numKeys) {
            error("Could not generate enough unique keys, " + keys.size() + " instead of " + numKeys);
        }

        return keys;
    }

    private ByteBuffer getFBValue()
    {
        return values.get(Stress.randomizer.nextInt(values.size()));
    }

    private ArrayList<Integer> generateFBColumnCounts()
    {
        Random randomizer = new Random();
        randomizer.setSeed(0);

        ArrayList<Integer> columnCountList = new ArrayList<Integer>();
        for (int i = 0; i < session.getNumTotalKeys(); i++)
        {
            columnCountList.add(getFBColumnCount(randomizer));
        }

        return columnCountList;
    }

    protected int getFBColumnCount(ByteBuffer key) throws IOException
    {
        return columnCountList.get(Integer.parseInt(ByteBufferUtil.string(key)));
    }


    public void write(ExplicitClientLibrary clientLibrary, int keysPerWrite, boolean transaction) throws IOException
    {
        if (values == null)
            values = generateFBValues();
        if (columnCountList == null)
            columnCountList = generateFBColumnCounts();
        FacebookGenerator facebookGenerator=new FacebookGenerator(values,0);


        String[] operations={"postOnWall","comment","createAlbum","addPicture","addFriend","createGroup","addPersonToGroup","postOnGroup",
                "updateComment","updateProfile","removeComment","removePicture","removeFriend","removeAlbum","sendMessage","refuseFriend",
                "acceptFriend","updateSetting","updateAlbum","updateGroup"};




        long startNano = System.nanoTime();
        List<ByteBuffer> keys = generateKeys(keysPerWrite);
        int totalColumns = 0;
        int totalBytes = 0;
        for(int i=0;i<keysPerWrite;i++){
            int operation=Stress.randomizer.nextInt(operations.length);
            generateMutationsAndWrite(clientLibrary, facebookGenerator, operations[operation], transaction);
        }

        session.operations.getAndIncrement();
        session.keys.getAndAdd(keysPerWrite);
        session.columnCount.getAndAdd(keysPerWrite*session.getColumns_per_key_write());
        session.bytes.getAndAdd(keysPerWrite*session.getColumns_per_key_write()*session.getColumnSize());
        long latencyNano = System.nanoTime() - startNano;
        session.latency.getAndAdd(latencyNano/1000000);
        session.latencies.add(latencyNano/1000);
    }

    public void writeOne(ExplicitClientLibrary clientLibrary,ByteBuffer key, Map<ByteBuffer, Map<String, List<Mutation>>> records, HashSet<Dep> deps, boolean transaction) throws IOException {
        //System.out.println("Writing " + ByteBufferUtil.string(key) + " with " + numColumns + " columns and " + keyByteTotal + " bytes");


        long startNano = System.nanoTime();

        boolean success = false;
        String exceptionMessage = null;

        for (int t = 0; t < session.getRetryTimes(); t++)
        {
            if (success)
                break;

            try
            {
                if (transaction) {
                    if(deps==null) {
                        clientLibrary.transactional_batch_mutate(records);
                    }else{
                        clientLibrary.transactional_batch_mutate(records, deps);
                    }
                } else {
                    if(deps==null) {
                        clientLibrary.batch_mutate(records);
                    }
                    else {
                        clientLibrary.batch_mutate(records, deps);
                    }
                }
                success = true;
            }
            catch (Exception e)
            {
                exceptionMessage = getExceptionMessage(e);
                success = false;
            }
        }

        if (!success)
        {
            error(String.format("Operation [%d] retried %d times - error inserting keys %s %s%n",
                    index,
                    session.getRetryTimes(),
                    key,
                    (exceptionMessage == null) ? "" : "(" + exceptionMessage + ")"));
        }

    }

    public void generateMutationsAndWrite(ExplicitClientLibrary clientLibrary, FacebookGenerator facebookGenerator, String operation, boolean transation) throws IOException {
        System.out.println(operation);
        Map<ByteBuffer, Map<String, List<Mutation>>> records = new HashMap<ByteBuffer, Map<String, List<Mutation>>>();
        HashSet<Dep> deps=new HashSet<Dep>();
        if(operation.equals("postOnWall")){
            ByteBuffer key=ByteBuffer.wrap(generateKey("Comments"));
            records.put(key,facebookGenerator.generateComment());
            records.put(ByteBuffer.wrap(generateKey("Walls")),facebookGenerator.addTo(key,"commentsOnWall"));
            writeOne(clientLibrary, key, records, null, transation);
        }
        else if(operation.equals("comment")){
            ByteBuffer key=ByteBuffer.wrap(generateKey("Comments"));
            ByteBuffer key_super=ByteBuffer.wrap(generateKey("Comments"));
            records.put(key,facebookGenerator.generateComment());
            records.put(key_super,facebookGenerator.addTo(key,"relatedComments"));
            deps.add(new Dep(key_super,records.get(key_super).get("Super1").get(0).getColumn_or_supercolumn().getColumn().timestamp));
            writeOne(clientLibrary, key, records, deps, transation);
        }
        else if(operation.equals("createAlbum")){
            ByteBuffer key=ByteBuffer.wrap(generateKey("Albums"));
            records.put(key,facebookGenerator.generateAlbum());
            writeOne(clientLibrary, key, records, null, transation);
        }
        else if(operation.equals("addPicture")){
            ByteBuffer key=ByteBuffer.wrap(generateKey("Picture"));
            ByteBuffer key_super=ByteBuffer.wrap(generateKey("Albums"));
            records.put(key,facebookGenerator.generatePicture());
            records.put(key_super,facebookGenerator.addTo(key,"pictures"));
            deps.add(new Dep(key_super,records.get(key_super).get("Standard1").get(0).getColumn_or_supercolumn().getColumn().timestamp));
            writeOne(clientLibrary, key, records, deps, transation);
        }
        else if(operation.equals("addFriend")){
            ByteBuffer key=ByteBuffer.wrap(generateKey("Profiles"));
            records.put(key,facebookGenerator.generateProfile());
            deps.add(new Dep(key,records.get(key).get("Standard1").get(0).getColumn_or_supercolumn().getColumn().timestamp));
            writeOne(clientLibrary, key, records, deps, transation);
        }
        else if(operation.equals("CreateGroup")){
            ByteBuffer key=ByteBuffer.wrap(generateKey("Groups"));
            records.put(key,facebookGenerator.generateGroup());
            writeOne(clientLibrary, key, records, null, transation);
        }
        else if(operation.equals("addPersonToGroup")){
            ByteBuffer key=ByteBuffer.wrap(generateKey("Groups"));
            records.put(key,facebookGenerator.addTo(facebookGenerator.getFBValue(),"personsOnGroup"));
            deps.add(new Dep(key,records.get(key).get("Super1").get(0).getColumn_or_supercolumn().getColumn().timestamp));
            writeOne(clientLibrary, key, records, deps, transation);
        }
        else if(operation.equals("postOnGroup")){
            ByteBuffer key=ByteBuffer.wrap(generateKey("Comments"));
            ByteBuffer key_super=ByteBuffer.wrap(generateKey("Groups"));
            records.put(key,facebookGenerator.generateComment());
            records.put(key_super,facebookGenerator.addTo(key,"commentsOnGroup"));
            deps.add(new Dep(key_super,records.get(key_super).get("Super1").get(0).getColumn_or_supercolumn().getColumn().timestamp));
            writeOne(clientLibrary, key, records, deps, transation);
        }
/*        else if(operation.equals("updateComment")){
            ByteBuffer key=ByteBuffer.wrap(generateKey("Comments"));
            records.put(key,facebookGenerator.deleteComment());
            records.put(key,facebookGenerator.generateComment());
            deps.add(new Dep(key,records.get(key).get("Comments").get(0).getColumn_or_supercolumn().getColumn().timestamp));
            writeOne(clientLibrary, key, records, deps, transation);
        }
        else if(operation.equals("updateProfile")){
            ByteBuffer key=ByteBuffer.wrap(generateKey("Profiles"));
            records.put(key,facebookGenerator.deleteProfile());
            records.put(key,facebookGenerator.generateProfile());
            deps.add(new Dep(key,records.get(key).get("Profiles").get(0).getColumn_or_supercolumn().getColumn().timestamp));
            writeOne(clientLibrary, key, records, deps, transation);
        }
        else if(operation.equals("removeComment")){
            ByteBuffer key=ByteBuffer.wrap(generateKey("Comments"));
            ByteBuffer super_key=ByteBuffer.wrap(generateKey("Comments"));
            records.put(key,facebookGenerator.deleteComment());
            records.put(super_key,facebookGenerator.remove(key));
            //TODO find is comment on comment, wall, ...
            deps.add(new Dep(key,records.get(key).get("Comments").get(0).getColumn_or_supercolumn().getColumn().timestamp));
            deps.add(new Dep(super_key,records.get(super_key).get("Comments").get(0).getColumn_or_supercolumn().getColumn().timestamp));
            writeOne(clientLibrary, key, records, deps, transation);
        }
        else if(operation.equals("removePicture")){
            ByteBuffer key=ByteBuffer.wrap(generateKey("Pictures"));
            ByteBuffer super_key=ByteBuffer.wrap(generateKey("Albums"));
            records.put(key,facebookGenerator.deletePicture());
            records.put(super_key,facebookGenerator.remove(key));
            deps.add(new Dep(key,records.get(key).get("Pictures").get(0).getColumn_or_supercolumn().getColumn().timestamp));
            deps.add(new Dep(super_key,records.get(super_key).get("Albums").get(0).getColumn_or_supercolumn().getColumn().timestamp));
            writeOne(clientLibrary, key, records, deps, transation);
        }
        else if(operation.equals("removeFriend")){
            ByteBuffer key=ByteBuffer.wrap(generateKey("Profiles"));
            records.put(key,facebookGenerator.deleteProfile());
            records.put(key,facebookGenerator.generateProfile());
            deps.add(new Dep(key,records.get(key).get("Profiles").get(0).getColumn_or_supercolumn().getColumn().timestamp));
            writeOne(clientLibrary, key, records, deps, transation);
        }
        else if(operation.equals("removeAlbum")){
            ByteBuffer key=ByteBuffer.wrap(generateKey("Albums"));
            records.put(key,facebookGenerator.deleteAlbum());
            deps.add(new Dep(key,records.get(key).get("Albums").get(0).getColumn_or_supercolumn().getColumn().timestamp));
            writeOne(clientLibrary, key, records, deps, transation);
        }*/
        else if(operation.equals("sendMessage")){
            ByteBuffer key=ByteBuffer.wrap(generateKey("Messages"));
            ByteBuffer super_key=ByteBuffer.wrap(generateKey("Conversations"));
            records.put(key,facebookGenerator.generateMessage());
            records.put(super_key,facebookGenerator.addTo(key,"messages"));
            deps.add(new Dep(super_key,records.get(super_key).get("Super1").get(0).getColumn_or_supercolumn().getColumn().timestamp));
            writeOne(clientLibrary, key, records, deps, transation);
        }
 /*       else if(operation.equals("refuseFriend")){
            ByteBuffer key=ByteBuffer.wrap(generateKey("Profiles"));
            records.put(key,facebookGenerator.deleteProfile());
            records.put(key,facebookGenerator.generateProfile());
            deps.add(new Dep(key,records.get(key).get("Profiles").get(0).getColumn_or_supercolumn().getColumn().timestamp));
            writeOne(clientLibrary, key, records, deps, transation);
        }
        else if(operation.equals("acceptFriend")){
            ByteBuffer key=ByteBuffer.wrap(generateKey("Profiles"));
            records.put(key,facebookGenerator.deleteProfile());
            records.put(key,facebookGenerator.generateProfile());
            deps.add(new Dep(key,records.get(key).get("Profiles").get(0).getColumn_or_supercolumn().getColumn().timestamp));
            writeOne(clientLibrary, key, records, deps, transation);
        }
        else if(operation.equals("updateSetting")){
            ByteBuffer key=ByteBuffer.wrap(generateKey("Settings"));
            records.put(key,facebookGenerator.deleteSetting());
            records.put(key,facebookGenerator.generateSetting());
            deps.add(new Dep(key,records.get(key).get("Settings").get(0).getColumn_or_supercolumn().getColumn().timestamp));
            writeOne(clientLibrary, key, records, deps, transation);
        }
        else if(operation.equals("updateAlbum")){
            ByteBuffer key=ByteBuffer.wrap(generateKey("Albums"));
            records.put(key,facebookGenerator.deleteAlbum());
            records.put(key,facebookGenerator.generateAlbum());
            deps.add(new Dep(key,records.get(key).get("Albums").get(0).getColumn_or_supercolumn().getColumn().timestamp));
            writeOne(clientLibrary, key, records, deps, transation);
        }
        else if(operation.equals("updateGroup")){
            ByteBuffer key=ByteBuffer.wrap(generateKey("Groups"));
            records.put(key,facebookGenerator.deleteGroup());
            records.put(key,facebookGenerator.generateGroup());
            deps.add(new Dep(key,records.get(key).get("Groups").get(0).getColumn_or_supercolumn().getColumn().timestamp));
            writeOne(clientLibrary, key, records, deps, transation);
        }*/
    }
}