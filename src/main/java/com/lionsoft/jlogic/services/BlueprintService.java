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

  @Autowired
  BlueprintRepository repository;

  @Autowired
  ProgramService programService;

  ApplicationHome home = new ApplicationHome(BlueprintService.class);
  Logger logger = LoggerFactory.getLogger(ProgramController.class);

  String message;

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

  public String getMessage() {
    return message;
  }

  public void setMessage(String m) {
    message = m;
  }

	public String getFilename(BlueprintEntity blueprint) {
    ProgramEntity program = blueprint.getProgram();
		return home.getDir()+"/../data/program/" + program.getId() + "/BP_" + blueprint.getId() + ".json";
	}

	public String getTemplateDirectory() {
		return home.getDir()+"/../data/blueprint";
	}

	public boolean saveContent(ProgramEntity program, String filename, BlueprintEntity blueprint, String content) {

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

  	  logger.info("Checking global variables...");

      JSONArray ja = (JSONArray) jo.get("variables");

      for (int k=0; k<ja.size(); k++) {
        JSONObject jvar = (JSONObject) ja.get(k);

        Variable v = new Variable((String) jvar.get("type"), ((Long) jvar.get("dimensions")).intValue(), (String) jvar.get("name"));
        v.setId(UUID.fromString((String) jvar.get("id")));
        v.setGlobal(jvar.containsKey("global") ? (boolean) jvar.get("global") : false);

        //System.out.println("Found "+v.toString());

        if (v.isGlobal()) {
          logger.info("Checking "+v.toString());

          Variable progVar = program.getVariable(v.getId());

          if (progVar == null) {
            // New variable, check name
            if (program.getVariable(v.getName()) != null) {
                setMessage("a global variable with name "+v.getName()+" already existing");
                return false;
            }

            logger.info("Adding "+v.toString());

            Variable newVar = programService.addVariable(program, v);

            if (newVar == null) {
                setMessage("Can't create global variable "+v.getName());
                return false;
            }
          } else {
            boolean isSameType = (v.getType().equals(progVar.getType())) && (v.getDimensions() == progVar.getDimensions());

            if (!isSameType && program.variableIsReferenced(progVar)) {
              setMessage("Can't modify referenced variable "+v.getName());
              return false;
            }

            logger.info("Updating "+v.toString());

            if (!programService.updateVariable(program, v)) {
              setMessage("Can't update variable "+v.getName());
              return false;
            }
          }
        }
      }
    }
    catch (ParseException e) {
      e.printStackTrace();
      setMessage(e.getMessage());
      return false;
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
      e.printStackTrace();
      setMessage(e.getMessage());
      return false;
    }

    return true;
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

      if (saveContent(program, blueprint.getFilename(), blueprint, content)) {
        //blueprint.getProgram().updateIndex(blueprint, content);
        repository.save(blueprint);
      }
      else
        return null;

    }
    catch (IOException e1) {
      e1.printStackTrace();
      return null;
    }

    logger.info("Created "+blueprint);

	  return blueprint;
	}

  public boolean update (BlueprintEntity blueprint, String content) {
    if (saveContent(blueprint.getProgram(), blueprint.getFilename(), blueprint, content)) {
      //blueprint.getProgram().updateIndex(blueprint, content);
      repository.save(blueprint);
      return true;
    }

    return false;
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
}
