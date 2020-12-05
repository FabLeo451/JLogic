package com.lionsoft.jlogic;

import java.util.concurrent.atomic.AtomicLong;
import java.lang.*;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.time.Instant;

import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.bind.annotation.RequestBody;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.server.ResponseStatusException;
import javax.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.concurrent.locks.*;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
public class ApiController {

  public String apiExecPrefix = "/api";

  @Autowired
  APIRepository repository;

  @Autowired
  APIService APIService;

  @Autowired
  BlueprintService blueprintService;

  @Autowired
  SessionService sessionService;

  Logger logger = LoggerFactory.getLogger(ApiController.class);

	@GetMapping(value = "/mapping", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<APIEntity> get() {
	  List<APIEntity> list = APIService.findAll();

	  for (APIEntity a: list)
	    System.out.println(a);

		return (APIService.findAll());
	}

  /**
   * Create mapping
   */
	@PostMapping(value = "/mapping", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<APIEntity> create(@RequestBody APIEntity api) {
	  logger.info("Creating API "+api.getName());

    // Check mapping
    Optional<APIEntity> apiOpt = APIService.searchByMethodAndURI(api.getMethod(), api.getPath());

    if (apiOpt.isPresent())
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mapping already present");

    // Check blueprint
    Optional<BlueprintEntity> blueprint = blueprintService.findById(api.getBlueprint().getId());

    if (!blueprint.isPresent())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Blueprint not found: "+api.getBlueprint().getId());

    //api.setBlueprint(blueprint.get());

    APIService.create(api);

		return (repository.findAll());
	}
	
  /**
   * Update mapping
   */
	@PutMapping(value = "/mapping/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<APIEntity> update(@PathVariable("id") String id, @RequestBody APIEntity api) {
    logger.info("Updating API "+api.getName());

    Optional<APIEntity> apiOpt = repository.findById(id);

    if (!apiOpt.isPresent())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "API not found");

    // Check mapping
    apiOpt = APIService.searchByMethodAndURI(api.getMethod(), api.getPath());

    if (apiOpt.isPresent() && apiOpt.get().getId() != id)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mapping already present");

    // Check blueprint
    Optional<BlueprintEntity> blueprint = blueprintService.findById(api.getBlueprint().getId());

    if (!blueprint.isPresent())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Blueprint not found: "+api.getBlueprint().getId());

    //api.setBlueprint(blueprint.get());

    APIService.update(api);

		return (repository.findAll());
	}

	@DeleteMapping(value = "/mapping/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<APIEntity> delete(@PathVariable("id") String id) {
	  logger.info("Deleting API "+id);

    Optional<APIEntity> api = repository.findById(id);

    if (!api.isPresent())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "API not found");

    APIService.delete(api.get());

		return (repository.findAll());
	}

	@PutMapping(value = "/mapping/{id}/enable", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<APIEntity> enableAPI(@PathVariable("id") String id) {

    Optional<APIEntity> api = repository.findById(id);

    if (!api.isPresent())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "API not found");

	  logger.info("Enabling API "+api.get().getName());

    APIService.setEnabled(api.get(), true);

		return (repository.findAll());
	}

	@PutMapping(value = "/mapping/{id}/disable", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<APIEntity> disableAPI(@PathVariable("id") String id) {

    Optional<APIEntity> api = repository.findById(id);

    if (!api.isPresent())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "API not found");

	  logger.info("Disabling API "+api.get().getName());

    APIService.setEnabled(api.get(), false);

		return (repository.findAll());
	}

  /**
   * Execute an API on GET /api/...
   */
	@GetMapping(value = "/api/**", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> executeGET(HttpServletRequest request) {
    logger.info("Executing GET "+request.getRequestURI());
    
    String uri = request.getRequestURI().replaceAll("^/api/", "");
    
    APIResult result = APIService.execute("GET", uri, null, request);

    if (result.getCode() != 0)
      throw new ResponseStatusException(result.getStatus(), result.getMessage());

		return new ResponseEntity<>(result.getResponse(), result.getStatus());
	}

  /**
   * Execute an API on POST /api/...
   */
  @PostMapping(value = "/api/**", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> executePOST(HttpServletRequest request,
                                            @RequestBody String data) {
	  logger.info("Executing POST "+request.getRequestURI());
	  
	  String uri = request.getRequestURI().replaceAll("^/api/", "");

    APIResult result = APIService.executeWithData("POST", uri, data, request);

    if (result.getCode() != 0)
      throw new ResponseStatusException(result.getStatus(), result.getMessage());

		return new ResponseEntity<>(result.getResponse(), result.getStatus());
	}

  /**
   * Execute an API on PUT /api/...
   */
  @PutMapping(value = "/api/**", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> executePUT(HttpServletRequest request,
                                            @RequestBody String data) {
	  logger.info("Executing PUT "+request.getRequestURI());
	  
	  String uri = request.getRequestURI().replaceAll("^/api/", "");

    APIResult result = APIService.executeWithData("PUT", uri, data, request);

    if (result.getCode() != 0)
      throw new ResponseStatusException(result.getStatus(), result.getMessage());

		return new ResponseEntity<>(result.getResponse(), result.getStatus());
	}

  /**
   * Execute an API on DELETE /api/...
   */
  @DeleteMapping(value = "/api/**", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> executeDELETE(HttpServletRequest request) {
	  logger.info("Executing DELETE "+request.getRequestURI());
	  
	  String uri = request.getRequestURI().replaceAll("^/api/", "");

    APIResult result = APIService.executeWithData("DELETE", uri, null, request);

    if (result.getCode() != 0)
      throw new ResponseStatusException(result.getStatus(), result.getMessage());

		return new ResponseEntity<>(result.getResponse(), result.getStatus());
	}
}
