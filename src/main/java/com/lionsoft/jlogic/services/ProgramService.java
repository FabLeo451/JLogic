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

@Service
public class ProgramService {

  final static int SUCCESS = 0;
  final static int ERROR = 1;

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

	public ProgramService() {

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

	public ProgramEntity createEmpty(String name) {
	  ProgramEntity program = new ProgramEntity(UUID.randomUUID().toString(), name);

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    program.setOwner(((User) auth.getPrincipal()).getUsername());
    program = repository.save(program);

    logger.info("Successfully created "+program.toString());

    String progDir = getProgramBaseDirectory()+"/"+program.getId();

    File progFile = new File(progDir);

    if (progFile.mkdir()) {
      //program.createIndex();
      //program.createProperties();

      // Create blueprints
	    //BlueprintEntity main = blueprintService.create(program, BlueprintType.MAIN, "Main");
	    //BlueprintEntity events = blueprintService.create(program, BlueprintType.EVENTS, "Events");

	    repository.refresh(program);

      return program;
    }

    return null;
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

  boolean deleteDirectory(File directoryToBeDeleted) {
    File[] allContents = directoryToBeDeleted.listFiles();

    if (allContents != null) {
      for (File file : allContents) {
        //deleteDirectory(file);
        file.delete();
      }
    }
    return directoryToBeDeleted.delete();
  }

  boolean deleteDirectory(String d) {
    File dir = new File(d);
    return(deleteDirectory(dir));
  }

	public boolean delete (ProgramEntity program) {

    for (BlueprintEntity b: program.getBlueprints())
      blueprintService.delete(b);

    repository.delete(program);

    File programDir = new File(program.getMyDir());

    if (deleteDirectory(programDir)) {

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

	public boolean compile (ProgramEntity program) {
	  boolean result = program.compile();
	  repository.save(program);
	  repository.refresh(program);
	  return (result);
	}

  public boolean run(ProgramEntity program, String methodName, String data, String logName, HttpServletRequest request) {
    boolean result = false;

    //Session session = sessionService.getSession(request);
    sessionService.setProgramUnit(request, program.getName()+"."+methodName);

    result = program.run(methodName, data, logName, request);

    //session.setStatus(Session.ACTIVE);
    sessionService.setProgramUnit(request, "");

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
/*
    if (pv != null && program.variableIsReferenced(pv)) {
      setMessage("used in one or more blueprints.");
      return false;
    }

	  if (program.deleteVariable(name)) {
	    repository.save(program);
	    //System.out.println(program.getVariables());
	    return (true);
	  }

    //setMessage("Can't delete variable "+pv.getName());

	  return false;*/
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
      blueprint = blueprintService.create(program, /*BlueprintType.GENERIC*/BlueprintType.valueOf(type), name);

      //logger.info("Updating blueprint "+blueprint.get().toString());

      Code code = blueprintService.update(blueprint, content);
      
      if (code != Code.SUCCESS) {
        setResult(1, "Can't import blueprint");
        return null;
      }
      
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

	/**
	 * This method guards against writing files to the file system outside of the target folder. 
	 * This vulnerability is called Zip Slip.
	 */
  public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
      File destFile = new File(destinationDir, zipEntry.getName());
   
      String destDirPath = destinationDir.getCanonicalPath();
      String destFilePath = destFile.getCanonicalPath();
   
      if (!destFilePath.startsWith(destDirPath + File.separator)) {
          throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
      }
   
      return destFile;
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
  
  public boolean unpack(String zipFile, String targetDir) {
    File destDir = new File(targetDir);
    byte[] buffer = new byte[1024];
    
    try {
      ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
      ZipEntry zipEntry = zis.getNextEntry();

      while (zipEntry != null) {
         File newFile = newFile(destDir, zipEntry);
         
         if (zipEntry.isDirectory()) {
           if (!newFile.isDirectory() && !newFile.mkdirs()) {
               logger.error("Failed to create directory " + newFile);
               return false;
           }
         } else {
           logger.info("Extracting "+zipEntry.getName());

           File parent = newFile.getParentFile();
           if (!parent.isDirectory() && !parent.mkdirs()) {
               logger.error("Failed to create directory " + parent);
           }
           
           // write file content
           FileOutputStream fos = new FileOutputStream(newFile);
           int len;
           while ((len = zis.read(buffer)) > 0) {
               fos.write(buffer, 0, len);
           }
           fos.close();
         }
         
         zipEntry = zis.getNextEntry();
      }
      
      zis.closeEntry();
      zis.close();
    } catch (FileNotFoundException e) {
        logger.error(e.getMessage());
        return false;
    } catch (IOException e) {
        logger.error(e.getMessage());
        return false;
    }
    
    return true;
  }
  
  public JSONObject loadJsonFile(String filename) {
    JSONObject jo = null;
    
    try {
      String content = new String (Files.readAllBytes(Paths.get(filename)));

      try {
        JSONParser jsonParser = new JSONParser();
        jo = (JSONObject) jsonParser.parse(content);
      }
      catch (ParseException e) {
        logger.error("Unable to parse json file "+filename+": "+e.getMessage());
        return null;
      }
    }
    catch (IOException e) {
      logger.error(e.getMessage());
      return null;
    }  
    
    return jo;
  }

	/**
	 * Import program from file
	 */
	public ProgramEntity importProgramFromFile(String importId, String zipFile) {
	  ProgramEntity program = null;
	  String unzippedDir = getTempDirectory()+"/"+importId;
    
    // Unpack file
    logger.info("Unpacking "+zipFile);

    if (unpack(zipFile, unzippedDir)) {
      // Load info file
      JSONObject jinfo = loadJsonFile(unzippedDir+"/info.json");
      
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
      deleteDirectory(unzippedDir);
      
      File file = new File(zipFile);
      file.delete();
    } else {
      logger.error("Unable to unzip file");
    }
            
	  return program;
	}
}
