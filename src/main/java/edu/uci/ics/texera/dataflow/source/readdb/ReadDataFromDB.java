package edu.uci.ics.texera.dataflow.source.readdb;

import edu.uci.ics.texera.api.constants.SchemaConstants;
import edu.uci.ics.texera.api.exception.DataflowException;
import edu.uci.ics.texera.api.schema.Attribute;
import edu.uci.ics.texera.api.schema.AttributeType;
import edu.uci.ics.texera.api.schema.Schema;
import edu.uci.ics.texera.dataflow.source.asterix.AsterixSource;
import edu.uci.ics.texera.dataflow.source.asterix.AsterixSourcePredicate;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

public class ReadDataFromDB {

    public static String RAW_DATA = "rawData";
    public static Attribute RAW_DATA_ATTR = new Attribute(RAW_DATA, AttributeType.TEXT);
    public static Schema ATERIX_SOURCE_SCHEMA = new Schema(SchemaConstants._ID_ATTRIBUTE, RAW_DATA_ATTR);
    private JSONArray resultJsonArray;

    public JSONArray getResultJaonArray(){
        return resultJsonArray;
    }
    public Schema getAterixSourceSchema(){
        return ATERIX_SOURCE_SCHEMA;
    }


    public void open(AsterixSourcePredicate predicate){ //, JSONArray resultJsonArray, Schema ATERIX_SOURCE_SCHEMA
        ReadProperties rps = new ReadProperties();
        String dbname = rps.getDbName();
        if(dbname.equals("elasticsearch")){
            readDataFromES(predicate);  //, resultJsonArray, ATERIX_SOURCE_SCHEMA
        }else if(dbname.equals("mysql")){
            readDataFromMysql(predicate);
        }else{

        }

    }

    private void readDataFromES(AsterixSourcePredicate predicate){ //, JSONArray resultJsonArray, Schema ATERIX_SOURCE_SCHEMA
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            String queryUrl = getUrl(predicate);
            HttpGet get=new HttpGet(queryUrl);
            System.out.println("post: "+queryUrl);
            org.apache.http.HttpResponse response = httpClient.execute(get);

            // if status is 200 OK, store the results
            if (response.getStatusLine().getStatusCode() == 200) {
                String result= EntityUtils.toString(response.getEntity());
                JSONObject jsonObject = new JSONObject(result);
                resultJsonArray = jsonObject.getJSONArray("response");

                // 创建和对象相关的Schema
                // 虽然此处Schema是static，但是每次连接数据库，都需要根据数据库中的数据改变Schema的值
                Schema.Builder schemaBuilder = new Schema.Builder();
                JSONObject firstLineData = resultJsonArray.getJSONObject(0);
                Iterator<String> iter = firstLineData.keys();
                while(iter.hasNext()) {
                    String attributeName = iter.next();
                    schemaBuilder.add(new Attribute(attributeName, AttributeType.STRING));
                }
                ATERIX_SOURCE_SCHEMA = schemaBuilder.build();

//                System.out.println("["+System.currentTimeMillis()+"]jsonObject:"+resultJsonArray.toString());

            } else {
                throw new DataflowException("Send query to asterix failed ");
            }
        } catch (Exception e) {
            throw new DataflowException(e);
        }
    }

    private void readDataFromMysql(AsterixSourcePredicate predicate){

    }

//    "http://127.0.0.1:9200/_dQuery/para?indexName=iris-2&from=0&size=5&query="
    private String getUrl(AsterixSourcePredicate predicate) {
        // predicate: DataSet - indexName; field - queryString; limit - size
        String host = "127.0.0.1";
        int port = 9200;
        String indexName = "iris-2";
        String queryString = "";
        int fromNum = 0;
        int sizeNum = 1;

        if(predicate.getHost()!=null || predicate.getHost().length()>=7) {
            host = predicate.getHost();
        }
        if(predicate.getPort()!=null || predicate.getPort()>0) {
            port = predicate.getPort();
        }
        if(predicate.getDataset()!=null) {
            indexName = predicate.getDataset();
        }
        if(predicate.getField()!=null && predicate.getField().length()!=0) {
            queryString = predicate.getField();
        }
        if(predicate.getLimit()!=null){
            sizeNum = predicate.getLimit();
        }
        String queryUrl = "http://" + host + ":" + port + "/_dQuery/para?"+
                "indexName="+indexName+
                "&query="+queryString+
                "&from="+fromNum+
                "&size="+sizeNum;
        return queryUrl;
    }
}
