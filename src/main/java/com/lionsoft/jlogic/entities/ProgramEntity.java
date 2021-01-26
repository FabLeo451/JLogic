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

import javax.persistence.JoinColumn;
import javax.persistence.ElementCollection;
import javax.persistence.CollectionTable;

import java.io.*;
import java.util.*;
/*import java.util.Date;
import java.util.List;
import java.util.Properties;*/

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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;

import javax.servlet.http.HttpServletRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;


@Entity
@Table(name="PROGRAM")
public class ProgramEntity {

    public final String tag = "PROGRAM";

    // Run result
    public static final int SUCCESS = 0;
    public static final int METHOD_NOT_FOUND = -1;
    public static final int BAD_INPUT = -2;
    public static final int EXCEPTION = -99;

    @JsonIgnore
    public final String jarName = "Program.jar";

    @JsonIgnore
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

    @Column(name="version")
    private String version;

    @Column(name="group_id")
    private String groupId;

    @Column(name="artifact_id")
    private String artifactId;

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

    @JsonIgnore
    @Column(name="parent_id")
    private String parentId;

    @OneToMany(cascade = CascadeType.ALL,
               fetch = FetchType.LAZY,
               mappedBy = "program")
    private List<BlueprintEntity> blueprints = new ArrayList<BlueprintEntity>();

    @OneToMany(mappedBy="program", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Variable> variables = Collections.emptyList();
/*
    @JsonIgnore
    @Transient
    private List<String> jarList;

    @JsonIgnore
    @Transient
    private List<String> classPathList;*/

    //@Transient
    //private List<String> dependencies;
    /*
    @ElementCollection
    @CollectionTable(name = "packages", joinColumns = @JoinColumn(name = "program_id"))
    @Column(name="package")
    List<Plugin> dependencies = new ArrayList<Plugin>();*/
    @OneToMany(mappedBy="program", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Plugin> dependencies = Collections.emptyList();

    @JsonIgnore
    public List<Plugin> getDependencies() { return dependencies; }

    @JsonIgnore
    @Transient
	  private int result;

    @JsonIgnore
    @Transient
	  private String message;

    @JsonIgnore
    @Transient
	  private String output;

    @JsonIgnore
    @Transient
	  private String httpResponse;

    @JsonIgnore
    @Transient
	  private int httpStatus;

    @JsonIgnore
    @Transient
	  private String homeDir = null;

    /*@Transient
	  private String programDir = null;*/

    public ProgramEntity() {

    }

    public ProgramEntity(String id, String name) {
        creationTime = new Date();
        updateTime = creationTime;
        version = "1.0.0";
        groupId = "org.jlogic";
        artifactId = "program";
        setId(id);
        setName(name);
        setStatus(ProgramStatus.READY);
    }

    //Setters and getters

    public void setVariables(List<Variable> vars) {
        this.variables = vars;
    }

    public List<Variable> getVariables() {
      return variables;
    }

    @Override
    public String toString() {
        return "ProgramEntity [id=" + id + ", name=" + name + ", version=" + version + ", status=" + status + "]";
    }

    public String getTag() { return (tag); }

    public String getId() { return (id); }
    public void setId(String id) { this.id = id; }

    public String getName() { return (name); }
    public void setName(String name) { this.name = name; }

    public String getVersion() { return (version); }
    public void setVersion(String version) { this.version = version; }

    public String getGroupId() { return (groupId); }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getArtifactId() { return (artifactId); }
    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }

    public String getPackage() { return (groupId+"."+artifactId); }
    public String getMainClass() { return (getPackage()+".Program"); }

    @JsonIgnore
    public String getSrcDir() {
        return ("/src/main/java/"+getGroupId().replace(".", "/")+"/"+getArtifactId());
    }

    @JsonIgnore
    public String getResourcesDir() {
        return ("/src/main/resources");
    }

    @JsonIgnore
    public String getPropertiesFile() {
        return (getMyDir()+"/"+getResourcesDir()+"/application.propertes");
    }

    @JsonIgnore
    public String getClasspathFile() { return (getMyDir()+"/cp.txt"); }
    @JsonIgnore
    public String getClassesDir() { return (getMyDir()+"/target/classes"); }

    @JsonIgnore
    public int getHTTPStatus() {
      return (httpStatus);
    }

    @JsonIgnore
    public String getHTTPResponse() {
      return (httpResponse);
    }

    public void setHTTPResponse(String httpResponse) {
      this.httpResponse = httpResponse;
    }

    public void setHTTPResponse(int status, String httpResponse) {
      this.httpStatus = status;
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
/*
    public void addBlueprint(BlueprintEntity b) {
      blueprints.add(b);
    }*/

    @JsonIgnore
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

    @JsonIgnore
    public String getMyDir() {
        //return home.getDir()+"/../data/program/"+getId();
        //return(programDir != null ? programDir : home.getDir()+"/../data/program/"+getId());
        return(getHomeDir()+"/data/program/"+getId());
    }

    @JsonIgnore
    public boolean hasDependency(Plugin p) {
        for (int i=0; i<dependencies.size(); i++) {
            if (p.equals(dependencies.get(i)))
                return true;
        }

        return false;
    }

    public void addDependency(Plugin p) {
        if (!hasDependency(p)) {
            dependencies.add(p);
            p.setProgram(this);
        }
    }

    @JsonIgnore
    public String getJavaFile() {
        return getMyDir()+"/"+getSrcDir()+"/Program.java";
    }

    @JsonIgnore
    @Transient
    public String getJARFilename() {
        // program-1.0.0-jar-with-dependencies
        return getMyDir()+"/target/"+artifactId+"-"+version+"-jar-with-dependencies.jar";
    }
/*
	public String getDefaultDepsFilename() {
		return getMyDir()+"/default-deps.json";
	}

	public String getDepsFilename() {
		return getMyDir()+"/deps.json";
	}*/

    @JsonIgnore
    public String getIndexFilename() {
    	return getMyDir()+"/index.json";
    }

    @JsonIgnore
    @Transient
    public String getClassFilename() {
    	return getClassesDir()+"/"+className;
    }

    @JsonIgnore
    public String getLogDir() {
    	return home.getDir()+"/../log";
    }

	public void setOutput(String s) {
		this.output = s;
	}

	public void appendOutput(String s) {
		this.output += s;
	}

  @JsonIgnore
	public String getOutput() {
		return output;
	}

	public void setMessage(String m) {
		this.message = m;
	}

  @JsonIgnore
	public String getMessage() {
		return message;
	}

	public void setResult(int code, String m) {
		this.result = code;
		this.message = m;
	}

  @JsonIgnore
	public int getResult() {
		return result;
	}

	public Variable getVariable(UUID id) {
	  for (Variable v: variables)
	    if (v.getId().equals(id))
	      return v;

	  return null;
	}

    @JsonIgnore
	public Variable getVariable(String name) {
	  for (Variable v: variables)
	    if (v.getName().equals(name))
	      return v;

	  return null;
	}

	public boolean hasVariable(String name) {
	  return (getVariable(name) != null);
	}

	public Variable addVariable(Variable v) {
	  if (!v.isValid())
	    return null;

	  v.setProgram(this);
	  variables.add(v);
	  return v;
	}

	public boolean deleteVariable(Variable v) {
	  return (variables.remove(v));
	}

	public boolean deleteVariable(String name) {
	  Variable v = getVariable(name);
	  return (deleteVariable(v));
	}

	public boolean variableIsReferenced(Variable v, BlueprintEntity bpExclude) {
    List<BlueprintEntity> bl = getBlueprints();

    for (BlueprintEntity b: bl) {
      if (bpExclude != null && (b.getId() == bpExclude.getId()))
        continue;

      if (b.referencesVariable(v))
        return true;
    }

	  return (false);
	}

	public boolean variableIsReferenced(Variable v) {
	  /*
    List<BlueprintEntity> bl = getBlueprints();

    for (BlueprintEntity b: bl) {
      if (b.referencesVariable(v))
        return true;
    }

	  return (false);
	  */
	  return(variableIsReferenced(v, null));
	}

	public BlueprintEntity getBlueprintByInternalId(int id) {
    List<BlueprintEntity> bl = getBlueprints();

    if (bl == null || bl.size() == 0)
      return (null);

    //System.out.println("bl.size() = "+bl.size());

    for (BlueprintEntity b: bl) {
      //System.out.println(b);
      if (b != null && b.getInternalId() == id)
        return b;
    }

	  return (null);
	}

  @JsonIgnore
  public int getNewInternalId() {
    int newId = 10;

    while (getBlueprintByInternalId(newId) != null)
      newId ++;

    return newId;
  }
/*
	public void addClassPath(String cp) {
	  if (!classPathList.contains(cp))
		  classPathList.add(getHomeDir() + "/" + cp);
	}

	public void addJAR(String jar) {
	  if (!jarList.contains(jar))
		  jarList.add(getHomeDir() + "/" + jar);
	}*/
	/*
	public void addDependency(String item) {
	  if (!dependencies.contains(item))
		  dependencies.add(item);
	}*/

	/*public String getClassesDir() {
		return home.getDir()+"/../classes";
	}*/
/*
    public void loadDeps() {
        String content;
        classPathList = new ArrayList<String>();
        \/*
        jarList = new ArrayList<String>();
        JSONParser jsonParser = new JSONParser();
        JSONArray ja;

        classPathList.add(getMyDir());

        // Default dependencies
        addClassPath("lib/Standard.jar");
        addClassPath("lib/java-getopt-1.0.13.jar");
        addClassPath("lib/log4j-1.2.12.jar");

        addJAR("lib/Standard.jar");
        addJAR("lib/java-getopt-1.0.13.jar");
        addJAR("lib/log4j-1.2.12.jar");

        // Others
        for (BlueprintEntity b : blueprints) {
            //System.out.println("Getting dependencies from "+b.getName());

            // JARs will be added to JAR list and class path list
            for (int i=0; i<b.getJARList().size(); i++) {
                //System.out.println("  "+b.getJARList().get(i));
                addJAR(b.getJARList().get(i));
                addClassPath(b.getJARList().get(i));
            }

            // Class paths will be added to class path list only
            for (int i=0; i<b.getClassPath().size(); i++) {
                //System.out.println("  "+b.getJARList().get(i));
                addClassPath(b.getClassPath().get(i));
            }
        }
        *\/
        String cp = Utils.loadTextFile(getClasspathFile());

        String[] deps = cp.split(":");

        for (int i=0; i<deps.length; i++) {
            if (!classPathList.contains(cp))
      		  classPathList.add(deps[i]);
        }
    }
*/
/*
  @JsonIgnore
	public String getBlueprintIndex () {
	  String index = null;

    try {
      index = new String (Files.readAllBytes(Paths.get(getIndexFilename())));
    }
    catch (IOException e) {
      logger.error ("Can't read index: "+e.getMessage());
    }

    return index;
	}*/
/*
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
      jitem.put("type", blueprint.getType().name());
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
	} */
/*
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
	}*/
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
      FileWriter file = new FileWriter(getPropertiesFile());
      file.write("# Properties of program "+getName());
      file.flush();
      file.close();
    } catch (IOException e) {
      logger.error(e.getMessage());
      return false;
    }

    return true;
	}

  @JsonIgnore
	public Properties getProperties() {
    PropertiesManager pm = new PropertiesManager(getPropertiesFile());
    return (pm.getProperties());
	}

  @JsonIgnore
	public PropertiesManager getPropertiesManager() {
    PropertiesManager pm = new PropertiesManager(getPropertiesFile());
    return (pm);
	}

	public Result generateJava() {
        List<String> args = new ArrayList<String>();
        args.add("java");
        args.add("-jar");
        args.add(home.getDir() + "/bp2java");
        args.add("-P");
        args.add(getName());
        /*args.add("--root");
        args.add(getHomeDir());*/
        /*args.add("-m");
        args.add("MANIFEST.MF");*/
        /*args.add("--deps");
        args.add(getDepsFilename());*/
        args.add("--format");
        args.add("-O");
        args.add(getJavaFile());

        for (BlueprintEntity b: blueprints) {
            args.add("-b");
            args.add(getMyDir() + "/BP_" + b.getId() + ".json");
        }

        return(Utils.execute(args, getMyDir()));
	}
/*
	public boolean compile() {
	  boolean result = true;

	  logger.info ("Compiling "+getName());

	  clean();
	  setOutput("");
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
          setUpdateTime(new Date());
          setMessage ("Successfully compiled "+getName());
		    } else {
			    setMessage ("Compiler error ("+exitVal+"): "+output);
			    result = false;
		    }

		    appendOutput(output.toString());
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
*/

/*
	public boolean createManifest(String filename) {
	  String manifest;

	  manifest = "Manifest-version: 1.0" + System.lineSeparator() +
               "Main-Class: Program" + System.lineSeparator() +
               "Class-path: ";

    for (int i = 0; i < classPathList.size(); i++) {
      manifest += " "+classPathList.get(i);
    }

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
*/

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



  @JsonIgnore
	public boolean getJar() {
        File f = new File(getJARFilename());
        return(f.exists());
	}
/*
	public boolean createJAR() {
	  boolean result = true;
	  String mf = getMyDir()+"/MANIFEST.MF";

	  //System.setProperty("user.dir", getMyDir());

	  logger.info("Creating JAR for "+getName());
	  //logger.debug("Current direcotry: "+System.getProperty("user.dir"));

    /////////////////loadDeps();

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
    //args.add("../"+className/*getClassFilename()*\/); // We are in temp directory

    // Add all .class files (Program.class and nested classes Program$x)

    String[] pathnames;
    File f = new File(getMyDir());
    pathnames = f.list();

    for (String pathname : pathnames) {
      if (pathname.endsWith(".class")) {
        System.out.println("Adding "+getMyDir()+"/"+pathname);
        args.add("../"+pathname);
      }
    }

    // Create temporary directory

    String tempDir = getMyDir()+"/temp";
    File tempDirFile = new File(tempDir);
    tempDirFile.mkdir();

    \/*if (!tempDirFile.mkdir()) {
      logger.error("Can't create "+tempDir);
      return false;
  }*\/


    // Unpack dependencies in temp directory

    for (int i = 0; i < jarList.size(); i++) {
      logger.info("Unpacking "+jarList.get(i)+" ...");
      unpackJAR(jarList.get(i), tempDir);
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

    System.out.println(args.toString());

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
*/
  @JsonIgnore
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

    // Delete all .class files
    String[] pathnames;
    f = new File(getMyDir());
    pathnames = f.list();

    for (String pathname : pathnames) {
      if (pathname.endsWith(".class")) {
        File c = new File(getMyDir()+"/"+pathname);
    	  c.delete();
      }
    }

	  f = new File(getMyDir()+"/MANIFEST.MF");
	  f.delete();
	}
/*
	Object[] getParams(Method m, JSONObject jdata) {
	  Object[] args = null;
	  int len = jdata.size();
	  System.out.println("Input length: "+len);

    Parameter[] parameters = m.getParameters();

    System.out.println("Method "+m.getName()+" has "+parameters.length+" parameters");

    if (parameters.length > 0) {
	    args = new Object[parameters.length];

	    Class types[] = m.getParameterTypes();

	    \/*for (int j = 0; j < types.length; j++)
        System.out.println("param #" + j + " " + types[j]);*\/

      // Scan method parameters
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

          System.out.println("args["+j+"] = "+args[j]);
        }
      }
	  }

	  return args;
	}
*/

@JsonIgnore
	Object[] getParams(Method m, Map<String, Object> actualParams) {
	  Object[] args = null;
	  int len = actualParams.size();
	  logger.info("Input actual parameters: "+len);

    Parameter[] parameters = m.getParameters();

    logger.info("Method "+m.getName()+" has "+parameters.length+" parameters");

    if (parameters.length > 0) {
	    args = new Object[parameters.length];

	    Class types[] = m.getParameterTypes();

	    /*for (int j = 0; j < types.length; j++)
        System.out.println("param #" + j + " " + types[j]);*/

      // Scan method parameters
      for (int j = 0; j < parameters.length; j++) {
        Parameter p = parameters[j];
        //System.out.println(p.toString());
        //System.out.println(p.getName()+" "+types[j]+" Type:"+types[j].getName()+" isArray:"+types[j].isArray()+" Modifiers: "+p.getModifiers()+" "+Modifier.toString(p.getModifiers()));

        if (types[j].isArray()) {
          Object[] oa = (Object[]) actualParams.get(p.getName());

          if (oa == null) {
            logger.warn("Input not found for parameter "+p.getName());
            continue;
          }

          int size = oa.length;
          //System.out.println("Array of "+size);

          Object[] objArray = null;

          if (types[j].getName().equals("[Ljava.lang.String;"))
            objArray = new String[size];
          else if (types[j].getName().equals("[Ljava.lang.Integer;"))
            objArray = new Integer[size];
          else if (types[j].getName().equals("[Ljava.lang.Boolean;"))
            objArray = new Boolean[size];
          else if (types[j].getName().equals("[Ljava.lang.Double;"))
            objArray = new Double[size];

          for (int k=0; k<size; k++) {
            if (types[j].getName().equals("[Ljava.lang.String;"))
              objArray[k] = (String) oa[k];
            else if (types[j].getName().equals("[Ljava.lang.Integer;"))
              objArray[k] = (Integer) ((Long) oa[k]).intValue();
            else if (types[j].getName().equals("[Ljava.lang.Boolean;"))
              objArray[k] = (Boolean) oa[k];
            else if (types[j].getName().equals("[Ljava.lang.Double;"))
              objArray[k] = (Double) oa[k];
          }

          args[j] = objArray;
        } else {
          if (types[j].getName().equals("java.lang.Integer"))
            args[j] = ((Long) actualParams.get(p.getName())).intValue();
          else
            args[j] = actualParams.get(p.getName());

        }

        System.out.println("args["+j+"] = "+args[j]);
      }
	  }

	  return args;
	}
//////////////////////////////////////////////////////////////////

/*
  private static <E extends Enum> E[] getEnumValues(Class<E> enumClass)
          throws NoSuchFieldException, IllegalAccessException {
      Field f = enumClass.getDeclaredField("$VALUES");
      //System.out.println(f);
      //System.out.println(Modifier.toString(f.getModifiers()));
      f.setAccessible(true);
      Object o = f.get(null);
      return (E[]) o;
  }
*/

    /**
    * Returns an array of dependency urls
    */
    /*
    @Transient
    @JsonIgnore
    public URL[] getURLs() {
        List<URL> urls = new ArrayList<>();
        URL[] clUrls = null;

        try {
            String cp = Utils.loadTextFile(getClasspathFile());

            String[] deps = cp.split(":");

            for (int i=0; i<deps.length; i++) {
                urls.add(new File(deps[i]).toURI().toURL());
            }

            File file = new File(getClassesDir());
            urls.add(file.toURI().toURL());

            clUrls = new URL[urls.size()];
            clUrls = urls.toArray(clUrls);
        } catch (MalformedURLException e) {
            logger.error("Malformed URL: "+e.getMessage());
            return null;
        }

        return clUrls;
    }*/

    /**
    * Returns an array of dependency urls
    */
    @Transient
    @JsonIgnore
    public URL[] getURLs() {
        List<URL> urls = Utils.getURLs(getClasspathFile());

        try {
            urls.add(new File(getClassesDir()).toURI().toURL());
            System.out.println("Added classes dir "+new File(getClassesDir()).toURI().toURL().toString());
        } catch (MalformedURLException e) {
            logger.error("Malformed URL: "+e.getMessage());
            return null;
        }

        URL[] clUrls = new URL[urls.size()];
        return(urls.toArray(clUrls));
    }

	public boolean run(String methodName, /*String data*/Map<String, Object> actualParams) {
	  return (run(methodName, actualParams, getName(), null));
	}

	public boolean run(String methodName, /*String data*/Map<String, Object> actualParams, String logName, HttpServletRequest request) {
	  //System.setProperty("user.dir", getClassesDir());
	  setResult(ProgramEntity.SUCCESS, "OK");

	  //System.out.println("data = "+data);

      logger.info("Collecting URLs...");
    //List<URL> urls = new ArrayList<>();
    URL[] clUrls = getURLs();

    try {
        //URL[] clUrls = getURLs();


        // Create a new class loader with the directory
        URLClassLoader cl = new URLClassLoader(/*urls*/clUrls);

        // Load in the class; MyClass.class should be located in
        // the directory file:/c:/myclasses/com/mycompany
        Class BPContext = cl.loadClass("com.lionsoft.jlogic.standard.BPContext");

        Constructor c1 = BPContext.getConstructor();
        Object context = c1.newInstance();

        // Get context methods
        //Method setRequest = BPContext.getDeclaredMethod("setRequest", HttpServletRequest.class);
        Method setParameters = BPContext.getDeclaredMethod("setParameters", Map.class);
        Method getStatus = BPContext.getDeclaredMethod("getStatus");
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

        //Class cls = cl.loadClass("Program");
        Class cls = Class.forName(getMainClass(), false, cl);

        //System.out.println("Loaded class : " + cls.getName());

        // Create a new instance from the loaded class
        Constructor constructor = cls.getConstructor(BPContext);
        Object programInstance = constructor.newInstance(context);

        Class EventType = cl.loadClass("com.lionsoft.jlogic.standard.EventType");

        Field f = EventType.getDeclaredField("BEGIN");
        f.setAccessible(true);
        Object eventBegin = f.get(null);

        f = EventType.getDeclaredField("EXCEPTION");
        f.setAccessible(true);
        Object eventException = f.get(null);

        f = EventType.getDeclaredField("END");
        f.setAccessible(true);
        Object eventEnd = f.get(null);

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

          if (actualParams != null)
            params = getParams(method, actualParams);

          Method _getCode = cls.getMethod("_getCode");
          Method _log = cls.getMethod("_log", String.class);
          Method _error = cls.getMethod("_error", String.class);
          Method _event = cls.getMethod("_event", EventType, String.class);

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
            _event.invoke(programInstance, eventBegin, null);

            if (params != null)
              method.invoke(programInstance, params);
            else
              method.invoke(programInstance);

            setHTTPResponse((int) getStatus.invoke(context), (String) getResponse.invoke(context));

          } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            //logger.error("InvocationTargetException: " +cause.getMessage());

            _event.invoke(programInstance, eventException, cause.getMessage());

            code = _getCode.invoke(programInstance);
            code = ((int)code) != 0 ? code : EXCEPTION;
            _error.invoke(programInstance, cause.getMessage());
            setResult((int)code, cause.getMessage());
          }
          finally {
            _event.invoke(programInstance, eventEnd, null);

            System.out.flush();
            System.err.flush();
            System.setErr(oldErr);
            System.setOut(oldOut);
          }

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


    } /*catch (MalformedURLException e) {
      setResult(ProgramEntity.EXCEPTION, e.getMessage());
      e.printStackTrace();
  }*/ catch (ClassNotFoundException e) {
      setResult(ProgramEntity.EXCEPTION, e.getMessage());
      e.printStackTrace();
    } catch (Exception e) {
      setResult(ProgramEntity.EXCEPTION, e.getMessage());
      e.printStackTrace();
    }

	  return result == 0;
	}
}
