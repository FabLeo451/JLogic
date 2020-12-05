package com.lionsoft.jlogic;

import org.springframework.boot.system.ApplicationHome;
import java.io.*;
/*
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
*/
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.Paths;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import java.security.Principal;

@Service
public class APIService {

  @Autowired
  APIRepository repository;

  @Autowired
  BlueprintService blueprintService;

  @Autowired
  ProgramService programService;

  ApplicationHome home = new ApplicationHome(APIService.class);
  Logger logger = LoggerFactory.getLogger(APIService.class);

	public APIService() {
	}

	public List<APIEntity> findAll() {
	  return (repository.findAll());
	}

	public Optional<APIEntity> findById (String id) {
    return (repository.findById(id));
	}

	public APIEntity create (APIEntity api) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

	  api.setId(UUID.randomUUID().toString());
    api.setUpdateTime(new Date());
    api.setOwner(((User) auth.getPrincipal()).getUsername());

    api = repository.saveAndFlush(api);

    return api;
	}

	public APIEntity update (APIEntity api) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    api.setUpdateTime(new Date());
    api.setOwner(((User) auth.getPrincipal()).getUsername());

    api = repository.saveAndFlush(api);

    return api;
	}

	public void delete (APIEntity api) {
    repository.delete(api);
	}

	public APIEntity setEnabled(APIEntity api, boolean enabled) {

    api.setEnabled(enabled);
    api = repository.saveAndFlush(api);

    return api;
	}

  /**
   * Execute the API with the given name
   */
  /*public APIResult execute(String name, String data, HttpServletRequest request) {
    APIResult result = new APIResult();

    Optional<APIEntity> api = repository.findByName(name);

    if (!api.isPresent()) {
      result.setResult(404, HttpStatus.NOT_FOUND, "API '"+name+"' not found");
      return(result);
    }

	  result = execute(api.get(), data, request);

    return(result);
  }*/

  /**
   * Search API by method and URI
   */
  public Optional<APIEntity> searchByMethodAndURI(String method, String uri) {
    List<APIEntity> list = findAll();
    APIEntity apiFound = null;

    for (APIEntity api : list) {
      // Check method
      if (!api.getMethod().equals(method))
        continue;
        
      // Check URI
      if (api.mapURI(uri) == null)
        continue;
        
      apiFound = api;
      break;
    }

    return Optional.ofNullable(apiFound);
  }

  /**
   * Execute the API with the given method and URI (string to be parsed)
   */
   
  public APIResult executeWithData(String method, String uri, String data, HttpServletRequest request) {
    APIResult result = new APIResult();
    JSONParser jsonParser = new JSONParser();
    JSONObject jdata;

    try {
      logger.info("Parsing data...");
      Map<String, Object> map = data != null ? Utils.jsonToMap(data) : null;

      return (execute(method, uri, map, request));

    } catch (ParseException e) {
      result.setResult(1, HttpStatus.BAD_REQUEST, e.getMessage());
      return(result);
    }
  }

  /**
   * Execute the API with the given method and URI (mapped parameters)
   */
  public APIResult execute(String method, String uri, Map<String, Object> actual, HttpServletRequest request) {
    APIResult result = new APIResult();
    Map<String, Object> params = new HashMap<>();
    
    // Search matching api
    Optional<APIEntity> apiOpt = searchByMethodAndURI(method, uri);
    
    if (!apiOpt.isPresent()) {
      result.setResult(404, HttpStatus.NOT_FOUND, "Mapping not found");
      return(result);
    }
    
    APIEntity api = apiOpt.get();
    
    // Add data
    if (actual != null) {
      for (Map.Entry<String, Object> p : actual.entrySet()) {
          params.put(p.getKey(), p.getValue());
      }
    }
    
    // Get path parameters
    Map<String, String> pathParams = api.mapURI(uri);
    
    // Add path parameters
    if (pathParams != null) {
      for (Map.Entry<String, String> p : pathParams.entrySet()) {
          params.put(p.getKey(), p.getValue());
      }
    }
    
    logger.info("Executing API "+api.getName());

	  result = execute(api, params, request);

    return(result);
  }

  /**
   * Execute the blueprint associated to APIEntity api
   */
  public APIResult execute(APIEntity api, /*String data*/Map<String, Object> actual, HttpServletRequest request) {
    APIResult result = new APIResult();

    if (!api.getEnabled()) {
      result.setResult(1, HttpStatus.FORBIDDEN, "API is disabled");
      return result;
    }

    ProgramEntity program = api.getBlueprint().getProgram();
    String method = api.getBlueprint().getMethod();

    logger.info("Executing "+program.getName()+"."+method);

    // Execute

    if (programService.run(program, method, actual, api.getName(), request)) {
      result.setResponse(program.getHTTPResponse());
      result.setResult(program.getResult(), HttpStatus.OK, "Success");
    } else {
      result.setResponse(program.getMessage());

      switch (program.getResult()) {
        case ProgramEntity.METHOD_NOT_FOUND:
          result.setResult(program.getResult(), HttpStatus.NOT_FOUND, program.getMessage());
          break;

        case ProgramEntity.BAD_INPUT:
          result.setResult(program.getResult(), HttpStatus.BAD_REQUEST, program.getMessage());
          break;

        default:
          result.setResult(-1, HttpStatus.INTERNAL_SERVER_ERROR, program.getMessage());
          break;
      }
    }

    return result;
  }

}
