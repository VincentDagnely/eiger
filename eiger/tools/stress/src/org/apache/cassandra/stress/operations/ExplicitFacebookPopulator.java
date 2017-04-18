package org.apache.cassandra.stress.operations;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import org.apache.cassandra.client.ExplicitClientLibrary;
import org.apache.cassandra.stress.Session;
import org.apache.cassandra.stress.util.Operation;
import org.apache.cassandra.thrift.*;
import org.apache.cassandra.utils.ByteBufferUtil;

public class ExplicitFacebookPopulator extends Operation
{
    private static ArrayList<ByteBuffer> values;
    private static ArrayList<Integer> columnCountList;

    public ExplicitFacebookPopulator(Session session, int index)
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
        if (values == null)
            values = generateFBValues();
        if (columnCountList == null)
            columnCountList = generateFBColumnCounts();

        FacebookGenerator facebookGenerator=new FacebookGenerator(values,0);

        //System.out.println("Populating " + rawKey + " with " + columnCount + " columns" + " and " + totalBytes + " bytes");
        Map<ByteBuffer, Map<String, List<Mutation>>> records = new HashMap<ByteBuffer, Map<String, List<Mutation>>>();
        // format used for keys
        String format = "%0" + session.getTotalKeysLength() + "d";
        String rawKey = String.format(format, index);
        ByteBuffer key = ByteBufferUtil.bytes(rawKey);
        records.put(key, facebookGenerator.generateComment());

        format = "%0" + session.getTotalKeysLength() + "d";
        rawKey = String.format(format, index);
        key = ByteBufferUtil.bytes(rawKey);
        records.put(key, facebookGenerator.generateAlbum());

        format = "%0" + session.getTotalKeysLength() + "d";
        rawKey = String.format(format, index);
        key = ByteBufferUtil.bytes(rawKey);
        records.put(key, facebookGenerator.generateConversation());

        format = "%0" + session.getTotalKeysLength() + "d";
        rawKey = String.format(format, index);
        key = ByteBufferUtil.bytes(rawKey);
        records.put(key, facebookGenerator.generateGroup());

        format = "%0" + session.getTotalKeysLength() + "d";
        rawKey = String.format(format, index);
        key = ByteBufferUtil.bytes(rawKey);
        records.put(key, facebookGenerator.generateMessage());

        format = "%0" + session.getTotalKeysLength() + "d";
        rawKey = String.format(format, index);
        key = ByteBufferUtil.bytes(rawKey);
        records.put(key, facebookGenerator.generatePicture());

        format = "%0" + session.getTotalKeysLength() + "d";
        rawKey = String.format(format, index);
        key = ByteBufferUtil.bytes(rawKey);
        records.put(key, facebookGenerator.generateProfile());

        format = "%0" + session.getTotalKeysLength() + "d";
        rawKey = String.format(format, index);
        key = ByteBufferUtil.bytes(rawKey);
        records.put(key, facebookGenerator.generateSetting());

        format = "%0" + session.getTotalKeysLength() + "d";
        rawKey = String.format(format, index);
        key = ByteBufferUtil.bytes(rawKey);
        records.put(key, facebookGenerator.generateWall());

        long start = System.currentTimeMillis();

        boolean success = false;
        String exceptionMessage = null;

        for (int t = 0; t < session.getRetryTimes(); t++)
        {
            if (success)
                break;

            try
            {
                clientLibrary.batch_mutate(records, new HashSet<>());
                success = true;
            }
            catch (Exception e)
            {
                exceptionMessage = getExceptionMessage(e);
                if (t + 1 == session.getRetryTimes()) {
                    e.printStackTrace();
                }
                success = false;
            }
        }

        if (!success)
        {
            error(String.format("Operation [%d] retried %d times - error inserting keys %s %s%n",
                    index,
                    session.getRetryTimes(),
                    rawKey,
                    (exceptionMessage == null) ? "" : "(" + exceptionMessage + ")"));
        }

        session.operations.getAndIncrement();
        session.keys.getAndAdd(1);
        session.columnCount.getAndAdd(1*22);
        session.bytes.getAndAdd(facebookGenerator.getTotalBytes());
        session.latency.getAndAdd(System.currentTimeMillis() - start);
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



}