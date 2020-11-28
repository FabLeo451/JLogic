package com.lionsoft.jlogic;

import org.springframework.boot.system.ApplicationHome;
import java.io.*;
/*
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;*/
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.Paths;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.Optional;

import com.lionsoft.jlogic.BlueprintType;

@Service
//@Transactional
public class BlueprintService {

  final static int SUCCESS = 0;
  final static int ERROR = 1;

  @Autowired
  BlueprintRepository repository;

  @Autowired
  ProgramService programService;

  ApplicationHome home = new ApplicationHome(BlueprintService.class);
  Logger logger = LoggerFactory.getLogger(ProgramController.class);

  //int code = 0;
  //String message;

	public BlueprintService() {
	}

	public Optional<BlueprintEntity> findById (String id) {
    return (repository.findById(id));
	}

	public Optional<BlueprintEntity> findByNameAndProgram (String name, ProgramEntity program) {
    return (repository.findByNameAndProgram(name, program));
	}

	public Optional<BlueprintEntity> findByName (String name) {
    return (repository.findByName(name));
	}
/*
	public int save() {
	  String filename = getDirectory() + getFileName(id);

	  // https://howtodoinjava.com/library/json-simple-read-write-json-examples/

	  //System.out.println(content.toString());

    try (FileWriter file = new FileWriter(filename)) {

        file.write(content.toString());
        file.flush();

    } catch (IOException e) {
        e.printStackTrace();
    }

	  return (0);
	}*/
/*
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
*/
	public String getFilename(BlueprintEntity blueprint) {
    ProgramEntity program = blueprint.getProgram();
		return home.getDir()+"/../data/program/" + program.getId() + "/BP_" + blueprint.getId() + ".json";
	}

	public String getTemplateDirectory() {
		return home.getDir()+"/../data/blueprint";
	}

	public Code saveContent(ProgramEntity program, String filename, BlueprintEntity blueprint, String content) {

    // Add/modify content data

    JSONParser jsonParser = new JSONParser();
    JSONObject jo = null;

    try {
      //jo = new JSONObject(content);
      jo = (JSONObject) jsonParser.parse(content);
      jo.put("id", blueprint.getId());
      jo.put("internalId", blueprint.getInternalId());
      jo.put("type", blueprint.getType().name());
      jo.put("name", blueprint.getName());
      jo.put("method", blueprint.getMethod());

      // Check global variables
  	  logger.info("Checking variables...");

      JSONArray ja = (JSONArray) jo.get("variables");

      for (int k=0; k<ja.size(); k++) {
        JSONObject jvar = (JSONObject) ja.get(k);

        Variable v = new Variable((String) jvar.get("type"), ((Long) jvar.get("dimensions")).intValue(), (String) jvar.get("name"));
        v.setId(UUID.fromString((String) jvar.get("id")));
        v.setGlobal(jvar.containsKey("global") ? (boolean) jvar.get("global") : false);

        //System.out.println("Found "+v.toString());

        if (v.isGlobal()) {
          // Check global variable
          logger.info("Checking global "+v.toString());

          Variable progVar = program.getVariable(v.getId());

          if (progVar == null) {
            // New variable, check name and add to program
            if (program.getVariable(v.getName()) != null) {
                logger.error("a global variable with name "+v.getName()+" already existing");
                return(Code.ERR_BP_GLOBAL_EXISTS);
            }

            logger.info("Adding "+v.toString());

            Variable newVar = programService.addVariable(program, v);

            if (newVar == null) {
                logger.error("Can't create global variable "+v.getName());
                return(Code.ERR_BP_CREATE_GLOBAL);
            }
          } else {
            // Exsisting variable, update
            boolean isSameType = (v.getType().equals(progVar.getType())) && (v.getDimensions() == progVar.getDimensions());

            if (!isSameType && program.variableIsReferenced(progVar)) {
              logger.error("Can't modify referenced variable "+v.getName());
              return(Code.ERR_BP_MODIFY_REFERENCED);
            }

            logger.info("Updating "+v.toString());

            if (!programService.updateVariable(program, v)) {
              logger.error("Can't update variable "+v.getName());
              return(Code.ERR_BP_UPDATE_VAR);
            }
          }
        } else {
          // Check local variable
          logger.info("Checking local "+v.getName());
          
          /* 
           * If a local variable exists as global too, check if it's referenced only by this blueprint and delete the global.
           * This can occur when user changes a variable from global to local
           */
          Variable progVar = program.getVariable(v.getId());
          
          if (progVar != null) {
            if (!program.variableIsReferenced(progVar, blueprint)) {
              programService.deleteVariable(program, progVar);
              logger.info("Deleted global "+v.getName());
            } else {
              logger.error("Variable "+v.getName()+" is referenced by other blueprints and cannot be changed from global to local");
              return(Code.ERR_BP_GLOBAL_TO_LOCAL);
            }
          }
        }
      }
    }
    catch (ParseException e) {
      logger.error(e.getMessage());
      return(Code.ERR_PARSE);
    }

    logger.info("Saving blueprint "+blueprint.getName());

    // Update database

    blueprint.setContent(jo.toString());

	  // Save file

	  try (FileWriter file = new FileWriter(filename)) {
      file.write(jo.toString());
      file.flush();
      file.close();
	  } catch (IOException e) {
      logger.error(e.getMessage());
      return(Code.ERR_IO);
    }

    return(Code.SUCCESS);
	}

	public BlueprintEntity create(ProgramEntity program, BlueprintType type, String name) {
	  String templateFilename;
	  //JSONObject jo = null;

	  logger.info("Creating blueprint "+name);

	  BlueprintEntity blueprint = new BlueprintEntity (UUID.randomUUID().toString(), name, type);
    // program.addBlueprint(blueprint);
	  blueprint.setProgram(program);
	  repository.save(blueprint);

	  //String filename = program.getMyDir()+"/BP_"+blueprint.getId()+".json";

	  //logger.info("Saving "+filename);

	  switch (type) {
	    case MAIN:
	      templateFilename = getTemplateDirectory()+"/Main.json";
        blueprint.setInternalId(1);
	      break;

	    case EVENTS:
	      templateFilename = getTemplateDirectory()+"/Events.json";
        blueprint.setInternalId(2);
	      break;

	    default:
	      templateFilename = getTemplateDirectory()+"/Generic.json";
	      break;
	  }

	  try {
	    String content = new String (Files.readAllBytes(Paths.get(templateFilename)));

      Code code = saveContent(program, blueprint.getFilename(), blueprint, content);
      
      if (code == Code.SUCCESS)
        repository.save(blueprint);
      else
        return null;
    }
    catch (IOException e) {
      e.printStackTrace();
      return null;
    }

    logger.info("Created "+blueprint);

	  return blueprint;
	}

  public Code update (BlueprintEntity blueprint, String content) {
    Code code = saveContent(blueprint.getProgram(), blueprint.getFilename(), blueprint, content);
    
    if (code == Code.SUCCESS)
      repository.save(blueprint);

    return(code);
  }

  public void delete (BlueprintEntity blueprint) {
    repository.delete(blueprint);
    //blueprint.getProgram().updateIndex(blueprint, null);
    File file = new File(getFilename(blueprint));
    file.delete();
  }
  
  public JSONObject toJSON(BlueprintEntity blueprint) {
    String programId = blueprint.getProgram().getId();
    String programName = blueprint.getProgram().getName();
    String filename = blueprint.getFilename();

    //System.out.println("Loading "+filename);

    try {
      String content = new String (Files.readAllBytes(Paths.get(filename)));

      // Add data
      try {
        JSONParser jsonParser = new JSONParser();
        JSONObject jo = (JSONObject) jsonParser.parse(content);

        jo.put("id", blueprint.getId());
        jo.put("programId", programId);
        jo.put("programName", programName);

        return jo;

      }
      catch (ParseException e) {
        logger.error(e.getMessage());
        return null;
      }
    }
    catch (IOException e) {
      logger.error(e.getMessage());
      return null;
    }
  }
  
  public BlueprintEntity clone(BlueprintEntity blueprint) {
    BlueprintEntity clone = null;
    String cloneName = "Clone of "+blueprint.getName();
    
    ProgramEntity program = blueprint.getProgram();
    clone = create(program, blueprint.getType(), cloneName);
    
    Code code = update(clone, toJSON(blueprint).toString());

    if (code != Code.SUCCESS) {
      delete(clone);
      //setResult(ERROR, getMessage());
      return null;
    }
    
    programService.refresh(program);
         
    return clone;
  }

}
