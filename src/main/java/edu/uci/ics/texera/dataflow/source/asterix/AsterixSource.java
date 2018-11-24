package edu.uci.ics.texera.dataflow.source.asterix;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Iterator;

import edu.uci.ics.texera.api.field.*;
import edu.uci.ics.texera.dataflow.source.readdb.ReadDataFromDB;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;

import edu.uci.ics.texera.api.constants.ErrorMessages;
import edu.uci.ics.texera.api.constants.SchemaConstants;
import edu.uci.ics.texera.api.dataflow.ISourceOperator;
import edu.uci.ics.texera.api.exception.DataflowException;
import edu.uci.ics.texera.api.exception.TexeraException;
import edu.uci.ics.texera.api.schema.Attribute;
import edu.uci.ics.texera.api.schema.AttributeType;
import edu.uci.ics.texera.api.schema.Schema;
import edu.uci.ics.texera.api.tuple.Tuple;
import edu.uci.ics.texera.dataflow.utils.DataflowUtils;
import edu.uci.ics.texera.storage.constants.LuceneAnalyzerConstants;
import org.json.JSONObject;

public class AsterixSource implements ISourceOperator {

    public static String RAW_DATA = "rawData";
    public static Attribute RAW_DATA_ATTR = new Attribute(RAW_DATA, AttributeType.TEXT);
    public static Schema ATERIX_SOURCE_SCHEMA = new Schema(SchemaConstants._ID_ATTRIBUTE, RAW_DATA_ATTR);

    private final AsterixSourcePredicate predicate;
    private JSONArray resultJsonArray;

    private int cursor = CLOSED;

    public AsterixSource(AsterixSourcePredicate predicate) {
        this.predicate = predicate;
    }

    @Override
    // dingguangwei
    public void open() throws TexeraException {
        if (cursor == OPENED) {
            return;
        }
        ReadDataFromDB readDataFromDB = new ReadDataFromDB();
        readDataFromDB.open(predicate); //, resultJsonArray, ATERIX_SOURCE_SCHEMA
        resultJsonArray = readDataFromDB.getResultJaonArray();
        ATERIX_SOURCE_SCHEMA = readDataFromDB.getAterixSourceSchema();
        cursor = OPENED;
    }

    @Override
    public Tuple getNextTuple() throws TexeraException {
        if (cursor == CLOSED) {
            throw new DataflowException(ErrorMessages.OPERATOR_NOT_OPENED);
        }
        if (cursor < resultJsonArray.length()) {
            Tuple.Builder tupleBuilder = new Tuple.Builder();
            for(Attribute attribute : ATERIX_SOURCE_SCHEMA.getAttributes()){
                if(attribute.getType().equals(AttributeType.DOUBLE)) {
                    tupleBuilder.add(attribute, new DoubleField((double)(resultJsonArray.getJSONObject(cursor).get(attribute.getName()))));
                } else if(attribute.getType().equals(AttributeType.INTEGER)) {
                    tupleBuilder.add(attribute, new IntegerField((int)(resultJsonArray.getJSONObject(cursor).get(attribute.getName()))));
                } else {
                    tupleBuilder.add(attribute, new StringField(resultJsonArray.getJSONObject(cursor).get(attribute.getName()).toString()));
                }
            }
            Tuple tuple = tupleBuilder.build();
            cursor ++;
            System.out.println("tuple:"+tuple.toString());
            return tuple;
        }
        return null;
    }

    @Override
    public void close() throws TexeraException {
        if (cursor == CLOSED) {
            return;
        }
        cursor = CLOSED;
    }

    @Override
    public Schema getOutputSchema() {
        return ATERIX_SOURCE_SCHEMA;
    }

    public Schema transformToOutputSchema(Schema... inputSchema) throws DataflowException {
        throw new TexeraException(ErrorMessages.INVALID_INPUT_SCHEMA_FOR_SOURCE);
    }
}