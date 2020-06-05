package com.lionsoft.jlogic;

import org.springframework.boot.system.ApplicationHome;
import java.io.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.time.Instant;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

import java.util.concurrent.locks.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
import java.beans.IntrospectionException;
import java.net.URLClassLoader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import java.security.Principal;

import java.lang.Thread;
import java.util.Properties;
import java.util.UUID;
import java.util.Optional;

@Service
public class ProgramService {

  @Autowired
  ProgramRepository repository;
  
  @Autowired
  BlueprintService blueprintService;
	
	Logger logger = LoggerFactory.getLogger(ProgramService.class);
	ApplicationHome home = new ApplicationHome(ProgramService.class);

	public ProgramService() {

	}

	public String getProgramBaseDirectory() {
		return home.getDir()+"/../data/program";
	}
	
	public ProgramEntity create (String name) {
	  ProgramEntity program = new ProgramEntity(UUID.randomUUID().toString(), name);
      
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
	  
    program.setOwner(((User) auth.getPrincipal()).getUsername());
    program = repository.saveAndFlush(program);
    
    String progDir = getProgramBaseDirectory()+"/"+program.getId();

    File progFile = new File(progDir);
    
    if (progFile.mkdir()) {
      program.createIndex();
      //program.createDefaultDependecies();
      program.createProperties();

      return program;
    }
    
    return null;
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

}
