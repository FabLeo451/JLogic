package com.lionsoft.jlogic;

import org.springframework.boot.system.ApplicationHome;
import java.io.*;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
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

  ApplicationHome home = new ApplicationHome(BlueprintService.class);
  Logger logger = LoggerFactory.getLogger(ProgramController.class);

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

	public String getFilename(BlueprintEntity blueprint) {
    ProgramEntity program = blueprint.getProgram();
		return home.getDir()+"/../data/program/" + program.getId() + "/BP_" + blueprint.getId() + ".json";
	}

	public String getTemplateDirectory() {
		return home.getDir()+"/../data/blueprint";
	}

	public boolean saveContent(String filename, BlueprintEntity blueprint, String content) {

	  logger.info("Saving blueprint "+blueprint.getName());

	  //System.out.println(content);

    // Add/modify content data

    JSONObject jo;

    try {
      jo = new JSONObject(content);
      jo.put("id", blueprint.getId());
      jo.put("type", blueprint.getType()/*.ordinal()*/);
      jo.put("name", blueprint.getName());
      jo.put("method", blueprint.getMethod());
    }
    catch (JSONException e) {
      e.printStackTrace();
      return false;
    }

    // Update database

    blueprint.setContent(jo.toString());

	  // Save file

	  try (FileWriter file = new FileWriter(filename)) {
      file.write(jo.toString());
      file.flush();
      file.close();
	  } catch (IOException e) {
      e.printStackTrace();
      return false;
    }

    return true;
	}

	public BlueprintEntity create(ProgramEntity program, BlueprintType type, String name) {
	  String templateFilename;
	  JSONObject jo = null;

	  logger.info("Creating blueprint "+name);

	  BlueprintEntity blueprint = new BlueprintEntity (UUID.randomUUID().toString(), name, type);
	  blueprint.setProgram(program);
	  repository.save(blueprint);

	  //String filename = program.getMyDir()+"/BP_"+blueprint.getId()+".json";

	  //logger.info("Saving "+filename);

	  switch (type) {
	    case MAIN:
	      templateFilename = getTemplateDirectory()+"/Main.json";
	      break;

	    case EVENTS:
	      templateFilename = getTemplateDirectory()+"/Events.json";
	      break;

	    default:
	      templateFilename = getTemplateDirectory()+"/Generic.json";
	      break;
	  }

	  try {
	    String content = new String (Files.readAllBytes(Paths.get(templateFilename)));

      if (saveContent(blueprint.getFilename(), blueprint, content)) {
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

  public void update (BlueprintEntity blueprint, String content) {
    if (saveContent(blueprint.getFilename(), blueprint, content)) {
      //blueprint.getProgram().updateIndex(blueprint, content);
      repository.save(blueprint);
    }
  }

  public void delete (BlueprintEntity blueprint) {
    repository.delete(blueprint);
    //blueprint.getProgram().updateIndex(blueprint, null);
    File file = new File(getFilename(blueprint));
    file.delete();
  }
}
