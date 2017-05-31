package org.apache.cassandra.stress.operations;

import org.apache.cassandra.stress.Stress;
import org.apache.cassandra.thrift.*;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.FBUtilities;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vincent on 06.04.17.
 */
public class FacebookGenerator {

    private List<ByteBuffer> values;
    private int totalBytes;

    public FacebookGenerator(List<ByteBuffer> values,int totalBytes){
        this.values=values;
        this.totalBytes=totalBytes;
    }

    public int getTotalBytes() { return totalBytes; }

    public ByteBuffer getFBValue()
    {
        return values.get(Stress.randomizer.nextInt(values.size()));
    }

    public Map<String, List<Mutation>> generateComment() throws IOException {
        List<Column> simpleColumns = new ArrayList<Column>();

        String[] simpleColumnNames={"comment","picture","video"};
        String[] superColumnNames={"relatedComments","personsWhoLiked","hashtags","persons@"};
        for (String c : simpleColumnNames)
        {
            ByteBuffer value = getFBValue();
            totalBytes += value.limit() - value.position();
            simpleColumns.add(new Column(ByteBufferUtil.bytes(c))
                    .setValue(value)
                    .setTimestamp(FBUtilities.timestampMicros()));
        }
        List<SuperColumn> superColumns = new ArrayList<SuperColumn>();
        for (String c : superColumnNames)
        {
            ByteBuffer value = getFBValue();
            totalBytes += value.limit() - value.position();
            ArrayList<Column> a=new ArrayList<Column>();
            a.add(new Column(ByteBufferUtil.bytes(c))
                    .setValue(value)
                    .setTimestamp(FBUtilities.timestampMicros()));
            superColumns.add(new SuperColumn(ByteBufferUtil.bytes(c),a));
        }

        List<Mutation> simpleMutations = new ArrayList<Mutation>();
        List<Mutation> superMutations = new ArrayList<Mutation>();
        Map<String, List<Mutation>> mutationMap = new HashMap<String, List<Mutation>>();

        for (Column c : simpleColumns)
        {
            ColumnOrSuperColumn column = new ColumnOrSuperColumn().setColumn(c);
            simpleMutations.add(new Mutation().setColumn_or_supercolumn(column));
        }
        for (SuperColumn c : superColumns)
        {
            ColumnOrSuperColumn column = new ColumnOrSuperColumn().setSuper_column(c);
            superMutations.add(new Mutation().setColumn_or_supercolumn(column));
        }

        mutationMap.put("Standard1", simpleMutations);
        mutationMap.put("Super1", superMutations);

        return mutationMap;
    }

    public Map<String, List<Mutation>> generateAlbum() throws IOException{
        List<Column> simpleColumns = new ArrayList<Column>();

        String[] simpleColumnNames={"name"};
        String[] superColumnNames={"pictures"};
        for (String c : simpleColumnNames)
        {
            ByteBuffer value = getFBValue();
            totalBytes += value.limit() - value.position();
            simpleColumns.add(new Column(ByteBufferUtil.bytes(c))
                    .setValue(value)
                    .setTimestamp(FBUtilities.timestampMicros()));
        }
        List<SuperColumn> superColumns = new ArrayList<SuperColumn>();
        for (String c : superColumnNames)
        {
            ByteBuffer value = getFBValue();
            totalBytes += value.limit() - value.position();
            ArrayList<Column> a=new ArrayList<Column>();
            a.add(new Column(ByteBufferUtil.bytes(c))
                    .setValue(value)
                    .setTimestamp(FBUtilities.timestampMicros()));
            superColumns.add(new SuperColumn(ByteBufferUtil.bytes(c),a));
        }

        List<Mutation> simpleMutations = new ArrayList<Mutation>();
        List<Mutation> superMutations = new ArrayList<Mutation>();
        Map<String, List<Mutation>> mutationMap = new HashMap<String, List<Mutation>>();

        for (Column c : simpleColumns)
        {
            ColumnOrSuperColumn column = new ColumnOrSuperColumn().setColumn(c);
            simpleMutations.add(new Mutation().setColumn_or_supercolumn(column));
        }
        for (SuperColumn c : superColumns)
        {
            ColumnOrSuperColumn column = new ColumnOrSuperColumn().setSuper_column(c);
            superMutations.add(new Mutation().setColumn_or_supercolumn(column));
        }

        mutationMap.put("Standard1", simpleMutations);
        mutationMap.put("Super1", superMutations);

        return mutationMap;
    }

    public Map<String, List<Mutation>> generatePicture() throws IOException{
        List<Column> simpleColumns = new ArrayList<Column>();

        String[] simpleColumnNames={"picture"};
        String[] superColumnNames={"personsWhoLiked","personsOnPicture"};
        for (String c : simpleColumnNames)
        {
            ByteBuffer value = getFBValue();
            totalBytes += value.limit() - value.position();
            simpleColumns.add(new Column(ByteBufferUtil.bytes(c))
                    .setValue(value)
                    .setTimestamp(FBUtilities.timestampMicros()));
        }
        List<SuperColumn> superColumns = new ArrayList<SuperColumn>();
        for (String c : superColumnNames)
        {
            ByteBuffer value = getFBValue();
            totalBytes += value.limit() - value.position();
            ArrayList<Column> a=new ArrayList<Column>();
            a.add(new Column(ByteBufferUtil.bytes(c))
                    .setValue(value)
                    .setTimestamp(FBUtilities.timestampMicros()));
            superColumns.add(new SuperColumn(ByteBufferUtil.bytes(c),a));
        }

        List<Mutation> simpleMutations = new ArrayList<Mutation>();
        List<Mutation> superMutations = new ArrayList<Mutation>();
        Map<String, List<Mutation>> mutationMap = new HashMap<String, List<Mutation>>();

        for (Column c : simpleColumns)
        {
            ColumnOrSuperColumn column = new ColumnOrSuperColumn().setColumn(c);
            simpleMutations.add(new Mutation().setColumn_or_supercolumn(column));
        }
        for (SuperColumn c : superColumns)
        {
            ColumnOrSuperColumn column = new ColumnOrSuperColumn().setSuper_column(c);
            superMutations.add(new Mutation().setColumn_or_supercolumn(column));
        }

        mutationMap.put("Standard1", simpleMutations);
        mutationMap.put("Super1", superMutations);

        return mutationMap;
    }

    public Map<String, List<Mutation>> generateWall() throws IOException{
        List<Column> simpleColumns = new ArrayList<Column>();

        String[] simpleColumnNames={};
        String[] superColumnNames={"commentsOnWall"};
        for (String c : simpleColumnNames)
        {
            ByteBuffer value = getFBValue();
            totalBytes += value.limit() - value.position();
            simpleColumns.add(new Column(ByteBufferUtil.bytes(c))
                    .setValue(value)
                    .setTimestamp(FBUtilities.timestampMicros()));
        }
        List<SuperColumn> superColumns = new ArrayList<SuperColumn>();
        for (String c : superColumnNames)
        {
            ByteBuffer value = getFBValue();
            totalBytes += value.limit() - value.position();
            ArrayList<Column> a=new ArrayList<Column>();
            a.add(new Column(ByteBufferUtil.bytes(c))
                    .setValue(value)
                    .setTimestamp(FBUtilities.timestampMicros()));
            superColumns.add(new SuperColumn(ByteBufferUtil.bytes(c),a));
        }

        List<Mutation> simpleMutations = new ArrayList<Mutation>();
        List<Mutation> superMutations = new ArrayList<Mutation>();
        Map<String, List<Mutation>> mutationMap = new HashMap<String, List<Mutation>>();

        for (Column c : simpleColumns)
        {
            ColumnOrSuperColumn column = new ColumnOrSuperColumn().setColumn(c);
            simpleMutations.add(new Mutation().setColumn_or_supercolumn(column));
        }
        for (SuperColumn c : superColumns)
        {
            ColumnOrSuperColumn column = new ColumnOrSuperColumn().setSuper_column(c);
            superMutations.add(new Mutation().setColumn_or_supercolumn(column));
        }

        //mutationMap.put("Standard1", simpleMutations);
        mutationMap.put("Super1", superMutations);

        return mutationMap;
    }

    public Map<String, List<Mutation>> generateGroup() throws IOException{
        List<Column> simpleColumns = new ArrayList<Column>();

        String[] simpleColumnNames={};
        String[] superColumnNames={"commentsOnGroup","personsOnGroup"};
        for (String c : simpleColumnNames)
        {
            ByteBuffer value = getFBValue();
            totalBytes += value.limit() - value.position();
            simpleColumns.add(new Column(ByteBufferUtil.bytes(c))
                    .setValue(value)
                    .setTimestamp(FBUtilities.timestampMicros()));
        }
        List<SuperColumn> superColumns = new ArrayList<SuperColumn>();
        for (String c : superColumnNames)
        {
            ByteBuffer value = getFBValue();
            totalBytes += value.limit() - value.position();
            ArrayList<Column> a=new ArrayList<Column>();
            a.add(new Column(ByteBufferUtil.bytes(c))
                    .setValue(value)
                    .setTimestamp(FBUtilities.timestampMicros()));
            superColumns.add(new SuperColumn(ByteBufferUtil.bytes(c),a));
        }

        List<Mutation> simpleMutations = new ArrayList<Mutation>();
        List<Mutation> superMutations = new ArrayList<Mutation>();
        Map<String, List<Mutation>> mutationMap = new HashMap<String, List<Mutation>>();

        for (Column c : simpleColumns)
        {
            ColumnOrSuperColumn column = new ColumnOrSuperColumn().setColumn(c);
            simpleMutations.add(new Mutation().setColumn_or_supercolumn(column));
        }
        for (SuperColumn c : superColumns)
        {
            ColumnOrSuperColumn column = new ColumnOrSuperColumn().setSuper_column(c);
            superMutations.add(new Mutation().setColumn_or_supercolumn(column));
        }

        //mutationMap.put("Standard1", simpleMutations);
        mutationMap.put("Super1", superMutations);

        return mutationMap;
    }

    public Map<String, List<Mutation>> generateProfile() throws IOException{
        List<Column> simpleColumns = new ArrayList<Column>();

        String[] simpleColumnNames={"profileProperty"};
        String[] superColumnNames={};
        for (String c : simpleColumnNames)
        {
            ByteBuffer value = getFBValue();
            totalBytes += value.limit() - value.position();
            simpleColumns.add(new Column(ByteBufferUtil.bytes(c))
                    .setValue(value)
                    .setTimestamp(FBUtilities.timestampMicros()));
        }
        List<SuperColumn> superColumns = new ArrayList<SuperColumn>();
        for (String c : superColumnNames)
        {
            ByteBuffer value = getFBValue();
            totalBytes += value.limit() - value.position();
            ArrayList<Column> a=new ArrayList<Column>();
            a.add(new Column(ByteBufferUtil.bytes(c))
                    .setValue(value)
                    .setTimestamp(FBUtilities.timestampMicros()));
            superColumns.add(new SuperColumn(ByteBufferUtil.bytes(c),a));
        }

        List<Mutation> simpleMutations = new ArrayList<Mutation>();
        List<Mutation> superMutations = new ArrayList<Mutation>();
        Map<String, List<Mutation>> mutationMap = new HashMap<String, List<Mutation>>();

        for (Column c : simpleColumns)
        {
            ColumnOrSuperColumn column = new ColumnOrSuperColumn().setColumn(c);
            simpleMutations.add(new Mutation().setColumn_or_supercolumn(column));
        }
        for (SuperColumn c : superColumns)
        {
            ColumnOrSuperColumn column = new ColumnOrSuperColumn().setSuper_column(c);
            superMutations.add(new Mutation().setColumn_or_supercolumn(column));
        }

        mutationMap.put("Standard1", simpleMutations);
        //mutationMap.put("Super1", superMutations);

        return mutationMap;
    }

    public Map<String, List<Mutation>> generateConversation() throws IOException{
        List<Column> simpleColumns = new ArrayList<Column>();

        String[] simpleColumnNames={};
        String[] superColumnNames={"personsOnConversation","messages"};
        for (String c : simpleColumnNames)
        {
            ByteBuffer value = getFBValue();
            totalBytes += value.limit() - value.position();
            simpleColumns.add(new Column(ByteBufferUtil.bytes(c))
                    .setValue(value)
                    .setTimestamp(FBUtilities.timestampMicros()));
        }
        List<SuperColumn> superColumns = new ArrayList<SuperColumn>();
        for (String c : superColumnNames)
        {
            ByteBuffer value = getFBValue();
            totalBytes += value.limit() - value.position();
            ArrayList<Column> a=new ArrayList<Column>();
            a.add(new Column(ByteBufferUtil.bytes(c))
                    .setValue(value)
                    .setTimestamp(FBUtilities.timestampMicros()));
            superColumns.add(new SuperColumn(ByteBufferUtil.bytes(c),a));
        }

        List<Mutation> simpleMutations = new ArrayList<Mutation>();
        List<Mutation> superMutations = new ArrayList<Mutation>();
        Map<String, List<Mutation>> mutationMap = new HashMap<String, List<Mutation>>();

        for (Column c : simpleColumns)
        {
            ColumnOrSuperColumn column = new ColumnOrSuperColumn().setColumn(c);
            simpleMutations.add(new Mutation().setColumn_or_supercolumn(column));
        }
        for (SuperColumn c : superColumns)
        {
            ColumnOrSuperColumn column = new ColumnOrSuperColumn().setSuper_column(c);
            superMutations.add(new Mutation().setColumn_or_supercolumn(column));
        }

        //mutationMap.put("Standard1", simpleMutations);
        mutationMap.put("Super1", superMutations);

        return mutationMap;
    }

    public Map<String, List<Mutation>> generateSetting() throws IOException{
        List<Column> simpleColumns = new ArrayList<Column>();

        String[] simpleColumnNames={"settings"};
        String[] superColumnNames={};
        for (String c : simpleColumnNames)
        {
            ByteBuffer value = getFBValue();
            totalBytes += value.limit() - value.position();
            simpleColumns.add(new Column(ByteBufferUtil.bytes(c))
                    .setValue(value)
                    .setTimestamp(FBUtilities.timestampMicros()));
        }
        List<SuperColumn> superColumns = new ArrayList<SuperColumn>();
        for (String c : superColumnNames)
        {
            ByteBuffer value = getFBValue();
            totalBytes += value.limit() - value.position();
            ArrayList<Column> a=new ArrayList<Column>();
            a.add(new Column(ByteBufferUtil.bytes(c))
                    .setValue(value)
                    .setTimestamp(FBUtilities.timestampMicros()));
            superColumns.add(new SuperColumn(ByteBufferUtil.bytes(c),a));
        }

        List<Mutation> simpleMutations = new ArrayList<Mutation>();
        List<Mutation> superMutations = new ArrayList<Mutation>();
        Map<String, List<Mutation>> mutationMap = new HashMap<String, List<Mutation>>();

        for (Column c : simpleColumns)
        {
            ColumnOrSuperColumn column = new ColumnOrSuperColumn().setColumn(c);
            simpleMutations.add(new Mutation().setColumn_or_supercolumn(column));
        }
        for (SuperColumn c : superColumns)
        {
            ColumnOrSuperColumn column = new ColumnOrSuperColumn().setSuper_column(c);
            superMutations.add(new Mutation().setColumn_or_supercolumn(column));
        }

        mutationMap.put("Standard1", simpleMutations);
        //mutationMap.put("Super1", superMutations);

        return mutationMap;
    }

    public Map<String, List<Mutation>> generateMessage() throws IOException{
        List<Column> simpleColumns = new ArrayList<Column>();

        String[] simpleColumnNames={"message_picture_video"};
        String[] superColumnNames={"hashtags","persons@"};
        for (String c : simpleColumnNames)
        {
            ByteBuffer value = getFBValue();
            totalBytes += value.limit() - value.position();
            simpleColumns.add(new Column(ByteBufferUtil.bytes(c))
                    .setValue(value)
                    .setTimestamp(FBUtilities.timestampMicros()));
        }
        List<SuperColumn> superColumns = new ArrayList<SuperColumn>();
        for (String c : superColumnNames)
        {
            ByteBuffer value = getFBValue();
            totalBytes += value.limit() - value.position();
            ArrayList<Column> a=new ArrayList<Column>();
            a.add(new Column(ByteBufferUtil.bytes(c))
                    .setValue(value)
                    .setTimestamp(FBUtilities.timestampMicros()));
            superColumns.add(new SuperColumn(ByteBufferUtil.bytes(c),a));
        }

        List<Mutation> simpleMutations = new ArrayList<Mutation>();
        List<Mutation> superMutations = new ArrayList<Mutation>();
        Map<String, List<Mutation>> mutationMap = new HashMap<String, List<Mutation>>();

        for (Column c : simpleColumns)
        {
            ColumnOrSuperColumn column = new ColumnOrSuperColumn().setColumn(c);
            simpleMutations.add(new Mutation().setColumn_or_supercolumn(column));
        }
        for (SuperColumn c : superColumns)
        {
            ColumnOrSuperColumn column = new ColumnOrSuperColumn().setSuper_column(c);
            superMutations.add(new Mutation().setColumn_or_supercolumn(column));
        }

        mutationMap.put("Standard1", simpleMutations);
        mutationMap.put("Super1", superMutations);

        return mutationMap;
    }

    public Map<String,List<Mutation>> addTo(ByteBuffer key,String superColumnName) throws IOException{
        List<Column> simpleColumns = new ArrayList<Column>();

        simpleColumns.add(new Column(key)
                .setValue((ByteBuffer)null)
                .setTimestamp(FBUtilities.timestampMicros()));

        SuperColumn superColumn=new SuperColumn(ByteBufferUtil.bytes(superColumnName),simpleColumns);

        List<Mutation> mutations = new ArrayList<Mutation>();
        Map<String, List<Mutation>> mutationMap = new HashMap<String, List<Mutation>>();
        ColumnOrSuperColumn column = new ColumnOrSuperColumn().setSuper_column(superColumn);
        mutations.add(new Mutation().setColumn_or_supercolumn(column));
        mutationMap.put("Super1", mutations);

        return mutationMap;
    }

    public Map<String, List<Mutation>> deleteComment(){
        List<Column> simpleColumns = new ArrayList<Column>();

        String[] simpleColumnNames={"comment","picture","video"};
        String[] superColumnNames={"relatedComments","personsWhoLiked","hashtags","persons@"};

        List<Mutation> simpleMutations = new ArrayList<Mutation>();
        List<Mutation> superMutations = new ArrayList<Mutation>();
        Map<String, List<Mutation>> mutationMap = new HashMap<String, List<Mutation>>();

        for (String c : simpleColumnNames)
        {
            Deletion del=new Deletion().setSuper_column(ByteBufferUtil.bytes(c)).setTimestamp(FBUtilities.timestampMicros());
            simpleMutations.add(new Mutation().setDeletion(del));
        }
        for (String c : superColumnNames)
        {
            Deletion del=new Deletion().setSuper_column(ByteBufferUtil.bytes(c)).setTimestamp(FBUtilities.timestampMicros());
            superMutations.add(new Mutation().setDeletion(del));
        }

        mutationMap.put("Standard1", simpleMutations);
        mutationMap.put("Super1", superMutations);

        return mutationMap;
    }

    public Map<String, List<Mutation>> deleteAlbum(){
        List<Column> simpleColumns = new ArrayList<Column>();

        String[] simpleColumnNames={"name"};
        String[] superColumnNames={"pictures"};

        List<Mutation> simpleMutations = new ArrayList<Mutation>();
        List<Mutation> superMutations = new ArrayList<Mutation>();
        Map<String, List<Mutation>> mutationMap = new HashMap<String, List<Mutation>>();

        for (String c : simpleColumnNames)
        {
            Deletion del=new Deletion().setSuper_column(ByteBufferUtil.bytes(c)).setTimestamp(FBUtilities.timestampMicros());
            simpleMutations.add(new Mutation().setDeletion(del));
        }
        for (String c : superColumnNames)
        {
            Deletion del=new Deletion().setSuper_column(ByteBufferUtil.bytes(c)).setTimestamp(FBUtilities.timestampMicros());
            superMutations.add(new Mutation().setDeletion(del));
        }

        mutationMap.put("Standard1", simpleMutations);
        mutationMap.put("Super1", superMutations);

        return mutationMap;
    }

    public Map<String, List<Mutation>> deletePicture(){
        List<Column> simpleColumns = new ArrayList<Column>();

        String[] simpleColumnNames={"picture"};
        String[] superColumnNames={"personsWhoLiked","personsOnPicture"};

        List<Mutation> simpleMutations = new ArrayList<Mutation>();
        List<Mutation> superMutations = new ArrayList<Mutation>();
        Map<String, List<Mutation>> mutationMap = new HashMap<String, List<Mutation>>();

        for (String c : simpleColumnNames)
        {
            Deletion del=new Deletion().setSuper_column(ByteBufferUtil.bytes(c)).setTimestamp(FBUtilities.timestampMicros());
            simpleMutations.add(new Mutation().setDeletion(del));
        }
        for (String c : superColumnNames)
        {
            Deletion del=new Deletion().setSuper_column(ByteBufferUtil.bytes(c)).setTimestamp(FBUtilities.timestampMicros());
            superMutations.add(new Mutation().setDeletion(del));
        }

        mutationMap.put("Standard1", simpleMutations);
        mutationMap.put("Super1", superMutations);

        return mutationMap;
    }

    public Map<String, List<Mutation>> deleteWall(){
        List<Column> simpleColumns = new ArrayList<Column>();

        String[] simpleColumnNames={};
        String[] superColumnNames={"commentsOnWall"};

        List<Mutation> simpleMutations = new ArrayList<Mutation>();
        List<Mutation> superMutations = new ArrayList<Mutation>();
        Map<String, List<Mutation>> mutationMap = new HashMap<String, List<Mutation>>();

        for (String c : simpleColumnNames)
        {
            Deletion del=new Deletion().setSuper_column(ByteBufferUtil.bytes(c)).setTimestamp(FBUtilities.timestampMicros());
            simpleMutations.add(new Mutation().setDeletion(del));
        }
        for (String c : superColumnNames)
        {
            Deletion del=new Deletion().setSuper_column(ByteBufferUtil.bytes(c)).setTimestamp(FBUtilities.timestampMicros());
            superMutations.add(new Mutation().setDeletion(del));
        }

        //mutationMap.put("Standard1", simpleMutations);
        mutationMap.put("Super1", superMutations);

        return mutationMap;
    }

    public Map<String, List<Mutation>> deleteGroup(){
        List<Column> simpleColumns = new ArrayList<Column>();

        String[] simpleColumnNames={};
        String[] superColumnNames={"commentsOnGroup","personsOnGroup"};

        List<Mutation> simpleMutations = new ArrayList<Mutation>();
        List<Mutation> superMutations = new ArrayList<Mutation>();
        Map<String, List<Mutation>> mutationMap = new HashMap<String, List<Mutation>>();

        for (String c : simpleColumnNames)
        {
            Deletion del=new Deletion().setSuper_column(ByteBufferUtil.bytes(c)).setTimestamp(FBUtilities.timestampMicros());
            simpleMutations.add(new Mutation().setDeletion(del));
        }
        for (String c : superColumnNames)
        {
            Deletion del=new Deletion().setSuper_column(ByteBufferUtil.bytes(c)).setTimestamp(FBUtilities.timestampMicros());
            superMutations.add(new Mutation().setDeletion(del));
        }

        //mutationMap.put("Standard1", simpleMutations);
        mutationMap.put("Super1", superMutations);

        return mutationMap;
    }

    public Map<String, List<Mutation>> deleteProfile(){
        List<Column> simpleColumns = new ArrayList<Column>();

        String[] simpleColumnNames={"profileProperty"};
        String[] superColumnNames={};

        List<Mutation> simpleMutations = new ArrayList<Mutation>();
        List<Mutation> superMutations = new ArrayList<Mutation>();
        Map<String, List<Mutation>> mutationMap = new HashMap<String, List<Mutation>>();

        for (String c : simpleColumnNames)
        {
            Deletion del=new Deletion().setSuper_column(ByteBufferUtil.bytes(c)).setTimestamp(FBUtilities.timestampMicros());
            simpleMutations.add(new Mutation().setDeletion(del));
        }
        for (String c : superColumnNames)
        {
            Deletion del=new Deletion().setSuper_column(ByteBufferUtil.bytes(c)).setTimestamp(FBUtilities.timestampMicros());
            superMutations.add(new Mutation().setDeletion(del));
        }

        mutationMap.put("Standard1", simpleMutations);
        //mutationMap.put("Super1", superMutations);

        return mutationMap;
    }

    public Map<String, List<Mutation>> deleteConversation(){
        List<Column> simpleColumns = new ArrayList<Column>();

        String[] simpleColumnNames={};
        String[] superColumnNames={"personsOnConversation","messages"};

        List<Mutation> simpleMutations = new ArrayList<Mutation>();
        List<Mutation> superMutations = new ArrayList<Mutation>();
        Map<String, List<Mutation>> mutationMap = new HashMap<String, List<Mutation>>();

        for (String c : simpleColumnNames)
        {
            Deletion del=new Deletion().setSuper_column(ByteBufferUtil.bytes(c)).setTimestamp(FBUtilities.timestampMicros());
            simpleMutations.add(new Mutation().setDeletion(del));
        }
        for (String c : superColumnNames)
        {
            Deletion del=new Deletion().setSuper_column(ByteBufferUtil.bytes(c)).setTimestamp(FBUtilities.timestampMicros());
            superMutations.add(new Mutation().setDeletion(del));
        }

        //mutationMap.put("Standard1", simpleMutations);
        mutationMap.put("Super1", superMutations);

        return mutationMap;
    }

    public Map<String, List<Mutation>> deleteSetting(){
        List<Column> simpleColumns = new ArrayList<Column>();

        String[] simpleColumnNames={"settings"};
        String[] superColumnNames={};

        List<Mutation> simpleMutations = new ArrayList<Mutation>();
        List<Mutation> superMutations = new ArrayList<Mutation>();
        Map<String, List<Mutation>> mutationMap = new HashMap<String, List<Mutation>>();

        for (String c : simpleColumnNames)
        {
            Deletion del=new Deletion().setSuper_column(ByteBufferUtil.bytes(c)).setTimestamp(FBUtilities.timestampMicros());
            simpleMutations.add(new Mutation().setDeletion(del));
        }
        for (String c : superColumnNames)
        {
            Deletion del=new Deletion().setSuper_column(ByteBufferUtil.bytes(c)).setTimestamp(FBUtilities.timestampMicros());
            superMutations.add(new Mutation().setDeletion(del));
        }

        mutationMap.put("Standard1", simpleMutations);
        //mutationMap.put("Super1", superMutations);

        return mutationMap;
    }

    public Map<String, List<Mutation>> deleteMessage(){
        List<Column> simpleColumns = new ArrayList<Column>();

        String[] simpleColumnNames={"message_picture_video"};
        String[] superColumnNames={"hashtags","persons@"};

        List<Mutation> simpleMutations = new ArrayList<Mutation>();
        List<Mutation> superMutations = new ArrayList<Mutation>();
        Map<String, List<Mutation>> mutationMap = new HashMap<String, List<Mutation>>();

        for (String c : simpleColumnNames)
        {
            Deletion del=new Deletion().setSuper_column(ByteBufferUtil.bytes(c)).setTimestamp(FBUtilities.timestampMicros());
            simpleMutations.add(new Mutation().setDeletion(del));
        }
        for (String c : superColumnNames)
        {
            Deletion del=new Deletion().setSuper_column(ByteBufferUtil.bytes(c)).setTimestamp(FBUtilities.timestampMicros());
            superMutations.add(new Mutation().setDeletion(del));
        }

        mutationMap.put("Standard1", simpleMutations);
        mutationMap.put("Super1", superMutations);

        return mutationMap;
    }

    public Map<String,List<Mutation>> remove(ByteBuffer key) throws IOException{

        List<Mutation> mutations = new ArrayList<Mutation>();
        Map<String, List<Mutation>> mutationMap = new HashMap<String, List<Mutation>>();

        Deletion del=new Deletion().setSuper_column(key).setTimestamp(FBUtilities.timestampMicros());
        mutations.add(new Mutation().setDeletion(del));
        mutationMap.put("Standard1", mutations);

        return mutationMap;
    }

}
