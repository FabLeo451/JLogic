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
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.Optional;
import java.util.Date;
import java.util.List;
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

  public APIResult execute(APIEntity api, String data, HttpServletRequest request) {
    APIResult result = new APIResult();

    if (!api.getEnabled()) {
      result.setResult(1, HttpStatus.FORBIDDEN, "API is disabled");
      return result;
    }

    ProgramEntity program = api.getBlueprint().getProgram();
    String method = api.getBlueprint().getMethod();

    logger.info("Executing "+program.getName()+"."+method);

    // Execute

    if (program.run(method, data, api.getName(), request)) {
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
