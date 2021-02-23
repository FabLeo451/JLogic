package com.lionsoft.jlogic;

import org.springframework.boot.system.ApplicationHome;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import java.time.Instant;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;

import java.util.concurrent.locks.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.InvocationTargetException;
import java.lang.annotation.Annotation;

import java.net.URL;
import java.net.MalformedURLException;
import java.beans.IntrospectionException;
import java.net.URLClassLoader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import javax.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import java.security.Principal;

import java.lang.Thread;
import java.util.Properties;
import java.util.UUID;
import java.util.Optional;

import java.util.zip.ZipOutputStream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.InvocationResult;

//import org.apache.maven.cli.MavenCli;

@Service
public class ProgramService {

    final static int SUCCESS = 0;
    final static int ERROR = 1;

    final static int COMPILE = 1;
    final static int JAR = 2;

    @Autowired
    ProgramRepository repository;

    @Autowired
    BlueprintService blueprintService;

    @Autowired
    SessionService sessionService;

    @Autowired
    BuildProperties buildProperties;

    Logger logger = LoggerFactory.getLogger(ProgramService.class);
    ApplicationHome home = new ApplicationHome(ProgramService.class);

    int code = 0;
    String message;

    @Value("${maven.home}")
    String MAVEN_HOME;

	public ProgramService() {
	}

	public List<ProgramEntity> findAll() {
        return (repository.findAll());
	}

    public int getCode() {
        return code;
    }

    public void setCode(int c) {
        code = c;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String m) {
        message = m;
    }

    public void setResult(int c, String m) {
        setCode(c);
        setMessage(m);
    }

	public String getProgramBaseDirectory() {
		return home.getDir()+"/../data/program";
	}

	public String getTempDirectory() {
		return home.getDir()+"/../temp";
	}

	public void refresh(ProgramEntity program) {
		repository.refresh(program);
	}

    public boolean compiled(ProgramEntity program) {
        File f = new File(program.getClassFilename());
        return(f.exists());
	}

    public boolean createPOM(ProgramEntity program) {
        //File f = new File(program.getClassFilename());
        //return(f.exists());

        String deps = "";

        for (int i=0; i<program.getDependencies().size(); i++) {
            logger.info("Found dependency: "+program.getDependencies().get(i).getGroupId()+"."+program.getDependencies().get(i).getArtifactId());

            deps += "<dependency>"+ System.lineSeparator() +
            "<groupId>"+program.getDependencies().get(i).getGroupId()+"</groupId>" + System.lineSeparator() +
            "<artifactId>"+program.getDependencies().get(i).getArtifactId()+"</artifactId>" + System.lineSeparator() +
            "<version>"+program.getDependencies().get(i).getVersion()+"</version>" + System.lineSeparator() +
            "</dependency>" + System.lineSeparator();
        }

        String pom = Utils.loadTextFileFromResources("pom-template.xml");
        pom = pom.replace("{name}", program.getName())
                 .replace("{version}", program.getVersion())
                 .replace("{groupId}", program.getGroupId())
                 .replace("{artifactId}", program.getArtifactId())
                 .replace("{jlogic-repository}", Utils.getHomeDir()+"/m2/repository")
                 //.replace("{standardPath}", Utils.getLibDir()+"/Standard.jar")
                 .replace("{mainClass}", program.getMainClass())
                 .replace("{dependencies}", deps);

        //System.out.println(pom);

        try {
            Utils.saveFile(pom, program.getMyDir()+"/pom.xml");
        } catch (IOException e) {
            logger.error(e.getMessage());
            return false;
        }

        return true;
	}

	public ProgramEntity createEmpty(String name) {
        ProgramEntity program = new ProgramEntity(UUID.randomUUID().toString(), name);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        program.setOwner(((User) auth.getPrincipal()).getUsername());

        // Create directory tree
        String progDir = getProgramBaseDirectory()+"/"+program.getId();

        logger.info("Creating project directory "+progDir+" ...");

        if (!new File(progDir).mkdir()) {
            repository.refresh(program);
            return null;
        }

        String treeJava = progDir+"/"+program.getSrcDir();
        String treeResources = progDir+"/src/main/resources";

        logger.info("Creating project directory tree ...");

        new File(treeJava).mkdirs();
        new File(treeResources).mkdirs();

        // pom
        /*
        logger.info("Creating POM file ...");

        if (!createPOM(program)) {
            logger.error("Unable to create POM file");
            return null;
        }*/

        // mvn archetype:generate -DgroupId=com.mycompany.app -DartifactId=my-app -DarchetypeArtifactId=maven-archetype-quickstart -DarchetypeVersion=1.4 -DinteractiveMode=false


        // Save program
        logger.info("Saving program into database..");
        program = repository.save(program);

        logger.info("Successfully created "+program.toString());

        return program;
	}

	public ProgramEntity create(String name) {
        ProgramEntity program = createEmpty(name);

        if (program == null)
            return null;

        //logger.info("Creating index for "+program.getName());
        //program.createIndex();

        logger.info("Adding properties to "+program.getName());
        program.createProperties();

        logger.info("Adding default blueprints to "+program.getName());
        BlueprintEntity main = blueprintService.create(program, BlueprintType.MAIN, "Main");
        BlueprintEntity events = blueprintService.create(program, BlueprintType.EVENTS, "Events");

        repository.refresh(program);

        return program;
	}



	public boolean delete (ProgramEntity program) {
        logger.info("Deleting program "+program.getName());

        for (BlueprintEntity b: program.getBlueprints())
            blueprintService.delete(b);

        repository.delete(program);

        File programDir = new File(program.getMyDir());

        if (Utils.deleteDirectory(programDir)) {
          return true;
        }

        return false;
	}

    public Optional<ProgramEntity> findById (String programId) {
        return (repository.findById(programId));
    }

    public void rename (ProgramEntity program, String name) {
        program.setName(name);
        repository.save(program);
    }

    public void save (ProgramEntity program) {
        repository.save(program);
    }

    /**
     * Compile program with JDK
     */
    /*
    public boolean compileJDK(ProgramEntity program) {
        boolean result = program.compile();
        repository.save(program);
        repository.refresh(program);
        return (result);
    }*/

    /**
     * Create classpath file
     */
    public boolean createCP(ProgramEntity program) {
        logger.info("Creating classpath file...");
/*
        List<String> args = new ArrayList<String>();

        args.add("mvn");
        args.add("--batch-mode"); // Disable ansi colors
        args.add("--quiet");
        args.add("dependency:build-classpath");
        args.add("-Dmdep.outputFile="+program.getClasspathFile());

        Result result = Utils.execute(args, program.getMyDir());

        return(result.success());
*/
        boolean result = false;
        InvocationRequest request = new DefaultInvocationRequest();
        request.setBatchMode(true);
        request.setPomFile(new File(program.getMyDir()+"/pom.xml"));
        request.setGoals(Collections.singletonList("dependency:build-classpath"));
        request.setBaseDirectory(new File(program.getMyDir()));
        request.setMavenOpts("-Dmdep.outputFile="+program.getClasspathFile());

        Invoker invoker = new DefaultInvoker();

        try {
            invoker.setMavenHome(new File(MAVEN_HOME));
            //invoker.setMavenExecutable("")
            System.out.println("MAVEN_HOME = "+invoker.getMavenHome());
            //invoker.execute(request);
            InvocationResult res = invoker.execute(request);


            result = res.getExitCode() == 0 ? true : false;
        } catch (MavenInvocationException e) {
            logger.error("Unable to execute Maven: "+e.getMessage());
        }

        return(result);
    }

    /**
     * Compile program with Maven
     */
    public Result compile(ProgramEntity program, int mode) {
        Result result = new Result();

        new File(program.getClasspathFile()).delete();
        program.setStatus(ProgramStatus.ERRORS);

        result = program.generateJava();

        System.out.println("bp2java result: "+result.getCode());
        System.out.println("bp2java success: "+result.success());

        if (result.success()) {
            if (!createPOM(program)) {
                result.setResult(1, "Unable to create POM file");
                return(result);
            }
/*
            List<String> args = new ArrayList<String>();

            args.add("mvn");
            args.add("--batch-mode"); // Disable ansi colors
            args.add("compile");

            if (mode == JAR)
                args.add("assembly:single");

            logger.info("Compiling "+program.getName()+"...");

            result = Utils.execute(args, program.getMyDir());

            if (result.success()) {
                program.setBuildTime(new Date());
                program.setUpdateTime(new Date());
                program.setStatus(ProgramStatus.COMPILED);
                result.setMessage ("Successfully compiled "+program.getName());
            } else {
                result.setMessage("Compiler error (code "+result.getCode()+")");
            }
            */


            InvocationRequest request = new DefaultInvocationRequest();
            request.setBatchMode(true);
            request.setPomFile(new File(program.getMyDir()+"/pom.xml"));
            //request.setBaseDirectory(new File(program.getMyDir()));

            if (mode == COMPILE)
                request.setGoals(Collections.singletonList("compile"));
            else if (mode == JAR)
                request.setGoals(Arrays.asList("compile", "assembly:single"));

            //request.setMavenOpts("--quiet");

            Invoker invoker = new DefaultInvoker();

            try {
                invoker.setMavenHome(new File(MAVEN_HOME));
                InvocationResult res = invoker.execute(request);

                if (res.getExitCode() == 0) {
                    program.setBuildTime(new Date());
                    program.setUpdateTime(new Date());
                    program.setStatus(ProgramStatus.COMPILED);
                    result.setMessage ("Successfully compiled "+program.getName());

                    if (!createCP(program))
                        logger.error("Unable to create classpath file");

                } else {
                    result.setResult(Result.ERROR, "Compiler error (code "+res.getExitCode()+")");
                }
            } catch (MavenInvocationException e) {
                result.setResult(Result.ERROR, "Unable to execute Maven: "+e.getMessage());
            }

/*
            System.setProperty("maven.multiModuleProjectDirectory", Utils.MAVEN_HOME);
            MavenCli maven = new MavenCli();
            int res = maven.doMain(new String[]{"package", "--quiet", "--batch-mode"}, program.getMyDir(), System.out, System.out);
            logger.info("Result code: "+res);
*/

        } else {
            result.setResult(Result.ERROR, "Can't generate source for "+program.getName()+": "+result.getMessage());
        }



        logger.info("Updating program info...");

        repository.save(program);

        return (result);
    }

    /**
     * Compile program (only class)
     */
    public Result compile(ProgramEntity program) {
        logger.info("Compiling program "+program.getName());
        return(compile(program, COMPILE));
    }

    /**
     * Compile program (only class)
     */
    public Result createJAR(ProgramEntity program) {
        logger.info("Creating jar for "+program.getName());
        return(compile(program, JAR));
    }

    /**
     * Run program with input parameters as string
     */
	public boolean run(ProgramEntity program, String methodName, String data, String logName, HttpServletRequest request) {
        try {
            logger.info("Parsing data...");
            Map<String, Object> map = Utils.jsonToMap(data);

            return (run(program, methodName, map, logName, request));
        } catch (ParseException e) {
            setResult(ProgramEntity.BAD_INPUT, e.getMessage());
            return false;
        }
	}

    /**
     * Run program with input parameters as Map
     */
    public boolean run(ProgramEntity program, String methodName, Map<String, Object> actual, String logName, HttpServletRequest request) {
        boolean result = false;

        if (!new File(program.getClasspathFile()).exists()){
            if (!createCP(program)) {
                logger.error("Unable to create classpath file");
                return(result);
            }
        }

        //sessionService.setActive(request, true);
        sessionService.setProgramUnit(request, program.getName()+"."+methodName);
        sessionService.setCurrentActive(true);

        result = program.run(methodName, actual, logName, request);

        sessionService.setProgramUnit(request, "");
        //sessionService.setActive(request, false);
        sessionService.setCurrentActive(false);

        return(result);
    }

	public Variable addVariable (ProgramEntity program, Variable v) {
        if (program.hasVariable(v.getName()))
            return null;

        if (!v.isValid()) {
            // Check id
            if (v.getId() == null)
                v.setId(UUID.randomUUID());

            // Check name
            if (v.getName() == null) {
                int i=1;
                String name;

                while (true) {
                    name ="Variable_"+i;

                    if (!program.hasVariable(name)) {
                        v.setName(name);
                        break;
                    }

                    i ++;
                }
            }

            if (v.getType() == null)
                v.setType("Integer");
        }

        Variable newVar = program.addVariable(v);
        repository.save(program);
        repository.refresh(program);

        logger.info("Created "+v.toString());

        return newVar;
	}

	public boolean updateVariable (ProgramEntity program, Variable v) {
        Variable pv = program.getVariable(v.getId());

        if (pv != null && v.isValid() /*&& !program.variableIsReferenced(pv)*/) {
            pv.set(v);
            repository.save(program);
            repository.refresh(program);
            logger.info("Updated "+v.toString());
            return (true);
        }

        return false;
	}

	public boolean deleteVariable (ProgramEntity program, Variable v) {
        if (v != null && program.variableIsReferenced(v)) {
            setMessage("used in one or more blueprints.");
            return false;
        }

        if (program.deleteVariable(v)) {
            repository.save(program);
            //System.out.println(program.getVariables());
            return (true);
        }

        //setMessage("Can't delete variable "+pv.getName());

        return false;
	}

	public boolean deleteVariable (ProgramEntity program, String name) {
        Variable pv = program.getVariable(name);
        return(deleteVariable(program, pv));
	}

	public boolean renameVariable (ProgramEntity program, String oldName, String newName) {
        Variable pv = program.getVariable(oldName);

        if (pv != null && program.getVariable(newName) == null) {
            pv.setName(newName);
            repository.save(program);
            return (true);
        }

        return false;
	}

    public JSONObject getIndex(ProgramEntity program) {
        if (compiled(program))
            return(getIndexFromClass(program));
        else
            return(getIndexFromFiles(program));
    }

    public JSONObject getIndexFromFiles(ProgramEntity program) {
        JSONObject jindex = new JSONObject();

        for (BlueprintEntity b: program.getBlueprints()) {
            jindex.put(b.getId(), blueprintService.getSpec(b));
        }

        return(jindex);
    }

  public JSONObject getIndexFromClass(ProgramEntity program) {
    JSONObject jprogram = new JSONObject();

    try {
      URL[] urls = program.getURLs();

      ClassLoader cl = new URLClassLoader(urls);

      Class Program = cl.loadClass("Program");
      Class BlueprintAnnotation = cl.loadClass("com.lionsoft.jlogic.standard.Blueprint");
      Class BPConnectorAnnotation = cl.loadClass("com.lionsoft.jlogic.standard.BPConnector");

      Method[] methods = Program.getMethods();
      for (Method m : methods) {
        String id = null;
        Integer internalId = null;
        JSONObject jbp = new JSONObject();
        JSONArray jinput = new JSONArray();
        JSONArray joutput = new JSONArray();
        jbp.put("input", jinput);
        jbp.put("output", joutput);

        JSONObject jexec = new JSONObject();
        jexec.put("label", "");
        jexec.put("type", "Exec");
        jinput.add(jexec);
        joutput.add(jexec);

        Method method;
        Object value;
        String[] parts = null;

        Annotation annotation = m.getAnnotation(BlueprintAnnotation);

        if (annotation != null) {
          // Is a Blueprint

          System.out.println(" method: " + m.getName());
          System.out.println(" ReturnType: "+ m.getReturnType());
          System.out.println(" Returns: "+ (m.getReturnType().equals(Void.TYPE) ? "No" : "Yes"));
          //System.out.println(" GenericReturnType: "+ m.getGenericReturnType());
          //System.out.println(m.toString());

          Annotation outAnnotation = m.getAnnotation(BPConnectorAnnotation);

          if (outAnnotation != null) {
            // Has output

            JSONObject jout = new JSONObject();
            Class<? extends Annotation> outConn = outAnnotation.annotationType();

            for (Method mOut : outConn.getDeclaredMethods()) {
               value = mOut.invoke(outAnnotation, (Object[])null);
               jout.put(mOut.getName(), value);
            }

            parts = m.getReturnType().toString().split("\\.");
            jout.put("type", parts[parts.length-1].replace(";", ""));

            jout.put("dimensions", StringUtils.countOccurrencesOf(m.getReturnType().toString(), "["));

            joutput.add(jout);
          }

          jbp.put("method", m.getName());

          for (Parameter p : m.getParameters()) {
            //System.out.println(" "+ p.getType() +"  " + p.getName() + " " + Modifier.toString(p.getModifiers()));
            //System.out.println(" dimensions: "+ StringUtils.countOccurrencesOf(p.getType().toString(), "["));
            parts = p.getType().toString().split("\\.");

            JSONObject jparam = new JSONObject();
            jparam.put("label", p.getName());
            jparam.put("type", parts[parts.length-1].replace(";", ""));
            jparam.put("dimensions", StringUtils.countOccurrencesOf(p.getType().toString(), "["));

            Annotation paramAnnotation = p.getAnnotation(BPConnectorAnnotation);

            if (paramAnnotation != null) {
              Class<? extends Annotation> bpconnector = paramAnnotation.annotationType();
              method = bpconnector.getMethod("id");
              value = method.invoke(paramAnnotation, (Object[])null);
              jparam.put("id", value);
            }

            jinput.add(jparam);
          }

          Class<? extends Annotation> type = annotation.annotationType();
          //System.out.println("Values of " + type.getName());
          /*for (Method method : type.getDeclaredMethods()) {
             Object value = method.invoke(annotation, (Object[])null);
             System.out.println(" " + method.getName() + ": " + value);
             jbp.put(method.getName(), value);
          }*/
          System.out.println("Annotation: "+type.getName());

          method = type.getMethod("id");
          value = method.invoke(annotation, (Object[])null);
          id = (String) value;
          jbp.put("id", value);

          method = type.getMethod("internalId");
          value = method.invoke(annotation, (Object[])null);
          internalId = (Integer) value;
          jbp.put("internalId", internalId);

          method = type.getMethod("name");
          value = method.invoke(annotation, (Object[])null);
          jbp.put("name", value);

          method = type.getMethod("type");
          value = method.invoke(annotation, (Object[])null);
          jbp.put("type", value);

          jprogram.put(id, jbp);
        }

      }
    } catch (ClassNotFoundException e) {
      logger.error("Class not found: "+e.getMessage());
      return null;
    } catch (InvocationTargetException e) {
      e.printStackTrace();
      return null;
    } catch (IllegalAccessException e) {
      e.printStackTrace();
      return null;
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
      return null;
    }

    return jprogram;
  }

  /**
   * Get blueprint by internal id
   */
	public BlueprintEntity getBlueprint (ProgramEntity program, int interalId) {
    for (BlueprintEntity b: program.getBlueprints()) {
      if (b.getInternalId() == interalId)
        return b;
    }

    return null;
	}

	/**
	 * Import a bluerint
	 */
	public BlueprintEntity importBlueprint(ProgramEntity program, String content) {
        BlueprintEntity blueprint = null;

        setResult(SUCCESS, "");

        try {
            JSONParser jsonParser = new JSONParser();
            JSONObject jbp = (JSONObject) jsonParser.parse(content);

            String name = (String) jbp.get("name");
            String type = (String) jbp.get("type");
            //int internalId = ((Long) jbp.get("internalId")).intValue();

            logger.info("Creating blueprint "+name);
            blueprint = blueprintService.create(program, /*BlueprintType.GENERIC*/BlueprintType.valueOf(type), name);

            logger.info("Updating "+blueprint.toString());
            Code code = blueprintService.update(blueprint, content);

            if (code != Code.SUCCESS) {
                logger.error("Unable to update importing blueprint "+name);
                return null;
            }

            logger.info("Refreshing program "+program.getName());
            repository.refresh(program);

            return(blueprint);

        } catch (ParseException e) {
            setResult(1, e.getMessage());
        }

        return null;
	}

	public BlueprintEntity importBlueprintFile(ProgramEntity program, String filename) {
        try {
            String content = new String (Files.readAllBytes(Paths.get(filename)));
            return(importBlueprint(program, content));
        }
        catch (IOException e) {
            logger.error(e.getMessage());
        }

        return null;
	}

	/**
	 * Pack program into a given zip file
	 */
	public boolean pack(ProgramEntity program, String packFilename) {
        List<String> srcFiles = new ArrayList<String>();

        logger.info("Packing "+program.getName()+" into "+packFilename);

        JSONObject jinfo = new JSONObject();
        jinfo.put("name", program.getName());
        jinfo.put("fromVersion", buildProperties.getVersion());
        jinfo.put("exportTime", (new Date()).toString());

        JSONArray jbpa = new JSONArray();

        for (BlueprintEntity b: program.getBlueprints()) {
            //System.out.println(b.getFilename());
            srcFiles.add(b.getFilename());

            JSONObject jbp = new JSONObject();
            jbp.put("internalId", b.getInternalId());
            jbp.put("name", b.getBaseFilename());
            jbpa.add(jbp);
        }

        jinfo.put("blueprints", jbpa);

        srcFiles.add(program.getMyDir()+"/Program.properties");

        try {
            FileOutputStream fos = new FileOutputStream(packFilename);
            ZipOutputStream zipOut = new ZipOutputStream(fos);

            // Pack program files
            for (String srcFile : srcFiles) {
                //logger.info("Packing "+srcFile);

                File fileToZip = new File(srcFile);
                FileInputStream fis = new FileInputStream(fileToZip);
                ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
                zipOut.putNextEntry(zipEntry);

                byte[] bytes = new byte[1024];
                int length;
                while((length = fis.read(bytes)) >= 0) {
                    zipOut.write(bytes, 0, length);
                }
                fis.close();
            }

            // Pack info file
            ZipEntry zipEntry = new ZipEntry("info.json");
            zipOut.putNextEntry(zipEntry);
            zipOut.write(jinfo.toString().getBytes("UTF-8"),
                       0,
                       jinfo.toString().getBytes("UTF-8").length);

            zipOut.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
	}

	/**
	 * Pack program
	 */
	public String pack(ProgramEntity program) {
	  String packFileBase = program.getName().replaceAll("[ .;]", "_")+".zip";
	  String packFile = getTempDirectory()+"/"+packFileBase;

	  if (!pack(program, packFile))
	    return null;

	  return packFile;
	}


    public Optional<String> getExtensionByStringHandling(String filename) {
        return Optional.ofNullable(filename)
          .filter(f -> f.contains("."))
          .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

  /*public String getInfoFromZip(ZipInputStream zis) {
    String info = null;

    try {
      ZipEntry zipEntry = zis.getNextEntry();

      while (zipEntry != null) {
        if (zipEntry.getName().equals("info.json")) {
          info = "Found";
        }

        zipEntry = zis.getNextEntry();
      }

    } catch (IOException e) { }

    return info;
  }*/

	/**
	 * Import program from file
	 */
	public ProgramEntity importProgramFromFile(String importId, String zipFile) {
        ProgramEntity program = null;
        String unzippedDir = getTempDirectory()+"/"+importId;

        // Unpack file
        logger.info("Unpacking "+zipFile);

        if (Utils.unpack(zipFile, unzippedDir)) {
            // Load info file
            JSONObject jinfo = Utils.loadJsonFile(unzippedDir+"/info.json");

            if (jinfo != null) {
                // Create new program
                String programName = (String) jinfo.get("name");
                logger.info("Creating program "+programName);

                program = createEmpty(programName);

                if (program != null) {
                    // Import blueprints
                    JSONArray jblueprints = (JSONArray) jinfo.get("blueprints");

                    for (int k=0; k<jblueprints.size(); k++) {
                        JSONObject jbp = (JSONObject) jblueprints.get(k);

                        int internalId = ((Long) jbp.get("internalId")).intValue();
                        String bpfile = (String) jbp.get("name");

                        logger.info("Importing "+bpfile);
                        BlueprintEntity b = importBlueprintFile(program, unzippedDir+"/"+bpfile);
                        b.setInternalId(internalId);
                    }

                    // Copy propertes file
                    try {
                        Path propsFrom = Paths.get(unzippedDir+"/Program.properties");
                        Path propsTo = Paths.get(program.getMyDir()+"/Program.properties");
                        Files.copy(propsFrom, propsTo, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        logger.error(e.getMessage());
                    }
                } else {
                    logger.error("Can't create program");
                }
            } else {
                logger.error("Unable to load info file");
            }

            logger.info("Deleting temporary dir "+unzippedDir);
            Utils.deleteDirectory(unzippedDir);

            File file = new File(zipFile);
            file.delete();
        } else {
            logger.error("Unable to unzip file");
        }

        return program;
	}

	public ProgramEntity clone(ProgramEntity program) {
        ProgramEntity clone = null;
        String cloneName = "Clone of "+program.getName();

        logger.info("Creating program "+cloneName);

        clone = createEmpty(cloneName);

        if (clone == null) {
            logger.error("Can't create program "+cloneName);
            return null;
        }

        // Import blueprints
        for (BlueprintEntity sourceBP: program.getBlueprints()) {
            BlueprintEntity b = importBlueprintFile(clone, sourceBP.getFilename());
            b.setInternalId(sourceBP.getInternalId());
        }

        // Copy propertes file
        try {
            Path propsFrom = Paths.get(program.getMyDir()+"/Program.properties");
            Path propsTo = Paths.get(clone.getMyDir()+"/Program.properties");
            Files.copy(propsFrom, propsTo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

        return(clone);
	}
}
