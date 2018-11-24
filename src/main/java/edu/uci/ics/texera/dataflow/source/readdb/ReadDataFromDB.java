package edu.uci.ics.texera.dataflow.source.readdb;

import edu.uci.ics.texera.api.constants.SchemaConstants;
import edu.uci.ics.texera.api.exception.DataflowException;
import edu.uci.ics.texera.api.schema.Attribute;
import edu.uci.ics.texera.api.schema.AttributeType;
import edu.uci.ics.texera.api.schema.Schema;
import edu.uci.ics.texera.dataflow.source.asterix.AsterixSource;
import edu.uci.ics.texera.dataflow.source.asterix.AsterixSourcePredicate;
import edu.uci.ics.texera.dataflow.utils.DataflowUtils;
import edu.uci.ics.texera.storage.constants.LuceneAnalyzerConstants;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static edu.uci.ics.texera.api.constants.SchemaConstants._ID;

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
        }else {
            readDataFromAsterix(predicate);
        }
    }

    private void readDataFromES(AsterixSourcePredicate predicate){ //, JSONArray resultJsonArray, Schema ATERIX_SOURCE_SCHEMA
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            String queryUrl = getUrl(predicate);
            HttpGet get=new HttpGet(queryUrl);
            System.out.println("[readDataFromES] post: "+queryUrl);
            org.apache.http.HttpResponse response = httpClient.execute(get);

            // if status is 200 OK, store the results
            if (response.getStatusLine().getStatusCode() == 200) {
                String result= EntityUtils.toString(response.getEntity());
                JSONObject jsonObject = new JSONObject(result);
                resultJsonArray = jsonObject.getJSONArray("response");

                // 创建和对象相关的Schema
                // 虽然此处Schema是static，但是每次连接数据库，都需要根据数据库中的数据改变Schema的值
                Schema.Builder schemaBuilder = new Schema.Builder();
                JSONObject firstLineData = resultJsonArray.getJSONObject(0); // 从es数据中获取所有数据属性
                Iterator<String> iter = firstLineData.keys();
                while(iter.hasNext()) {
                    String attributeName = iter.next();
                    System.out.println("[readDataFromES] attributeName = "+attributeName);
                    if(attributeName.equals(_ID)) {
                        schemaBuilder.add(SchemaConstants._ID_ATTRIBUTE);
                    }
                    else {
                        schemaBuilder.add(new Attribute(attributeName, AttributeType.STRING));
                    }
                }
                ATERIX_SOURCE_SCHEMA = schemaBuilder.build();

                // System.out.println("["+System.currentTimeMillis()+"]jsonObject:"+resultJsonArray.toString());

            } else {
                throw new DataflowException("Send query to asterix failed ");
            }
        } catch (Exception e) {
            throw new DataflowException(e);
        }
    }

    private void readDataFromMysql(AsterixSourcePredicate predicate){
        throw new DataflowException("Sorry! Do not support mysql now!");
    }

    private void readDataFromAsterix(AsterixSourcePredicate predicate){
        try {
            String asterixAddress = "http://" + predicate.getHost() + ":" + predicate.getPort() +
                    "/query/service";
            String asterixQuery = generateAsterixQuery(predicate);
            HttpResponse<JsonNode> jsonResponse = Unirest.post(asterixAddress)
                    .queryString("statement", asterixQuery)
                    .field("mode", "immediate")
                    .asJson();

            // if status is 200 OK, store the results
            if (jsonResponse.getStatus() == 200) {
                this.resultJsonArray = jsonResponse.getBody().getObject().getJSONArray("results");
            } else {
                throw new DataflowException("Send query to asterix failed: " +
                        "error status: " + jsonResponse.getStatusText() + ", " +
                        "error body: " + jsonResponse.getBody().toString());
            }
        } catch (UnirestException e) {
            throw new DataflowException(e);
        }
    }

    // used to generate elasticsearch query url
    // "http://127.0.0.1:9200/_dQuery/para?indexName=iris-2&from=0&size=5&query="
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

    // used to generate asterix query sentence
    private static String generateAsterixQuery(AsterixSourcePredicate predicate) {
        String asDataset = "ds";
        StringBuilder sb = new StringBuilder();
        sb.append("use " + predicate.getDataverse() + ';').append("\n");
        sb.append("select * ").append("\n");
        sb.append("from " + predicate.getDataset() + " as " + asDataset).append("\n");
        sb.append("where true").append("\n");
        if (predicate.getField() != null && predicate.getKeyword() != null) {
            List<String> keywordList = DataflowUtils.tokenizeQuery(
                    LuceneAnalyzerConstants.standardAnalyzerString(), predicate.getKeyword());
            String asterixKeyword =
                    "[" +
                            keywordList.stream().map(keyword -> "\"" + keyword + "\"")
                                    .collect(Collectors.joining(", ")) +
                            "]";
            String asterixField = "`" + predicate.getField() + "`";
            sb.append("and ftcontains(" + asDataset + "." + asterixField + ", ");
            sb.append(asterixKeyword + ", " + "{\"mode\":\"all\"}" + ")").append("\n");
        }
        if(predicate.getStartDate() != null){
            String startDate = predicate.getStartDate();
            sb.append("and create_at >= datetime(\""+startDate +"T00:00:04.000Z\")").append("\n");
        }
        if(predicate.getEndDate() != null){
            String endDate = predicate.getEndDate();

            sb.append("and create_at <= datetime(\""+endDate +"T00:00:04.000Z\")").append("\n");
        }
        if (predicate.getLimit() != null) {
            sb.append("limit " + predicate.getLimit()).append("\n");
        }
        sb.append(";");
        return sb.toString();
    }
}
