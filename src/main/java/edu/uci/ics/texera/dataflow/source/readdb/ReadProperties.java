package edu.uci.ics.texera.dataflow.source.readdb;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ReadProperties {
    private static String PROPERTIES_PATH="texera.properties";
    private String dbname;

    public String getDbName(){

        dbname = GetValueByKey(PROPERTIES_PATH, "dbname");
        return dbname;
    }

    public static String GetValueByKey(String filePath, String key) {
        Properties pps = new Properties();
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(filePath));
            pps.load(in);
            String value = pps.getProperty(key);
//            System.out.println(key + " = " + value);
            return value;

        }catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    public static void main(String []agrs){
        ReadProperties r = new ReadProperties();
        String dbname = r.getDbName();
        System.out.println(dbname);
    }


}
