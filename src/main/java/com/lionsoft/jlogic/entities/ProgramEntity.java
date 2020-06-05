package com.lionsoft.jlogic;

import org.springframework.boot.system.ApplicationHome;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
//import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import java.io.*;
import java.util.*;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
import java.beans.IntrospectionException;
import java.net.URLClassLoader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;

import javax.servlet.http.HttpServletRequest;

 
@Entity
@Table(name="PROGRAM")
public class ProgramEntity {

    // Run result
    public static final int SUCCESS = 0;  
    public static final int METHOD_NOT_FOUND = -1;  
    public static final int BAD_INPUT = -2;  
    public static final int EXCEPTION = -99;
    
    public final String jarName = "Program.jar";
    public final String className = "Program.class";
    
    @Transient
    Logger logger = LoggerFactory.getLogger(ProgramEntity.class);
    
    @Transient
    ApplicationHome home = new ApplicationHome(ProgramEntity.class);
 
    @Id
    //@GeneratedValue
    private String id;
     
    @Column(name="name")
    private String name;
     
    @Enumerated(EnumType.STRING)
    @Column(name="status")
    private ProgramStatus status;
    
    @Column(name="creation_time")
    private Date creationTime;
    
    @Column(name="update_time")
    private Date updateTime;
    
    @Column(name="build_time")
    private Date buildTime;
    
    @Column(name="owner")
    private String owner;
    
    @Column(name="parent_id")
    private String parentId;
 
    @OneToMany(cascade = CascadeType.ALL,
            fetch = FetchType.EAGER,
            mappedBy = "program")
    private List<BlueprintEntity> blueprints;
    
    @Transient
    private List<String> classPathList;
    
    //@Transient
    //private List<String> dependencies;

    @Transient
	  private int result;
    
    @Transient
	  private String message;
    
    @Transient
	  private String output;
    
    @Transient
	  private String httpResponse;
    
    @Transient
	  private String homeDir = null;
    
    /*@Transient
	  private String programDir = null;*/
          
    public ProgramEntity() {
    
    }
    
    public ProgramEntity(String id, String name) {
      creationTime = new Date();
      updateTime = creationTime;
      setId(id);
      setName(name);
      setStatus(ProgramStatus.READY);
    }
     
    //Setters and getters
 
    @Override
    public String toString() {
        return "ProgramEntity [id=" + id + ", name=" + name + ", status=" + status + "]";
    }
    
    public String getId() {
      return (id);
    }
    
    public void setId(String id) {
      this.id = id;
    }
    
    public String getName() {
      return (name);
    }
    
    public void setName(String name) {
      this.name = name;
    }
    
    public String getHTTPResponse() {
      return (httpResponse);
    }
    
    public void setHTTPResponse(String httpResponse) {
      this.httpResponse = httpResponse;
    }
    
    public ProgramStatus getStatus() {
      return (status);
    }
    
    public void setStatus(ProgramStatus status) {
      this.status = status;
    }
    
    public Date getCreationTime() {
      return (creationTime);
    }
    
    public void setCreationTime(Date time) {
      this.creationTime = time;
    }
    
    public Date getUpdateTime() {
      return (updateTime);
    }
    
    public void setUpdateTime(Date time) {
      this.updateTime = time;
    }
    
    public void touch () {
      setUpdateTime(new Date());
    }
    
    public Date getBuildTime() {
      return (buildTime);
    }
    
    public void setBuildTime(Date time) {
      this.buildTime = time;
    }
    
    public String getOwner() {
      return (owner);
    }
    
    public void setOwner(String owner) {
      this.owner = owner;
    }
    
    public String getParentId() {
      return (parentId);
    }
    
    public void setParentId(String parentId) {
      this.parentId = parentId;
    }

    public List<BlueprintEntity> getBlueprints() {
      return (blueprints);
    }

    public void addBlueprint(BlueprintEntity b) {
      blueprints.add(b);
    }
    
	  public String getHomeDir() {
	    if (homeDir == null) {
        File f = new File(home.getDir()+"/..");
        
        try {
          homeDir = f.getCanonicalPath();
        } catch (IOException e) {
          logger.error(e.getMessage());
        }
      }
      
      return (homeDir);
	  }
    
	  public String getMyDir() {
		  //return home.getDir()+"/../data/program/"+getId();
		  //return(programDir != null ? programDir : home.getDir()+"/../data/program/"+getId());
		  return(getHomeDir()+"/data/program/"+getId());
	  }
    
  public String getJavaFile() {
	  return getMyDir()+"/Program.java";
  }
  
  @Transient
  public String getJARFilename() {
	  return getMyDir()+"/"+jarName;
  }
/*	
	public String getDefaultDepsFilename() {
		return getMyDir()+"/default-deps.json";
	}
	
	public String getDepsFilename() {
		return getMyDir()+"/deps.json";
	}*/
	
	public String getIndexFilename() {
		return getMyDir()+"/index.json";
	}
	
	@Transient
	public String getClassFilename() {
		return getMyDir()+"/"+className;
	}
	
	public String getLogDir() {
		return home.getDir()+"/../log";
	}

	public void setOutput(String s) {
		this.output = s;
	}

	public String getOutput() {
		return output;
	}

	public void setMessage(String m) {
		this.message = m;
	}

	public String getMessage() {
		return message;
	}

	public void setResult(int code, String m) {
		this.result = code;
		this.message = m;
	}
	
	public int getResult() {
		return result;
	}
	
	public void addClassPath(String cp) {
	  if (!classPathList.contains(cp))
		  classPathList.add(getHomeDir() + "/" + cp);
	}
	/*
	public void addDependency(String item) {
	  if (!dependencies.contains(item))
		  dependencies.add(item);
	}*/
	
	/*public String getClassesDir() {
		return home.getDir()+"/../classes";
	}*/
			
	public void loadDeps() {
	  String content;
    classPathList = new ArrayList<String>();
    //dependencies = new ArrayList<String>();
    JSONParser jsonParser = new JSONParser();
    JSONArray ja;
/*	  
    try {
      // Default dependencies
      
      ja = (JSONArray) jsonParser.parse(new FileReader(getDefaultDepsFilename()));

      ja.forEach (jdep -> {
        addClassPath(getHomeDir() + "/" + jdep.toString());
      });

      // Others
      ja = (JSONArray) jsonParser.parse(new FileReader(getDepsFilename()));

      ja.forEach (jdep -> {
        addClassPath(getHomeDir() + "/" + jdep.toString());
      });
    } 
    catch (FileNotFoundException e) {
      logger.warn(e.getMessage());
    }
    catch (IOException e) {
      logger.warn(e.getMessage());
    }
    catch (ParseException e) {
      logger.warn(e.getMessage());
    }*/
    
      // Default dependencies
	    addClassPath("/lib/Standard.jar"); 
	    addClassPath("/lib/java-getopt-1.0.13.jar");
	    addClassPath("/lib/log4j-1.2.12.jar");
      
      // Others
      for (BlueprintEntity b : blueprints) {
        //System.out.println("Getting dependencies from "+b.getName());
        
        for (int i=0; i<b.getJARList().size(); i++) {
          //System.out.println("  "+b.getJARList().get(i));
          addClassPath(b.getJARList().get(i));
        }
      }
	}
	
	public String getBlueprintIndex () {
	  String index = null;
	  
    try {
      index = new String (Files.readAllBytes(Paths.get(getIndexFilename())));
    }
    catch (IOException e) {
      logger.error ("Can't read index: "+e.getMessage());
    }
    
    return index;
	}
	
	public void updateIndex (BlueprintEntity blueprint, String content) {
    JSONObject jindex, jbp = null;
    JSONParser jsonParser = new JSONParser();

    // Open index
    try (FileReader reader = new FileReader(getIndexFilename())) {
        jindex = (JSONObject) jsonParser.parse(reader);
        
        if (content != null)
          jbp = (JSONObject) jsonParser.parse(content);
          
    } catch (FileNotFoundException e) {
        logger.error(e.getMessage());
        return;
    } catch (IOException e) {
        logger.error(e.getMessage());
        return;
    } catch (ParseException e) {
        logger.error(e.getMessage());
        return;
    }

    if (content != null) {
      // Crete item
      JSONObject jitem = new JSONObject();
      jitem.put("id", blueprint.getId());
      jitem.put("name", blueprint.getName());
      jitem.put("type", blueprint.getType().ordinal());
      jitem.put("input", jbp.get("input"));
      jitem.put("output", jbp.get("output"));
      
      //if (jbp.containsKey("method"))
        jitem.put("method", jbp.get("method"));
        
      jindex.put(blueprint.getId(), jitem);
    } else {
      // Delete item
      jindex.remove(blueprint.getId());
    }

    // Write JSON file
    try (FileWriter file = new FileWriter(getIndexFilename())) {
        file.write(jindex.toJSONString());
        file.flush();
    } catch (IOException e) {
        e.printStackTrace();
    }      
	}
	
	public boolean createIndex () {
    try (FileWriter file = new FileWriter(getMyDir()+"/index.json")) {
        file.write("{}");
        file.flush();
        file.close();          
    } catch (IOException e3) {
        e3.printStackTrace();
        return false;
    }
    
    return true;
	}
	/*
	public boolean createDefaultDependecies () {
	  JSONArray ja = new JSONArray();
	  
	  //ja.put(""); // classes dir.
	  ja.add("lib/Standard.jar"); 
	  ja.add("lib/java-getopt-1.0.13.jar");
	  ja.add("lib/log4j-1.2.12.jar");
	  
    try (FileWriter file = new FileWriter(getDefaultDepsFilename())) {
        file.write(ja.toString());
        file.flush();
        file.close();          
    } catch (IOException e3) {
        e3.printStackTrace();
        return false;
    }
    
    return true;
	}*/
			
	public boolean createProperties() {
    try {
      FileWriter file = new FileWriter(getMyDir()+"/Program.properties");
      file.write("# Properties of program "+getName());
      file.flush(); 
      file.close();           
    } catch (IOException e) {
      logger.error(e.getMessage());
      return false;
    }
    
    return true;
	}
	
	public Properties getProperties() {
    PropertiesManager pm = new PropertiesManager(getMyDir()+"/Program.properties");
    return (pm.getProperties());
	}
	
	public PropertiesManager getPropertiesManager() {
    PropertiesManager pm = new PropertiesManager(getMyDir()+"/Program.properties");
    return (pm);
	}
			
	public boolean generateJava() {
	  String code = "", command;//, bpFilename;
	  //JSONObject jindex = getIndex();
	  boolean result = true;

	  List<String> args = new ArrayList<String>();
    args.add("java");
    args.add("-jar");
    args.add(home.getDir() + "/bp2java");
    args.add("-P");
    args.add(getName());
    args.add("-m");
    args.add("MANIFEST.MF");
    /*args.add("--deps");
    args.add(getDepsFilename());*/
    args.add("--format");
    args.add("-O");
    args.add(getJavaFile());

	  //JSONObject jitem = null;
	  /*
    Iterator<String> keys = jindex.keys();

    while(keys.hasNext()) {
      String k = keys.next();
      //jitem = jindex.optJSONObject(k);
      
      if (k.equals("_index"))
        continue;
      
      bpFilename = getDir() + "/" + Blueprint.getFileName(k);

      args.add("-b");
      args.add(bpFilename);
    }*/
    
    for (BlueprintEntity b: blueprints) {
      args.add("-b");
      args.add(getMyDir() + "/BP_" + b.getId() + ".json");
    }
    
    ProcessBuilder processBuilder = new ProcessBuilder();
    //processBuilder.inheritIO().command(args);
    processBuilder.command(args);
    processBuilder.directory(new File(getMyDir()));
    
    try {
      Process process = processBuilder.start();

		  StringBuilder output = new StringBuilder();

		  BufferedReader outReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		  BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
		  
		  String line;
		  
		  while ((line = outReader.readLine()) != null) 
			  output.append(line + "\n");
		  
		  while ((line = errReader.readLine()) != null) 
			  output.append(line + "\n");

		  int exitVal = process.waitFor();
		  
		  if (exitVal == 0) {
		  } else {
			  logger.error ("Error generating Java code ("+exitVal+"): "+output.toString());
			  result = false;
		  }    
  
  	  setOutput(output.toString());
	  } 
	  catch (IOException e) {
		  logger.error (e.getMessage());
		  result = false;
	  } 
	  catch (InterruptedException e) {
		  logger.error (e.getMessage());
		  result = false;
	  }
	  
	  return result;
	}
	
	public boolean compile() {
	  boolean result = true;
	  
	  logger.info ("Compiling "+getName());
	  
	  clean();
	  setStatus(ProgramStatus.ERRORS);
	  
	  //System.setProperty("user.dir", getMyDir());
	  
	  if (generateJava()) {
	    String cp = ".";
	    List<String> args = new ArrayList<String>();
	  
	    //System.out.println("Loading dependencies");
	    
	    loadDeps();

      for (int i = 0; i < classPathList.size(); i++) {
        cp += ":"+classPathList.get(i);
      }
      
      System.out.println("cp = "+cp);
    	    
	    // javac -cp ".:json-simple-1.1.1.jar" Program.java
      args.add("javac");
      args.add("-parameters");
      args.add("-cp");
      args.add(cp);
      args.add(getJavaFile());

      ProcessBuilder processBuilder = new ProcessBuilder();
      //processBuilder.inheritIO().command(args);
      processBuilder.command(args);
      processBuilder.directory(new File(getMyDir()));
      
      try {
        Process process = processBuilder.start();

		    StringBuilder output = new StringBuilder();
		    BufferedReader outReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		    BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

		    String line;
		  
		    while ((line = outReader.readLine()) != null) 
			    output.append(line + "\n");
		    
		    while ((line = errReader.readLine()) != null) 
			    output.append(line + "\n");

		    int exitVal = process.waitFor();
		    
		    if (exitVal == 0) {
		      setBuildTime(new Date());
          setMessage ("Successfully compiled "+getName());
		    } else {
			    setMessage ("Compiler error ("+exitVal+"): "+output);
			    result = false;
		    }   
		    
		    setOutput(output.toString()); 
	    } 
	    catch (IOException e) {
		    setMessage (e.getMessage());
		    result = false;
	    } 
	    catch (InterruptedException e) {
		    setMessage (e.getMessage());
		    result = false;
	    }	  	    
	  }
	  else {
	    setMessage ("Can't generate Java code for program "+getName());
	    result = false;
	  }
	  
	  setStatus(result ? ProgramStatus.COMPILED : ProgramStatus.ERRORS);
	  
	  return result;
	}
	
	public boolean createManifest(String filename) {
	  String manifest;
	  
	  manifest = "Manifest-version: 1.0" + System.lineSeparator() +
               "Main-Class: Program" + System.lineSeparator();
               /*"Class-path: ";
               
    for (int i = 0; i < dependencies.size(); i++) {
      manifest += " classes/" + dependencies.get(i);
    }*/
    
    manifest += System.lineSeparator();
                     
    try {
      FileWriter file = new FileWriter(filename);
      file.write(manifest);
      file.flush();            
    } catch (IOException e) {
      logger.error(e.getMessage());
      return false;
    }
    
    logger.debug(manifest);
    
    return true;
	}

  boolean deleteDirectory(File directoryToBeDeleted) {
    File[] allContents = directoryToBeDeleted.listFiles();
    
    if (allContents != null) {
      for (File file : allContents) {
        deleteDirectory(file);
        file.delete();
      }
    }
    return directoryToBeDeleted.delete();
  }

public static void copyStream(InputStream input, OutputStream output)
     throws IOException
{
    // Reads up to 5M at a time. Try varying this.
    byte[] buffer = new byte[5*1024*1024];
    int read;

    while ((read = input.read(buffer)) != -1)
    {
        output.write(buffer, 0, read);
    }
}
	
	public boolean unpackJAR(String jarFile, String destDir) {
	  java.util.jar.JarFile jar;
	  
	  try {
      jar = new java.util.jar.JarFile(jarFile);
    } catch (IOException e) {
      logger.error(e.getMessage());
      return false;
    }
    
    java.util.Enumeration items = jar.entries();
    
    while (items.hasMoreElements()) {
	    java.util.jar.JarEntry file = (java.util.jar.JarEntry) items.nextElement();

      String destFilePath = destDir + java.io.File.separator + file.getName();
	    java.io.File f = new java.io.File(destFilePath);
	    
	    if (file.isDirectory()) { // if its a directory, create it
		    f.mkdir();
		    continue;
	    }
	    
	    java.io.InputStream is = null;
	    java.io.OutputStream os = null;
	    //java.io.FileOutputStream fos = null;
	    
	    try {
	      is = jar.getInputStream(file); // get the input stream
        os = Files.newOutputStream(Paths.get(destFilePath));
        /*
	      fos = new java.io.FileOutputStream(f);
	      
	      while (is.available() > 0) {  // write contents of 'is' to 'fos'
		      fos.write(is.read());
	      }
	      
	      fos.close();
        */

        copyStream(is, os);

        os.close();
	      is.close();
	    
      } catch (IOException e) {
        logger.error(e.getMessage());
      } finally {
	    }
    }
    
    return true;
	}
	
	public boolean hasJAR() {
    File f = new File(getJARFilename()); 
    return(f.exists());
	}
			
	public boolean createJAR() {
	  boolean result = true;
	  String mf = getMyDir()+"/MANIFEST.MF";
	  
	  //System.setProperty("user.dir", getMyDir());
	  
	  logger.info("Creating JAR for "+getName());
	  logger.debug("Current direcotry: "+System.getProperty("user.dir"));	  

    loadDeps();
    
    if (!createManifest(mf)) {
      logger.error("Can't create manifest file");
      return false;
    }
  
    List<String> args = new ArrayList<String>();
    
    // jar -cvfm Hello.jar MANIFEST.MF Program.class
    args.add("jar");
    args.add("-cvfm");
    args.add(getJARFilename());
    args.add(mf);
    args.add("../"+className/*getClassFilename()*/); // We are in temp directory
    
    // Create temporary directory
    
    String tempDir = getMyDir()+"/temp";
    File tempDirFile = new File(tempDir);
    tempDirFile.mkdir();
    
    /*if (!tempDirFile.mkdir()) {
      logger.error("Can't create "+tempDir);
      return false;
    }*/
               
    
    // Unpack dependencies in temp directory

    for (int i = 0; i < classPathList.size(); i++) {
      //args.add(classPathList.get(i));
      logger.info("Unpacking "+classPathList.get(i)+" ...");
      unpackJAR(classPathList.get(i), tempDir);
    }
    
    // Add directories to jar command line
   
    File[] allContents = tempDirFile.listFiles();
    
    if (allContents != null) {
      for (File file : allContents) {
        Path path = Paths.get(file.toString()); 
        
        //System.out.println(path.getFileName());
        
        if (!path.getFileName().toString().equals("META-INF"))
          args.add(path.getFileName().toString());
      }
    }
    
    //System.out.println(args.toString());
    
    // Run jar
    
    logger.info("Creating "+getJARFilename());

    ProcessBuilder processBuilder = new ProcessBuilder();
    //processBuilder.inheritIO().command(args);
    processBuilder.command(args);
    
    // Move in the directory with all dependencies
    processBuilder.directory(new File(tempDir));
    
    try {
      Process process = processBuilder.start();

	    StringBuilder output = new StringBuilder();
	    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

	    String line;
	    while ((line = reader.readLine()) != null) {
		    output.append(line + "\n");
	    }

	    int exitVal = process.waitFor();
	    
	    if (exitVal == 0) {
        setMessage ("JAR successfully created for "+getName());
		    //System.out.println(output);
	    } else {
		    setMessage ("Error creating JAR ("+exitVal+"): "+output);
		    result = false;
	    }    
    } 
    catch (IOException e) {
	    setMessage (e.getMessage());
	    result = false;
    } 
    catch (InterruptedException e) {
	    setMessage (e.getMessage());
	    result = false;
    } finally {
      deleteDirectory(tempDirFile);
    }
  
	  return result;
	}
	
	public String getJava() {
	  String javaCode = null;

    try {
      javaCode = new String (Files.readAllBytes(Paths.get(getJavaFile())));
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    
	  return javaCode;
	}
	
	public void clean() {
	  File f;
	  
	  f = new File(getJavaFile());
	  f.delete();
	  
	  f = new File(getJARFilename());
	  f.delete();
	  
	  f = new File(getClassFilename());
	  f.delete();
	  
	  f = new File(getMyDir()+"/MANIFEST.MF");
	  f.delete();
	}
	
	Object[] getParams(Method m, JSONObject jdata) {
	  Object[] args = null;
	  int len = jdata.size();
	  System.out.println("Input length: "+len);
	  
    Parameter[] parameters = m.getParameters();
    
    System.out.println("Method "+m.getName()+" has "+parameters.length+" parameters");
    
    if (parameters.length > 0) {
	    args = new Object[parameters.length];
	    
	    Class types[] = m.getParameterTypes();
	    
	    /*for (int j = 0; j < types.length; j++)
        System.out.println("param #" + j + " " + types[j]);*/
        
      for (int j = 0; j < parameters.length; j++) {
        Parameter p = parameters[j];
        //System.out.println(p.toString());
        System.out.println(p.getName()+" "+types[j]+" Type:"+types[j].getName()+" isArray:"+types[j].isArray()+" Modifiers: "+p.getModifiers()+" "+Modifier.toString(p.getModifiers()));
        
        if (types[j].isArray()) {
          JSONArray ja = (JSONArray) jdata.get(p.getName());
          
          Object[] objArray = null; 
  
          if (types[j].getName().equals("[Ljava.lang.String;"))
            objArray = new String[ja.size()];
          else if (types[j].getName().equals("[Ljava.lang.Integer;"))
            objArray = new Integer[ja.size()];
          else if (types[j].getName().equals("[Ljava.lang.Boolean;"))
            objArray = new Boolean[ja.size()];
          else if (types[j].getName().equals("[Ljava.lang.Double;"))
            objArray = new Double[ja.size()];
          
          for (int k=0; k<ja.size(); k++) {            
            if (types[j].getName().equals("[Ljava.lang.String;"))
              objArray[k] = (String) ja.get(k);
            else if (types[j].getName().equals("[Ljava.lang.Integer;"))
              objArray[k] = (Integer) ja.get(k);
            else if (types[j].getName().equals("[Ljava.lang.Boolean;"))
              objArray[k] = (Boolean) ja.get(k);
            else if (types[j].getName().equals("[Ljava.lang.Double;"))
              objArray[k] = (Double) ja.get(k);
          }

          args[j] = objArray;
        } else {
          System.out.println("Getting parameter "+p.getName());
          Object obj = jdata.get(p.getName());
          
          if (obj instanceof String)
            args[j] = (String) obj;
          else if (obj instanceof Integer)
            args[j] = (Integer) obj;
          else if (obj instanceof Boolean)
            args[j] = (Boolean) obj;
          else if (obj instanceof Double)
            args[j] = (Double) obj;
            
          System.out.println("args[ "+j+"] = "+args[j]);
        }
      }
	  }
          
	  return args;
	}
	
	public boolean run(String methodName, String data) {
	  return (run(methodName, data, getName(), null));
	}
	
	public boolean run(String methodName, String data, String logName, HttpServletRequest request) {
	  //System.setProperty("user.dir", getClassesDir());
	  setResult(ProgramEntity.SUCCESS, "OK");
	  
	  //System.out.println("data = "+data);
    
    List<URL> urls = new ArrayList<>();
	  //System.setProperty("java.class.path", System.getProperty("java.class.path")+cp);

    // Create a File object on the root of the directory containing the class file
    //System.out.println("Loading class "+ getClassFilename() +"...");

    File file = new File(getMyDir());

    try {
	    //System.out.println("Loading dependencies");
	    
	    loadDeps();

      for (int i = 0; i < classPathList.size(); i++) {
        System.out.println("Adding classpath "+ classPathList.get(i));
        File f = new File(classPathList.get(i));
        urls.add(f.toURI().toURL());
      }
      
       urls.add(file.toURI().toURL());
     
       URL[] clUrls = new URL[urls.size()];
       clUrls = urls.toArray(clUrls);
    
        // Convert File to a URL
        //URL url = file.toURI().toURL();          // file:/c:/myclasses/
        //URL[] urls = new URL[]{url};

        // Create a new class loader with the directory
        ClassLoader cl = new URLClassLoader(/*urls*/clUrls);
        
        
        /*Class clsExitException = cl.loadClass("ExitException");
        System.out.println("Loaded class : " + clsExitException.getName());*/

        // Load in the class; MyClass.class should be located in
        // the directory file:/c:/myclasses/com/mycompany
        Class BPContext = cl.loadClass("com.lionsoft.standard.BPContext");
        //System.out.println("Loaded class : " + BPContext.getName());
        
        Constructor c1 = BPContext.getConstructor();
        Object context = c1.newInstance();
      
        // Get context methods
        //Method setRequest = BPContext.getDeclaredMethod("setRequest", HttpServletRequest.class);
        Method setParameters = BPContext.getDeclaredMethod("setParameters", Map.class);
        Method getResponse = BPContext.getDeclaredMethod("getResponse");
        Method setLogPath = BPContext.getMethod("setLogPath", String.class);
        Method setLogName = BPContext.getMethod("setLogName", String.class);
        Method setGlobalProperties = BPContext.getMethod("setGlobalProperties", Properties.class);
        Method setProgramProperties = BPContext.getMethod("setProgramProperties", String.class, Properties.class);
        
        // Set
        //setRequest.invoke(context, request);
        setParameters.invoke(context, request != null ? request.getParameterMap() : null);
        setLogPath.invoke(context, getLogDir());
        setLogName.invoke(context, logName);
        
        // Set global properties
        GlobalProperties gProp = GlobalProperties.getInstance();
        setGlobalProperties.invoke(context, gProp.getProperties());
        
        // Set program properties
        setProgramProperties.invoke(context, getMyDir()+"/Program.properties", this.getProperties());
        
        Class cls = cl.loadClass("Program");
        //Class cls = Class.forName("Program", false, cl);
        
        //System.out.println("Loaded class : " + cls.getName());
         
        // Create a new instance from the loaded class
        Constructor constructor = cls.getConstructor(BPContext);
        Object programInstance = constructor.newInstance(context);
        
        // Search method
        Method method = null;
        
        Method[] methods = cls.getMethods();
        for (Method m : methods) {
            //System.out.println(m.toString());
            
            if (m.getName().equals(methodName))
              method = m;
        }
        
        //System.out.println(method != null ? "Found " + methodName : "Not found " + methodName);
        
        if (method != null) {
          Object[] params = null;
        
          
          if (data != null) {
            JSONParser jsonParser = new JSONParser();
            JSONObject jdata;
            
            try {
              //jdata = new JSONObject(data);
              jdata = (JSONObject) jsonParser.parse(data);
              params = getParams(method, jdata);
            } catch (ParseException e) {
              setResult(ProgramEntity.BAD_INPUT, e.getMessage());
              return false;
            }
          }
          
          Method _getCode = cls.getMethod("_getCode");
          Method _log = cls.getMethod("_log", String.class);
          Method _error = cls.getMethod("_error", String.class);

          _log.invoke(programInstance, "Start thread id "+Thread.currentThread().getId());

          // Redirect output and error
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          PrintStream ps = new PrintStream(baos);
          
          PrintStream oldOut = System.out; // Save the old System.out
          PrintStream oldErr = System.err; // Save the old System.err
          
          System.setOut(ps);
          System.setErr(ps);
          

          //Method method = cls.getMethod(methodName);
          //System.out.println("Invoked method name: " + method.getName());
          /*final Object[] args = new Object[1];
          args[0] = new String[] { "1", "2"};
          method.invoke(null, args);*/
          
          Object code = null;
          
          try {
            if (params != null)
              method.invoke(programInstance, params);
            else
              method.invoke(programInstance);
              
            setHTTPResponse((String) getResponse.invoke(context));
            
          } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            logger.error("InvocationTargetException: " +cause.getMessage());
            
            code = _getCode.invoke(programInstance);
            code = ((int)code) != 0 ? code : EXCEPTION;
            _error.invoke(programInstance, cause.getMessage());
            setResult((int)code, cause.getMessage());
          }

          System.out.flush();
          System.setErr(oldErr);
          System.setOut(oldOut);
          
          output = baos.toString();
            
          _log.invoke(programInstance, "End thread id "+Thread.currentThread().getId());
       
          // Exit code
          if (code != null) {
            result = (int)code;
            
            if (result == 0)
              logger.info("Successfully executed.");
            else
              logger.error("Execution terminated with errors");
          }
          
        } else {
          setResult(ProgramEntity.METHOD_NOT_FOUND, "Method not found: "+methodName);
          return false;
        }
          
        
    } catch (MalformedURLException e) {
      setResult(ProgramEntity.EXCEPTION, e.getMessage());
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      setResult(ProgramEntity.EXCEPTION, e.getMessage());
      e.printStackTrace();
    } catch (Exception e) {
      setResult(ProgramEntity.EXCEPTION, e.getMessage());
      e.printStackTrace();
    }

	  return result == 0;
	}
}
