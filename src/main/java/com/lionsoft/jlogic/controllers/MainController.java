package com.lionsoft.jlogic;

import org.springframework.boot.system.ApplicationHome;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.boot.info.BuildProperties;
import java.time.Instant;
import java.io.FileNotFoundException;
import org.springframework.web.server.ResponseStatusException;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.io.*;
import java.util.stream.Stream;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.boot.system.ApplicationHome;
import java.util.concurrent.locks.*;
import org.apache.commons.lang3.time.DateUtils;

/*
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.session.SessionInformation;
*/

class PackageInfo {
  private String name;
  private String version;
  private Instant buildTime;
  
	public PackageInfo(BuildProperties buildProperties) {
		this.name = buildProperties.getName();
		this.version = buildProperties.getVersion();
		this.buildTime = buildProperties.getTime();
	}
	
	public String getName() {
		return name;
	}
	
	public String getVersion() {
		return version;
	}
	
	public Instant getBuildTime() {
		return buildTime;
	}
};

class SystemInfo {
  private String nodeName;
  
	public SystemInfo() {
    try
    {
        InetAddress addr;
        addr = InetAddress.getLocalHost();
        nodeName = addr.getHostName();
    }
    catch (UnknownHostException ex)
    {
        System.out.println("Hostname can not be resolved");
    }
	}
	  
  public String getNodeName () {
    return nodeName;
  }
}

@RestController
public class MainController {

  Logger logger = LoggerFactory.getLogger(ProgramController.class);
  ApplicationHome home = new ApplicationHome(BlueprintService.class);

  @Autowired
  BuildProperties buildProperties;
  
  @Autowired
  CatalogService catalogService;
  
  @Autowired
  SessionService sessionService;
  
/*  
  @Autowired
  private SessionRegistry sessionRegistry;
*/
  
	@GetMapping(value="/", produces = MediaType.APPLICATION_JSON_VALUE)
	public PackageInfo getPackageInfo() {
		return new PackageInfo(buildProperties);
	}

	@GetMapping(value="/catalog", produces = MediaType.APPLICATION_JSON_VALUE)
	public String getCatalog() {
		return catalogService.getCatalog().toString();
	}
	
  private JSONObject processNode (JSONObject jnode, String path) {
    JSONObject jo = jnode;
    String java = jnode.optString("java");
    
    if (java.length() > 0) {
      // when "java":"@my-code.java" load file and set "java" attribute
      if (java.substring(0,1).equals("@")) {
        String filename = path+"/"+java.substring(1);
        System.out.println("Loading "+filename);
        
        try {
          String content = new String (Files.readAllBytes(Paths.get(filename)));
          jo.put("java", content);
        } catch (IOException e) {
          logger.error("Can't load "+filename);
        }
      }
    }
    
    return (jo);
  }

	private JSONObject getAssetJSON() {
	  String assetDir = home.getDir()+"/../data/asset";
	  JSONObject jAsset = new JSONObject();
    JSONArray jNodeTypesArray;
    JSONArray jTypesArray = new JSONArray();
    String content;

    try { 
      content = new String (Files.readAllBytes(Paths.get(assetDir+"/node_types.json")));
      JSONObject jNodeTypes = new JSONObject(content);
      jNodeTypesArray = jNodeTypes.getJSONArray("node_types");
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
    }

    jAsset.put("nodes", new JSONArray());
    jAsset.put("types", new JSONArray());
    jAsset.put("node_types", jNodeTypesArray);

    jAsset = getAssetJSON(jAsset, assetDir);
    jAsset = getAssetJSON(jAsset, home.getDir()+"/../plugin");

    return (jAsset);
  }

	private JSONObject getAssetJSON(JSONObject jAsset , String assetDir) {
	  JSONArray jTypesArray = jAsset.getJSONArray("types");
	  JSONArray jNodesArray = jAsset.getJSONArray("nodes");
	  
	  String content;
	  
	    
    try (Stream<Path> paths = Files.walk(Paths.get(assetDir))) {
      List<String> list = paths.map(path -> path.toString()).filter(f -> f.endsWith(".json")).collect(Collectors.toList());
      
      for (int i = 0; i < list.size(); i++) {
        //System.out.println(list.get(i));
        
        File file = Paths.get(list.get(i)).toFile();
        File parentDir = file.getParentFile(); // to get the parent dir 
        String parentDirName = file.getParent(); // to get the parent dir name
        //System.setProperty("user.dir", parentDirName);
    
        content = new String (Files.readAllBytes(Paths.get(list.get(i))));
        
        try {
          // Array of nodes, no types
          JSONArray ja = new JSONArray(content);

          for (int j = 0; j < ja.length(); j++)
            //jNodesArray.put(ja.get(j));
            jNodesArray.put(processNode((JSONObject)ja.get(j), parentDirName));
        }
        catch (JSONException e1) {
          // Try object with types and nodes
          
          Boolean hasTypes = Boolean.FALSE;
          Boolean hasNodes = Boolean.FALSE;
          JSONObject jo = new JSONObject(content);
          
          if (jo.has("types")) {
            JSONArray jt = jo.getJSONArray("types");

            for (int j = 0; j < jt.length(); j++)
              jTypesArray.put(jt.get(j));
              
            hasTypes = Boolean.TRUE;
          }
          
          if (jo.has("nodes")) {
            JSONArray jn = jo.getJSONArray("nodes");
            for (int j = 0; j < jn.length(); j++)
              jNodesArray.put(processNode((JSONObject)jn.get(j), parentDirName));
              
            hasNodes = Boolean.TRUE;
          }
          
          if (!hasNodes && !hasTypes && !jo.has("node_types")) {
            jNodesArray.put(processNode(jo, parentDirName));
          }
        }
      }
    } catch (IOException e) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
    }

		return jAsset;
	}
	@GetMapping(value="/asset", produces = MediaType.APPLICATION_JSON_VALUE)
	public String getAsset() throws IOException {
	  JSONObject jAsset = getAssetJSON();
		return jAsset.toString();
	}

	@GetMapping(value="/jsAsset", produces = "application/x-javascript")
	public String getJSAsset() {
	  JSONObject jAsset = getAssetJSON();
		return "_asset = "+jAsset.toString()+";\n";  
	}

	@GetMapping(value="/jsBlueprints", produces = "application/x-javascript")
	public String getJSBlueprints() {
	  String content = "{}";
	  /*
    Catalog c = new Catalog(Catalog.BLUEPRINT_CATALOG, Boolean.TRUE);
    try {
      c.open();
      content = c.toString();
      c.unlock();
    }
    catch (FileNotFoundException e) {
      //logger.error(e.getMessage());
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
    }*/
    
		return "_blueprints = "+content+";\n";  
	}
/*
	@GetMapping("/jsDynamic")
	public String getJSDyn() {
	  SystemInfo systemInfo = new SystemInfo();
    PackageInfo packageInfo = new PackageInfo(buildProperties);

    JSONArray jmenu = new JSONArray(
      //"{ \"href\":\"%s\", \"icon\":\"%s\", \"label\":\"%s\" }"+
      "[{ \"href\":\"/home\", \"icon\":\"i-home\", \"label\":\"Home\" },"+
      "{ \"href\":\"/bp\", \"icon\":\"i-project-diagram\", \"label\":\"Blueprints\" },"+
      "{ \"href\":\"/apipanel\", \"icon\":\"i-cube\", \"label\":\"APIs\" },"+
      "{ \"href\":\"/edit-properties\", \"icon\":\"i-sliders-h\", \"label\":\"Properties\" },"+
      "{ \"href\":\"/view-sessions\", \"icon\":\"i-sign-in-alt\", \"label\":\"Sessions\" },"+
      "{ \"href\":\"/stats\", \"icon\":\"i-chart-bar\", \"label\":\"Analytics\" },"+
      "{ \"href\":\"/users\", \"icon\":\"i-users\", \"label\":\"Users\" }"+
      "]");
    
    JSONObject jo = new JSONObject();
    
    JSONObject jsystem = new JSONObject();
    jsystem.put("nodename", systemInfo.getNodeName());
    
    JSONObject jpackage = new JSONObject();
    jpackage.put("application_name", packageInfo.getName());
    jpackage.put("package", packageInfo.getName());
    jpackage.put("version", packageInfo.getVersion());
    
    jo.put("system", jsystem);
    jo.put("package", jpackage);
    jo.put("menu", jmenu);
    jo.put("auth", new JSONObject("{\"enabled\":false, \"username\": \"Anonymous\"}"));
    jo.put("ssl", Boolean.FALSE);

    return "__SERVER = "+jo.toString()+";\n";    
	}
*/
/*	
  @Bean
  SessionRegistry sessionRegistry() { 
      return new SessionRegistryImpl(); 
  }*/
  	
	@GetMapping(value="/sessions", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<Session> getSessions() {
    //SessionsUtils sessionUtils = new SessionsUtils();
    List<Session> l = sessionService.getList();
    
		return l;
	}
  	
	@GetMapping(value="/stats", produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, Long> getStats() {
		return sessionService.getStats(DateUtils.addMinutes(new Date(), -10), new Date());
	}
}

