package com.lionsoft.jlogic;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import java.util.*;

public class Utils {

  /**
   * Convert a json string into a Map
   */
  static Map<String, Object> jsonToMap(String data) throws ParseException {
    JSONParser jsonParser = new JSONParser();
    JSONObject jdata;

    jdata = (JSONObject) jsonParser.parse(data);
    
    Map<String, Object> map = new HashMap();

    for (Iterator iterator = jdata.keySet().iterator(); iterator.hasNext();) {
        String key = (String) iterator.next();
        
        if (jdata.get(key) instanceof JSONArray) {
          JSONArray ja = (JSONArray) jdata.get(key);
          Object[] oa = new Object[ja.size()];
          
          for (int i=0; i<ja.size(); i++) {
            oa[i] = ja.get(i);
          }
            
          map.put(key, oa);
        }
        else
          map.put(key, jdata.get(key));
    }
    
    return(map);
  }
}
