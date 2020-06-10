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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.concurrent.locks.*;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
public class ApiController {

  @Autowired
  APIRepository repository;
  
  @Autowired
  APIService APIService;
  
  @Autowired
  BlueprintService blueprintService;

  Logger logger = LoggerFactory.getLogger(ApiController.class);

	@GetMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<APIEntity> get() {
	  List<APIEntity> list = APIService.findAll();
	  
	  for (APIEntity a: list)
	    System.out.println(a);
	    
		return (APIService.findAll());
	}
	
	@PostMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<APIEntity> create(@RequestBody APIEntity api) {
	  logger.info("Creating API "+api.getName());

    Optional<APIEntity> apiCheck = repository.findByName(api.getName());
    
    if (apiCheck.isPresent())
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Existing API");

    Optional<BlueprintEntity> blueprint = blueprintService.findById(api.getBlueprint().getId());
    
    if (!blueprint.isPresent())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Blueprint not found: "+api.getBlueprint().getId());
  
    //api.setBlueprint(blueprint.get());
    
    APIService.create(api);

		return (repository.findAll());
	}
	
	@PutMapping(value = "/api/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<APIEntity> edit(@PathVariable("id") String id, @RequestBody APIEntity api) {
    logger.info("Updating API "+api.getName());
	  
    Optional<APIEntity> apiCheck = repository.findById(id);
    
    if (!apiCheck.isPresent())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "API not found");

    apiCheck = repository.findByName(api.getName());
    
    if (apiCheck.isPresent() && (!apiCheck.get().getId().equals(id)))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Existing API with same name");

    Optional<BlueprintEntity> blueprint = blueprintService.findById(api.getBlueprint().getId());
    
    if (!blueprint.isPresent())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Blueprint not found: "+api.getBlueprint().getId());
  
    //api.setBlueprint(blueprint.get());
    
    APIService.update(api);

		return (repository.findAll());
	}
	
	@DeleteMapping(value = "/api/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<APIEntity> delete(@PathVariable("id") String id) {
	  logger.info("Deleting API "+id);
	  
    Optional<APIEntity> api = repository.findById(id);
    
    if (!api.isPresent())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "API not found");
    
    APIService.delete(api.get());

		return (repository.findAll());
	}

	@GetMapping(value = "/api/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> execute(HttpServletRequest request, @PathVariable("name") String name) {
	  logger.info("Executing API "+name);

    Optional<APIEntity> api = repository.findByName(name);
    
    if (!api.isPresent())
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "API not found: "+name);
      
    // Check if enabled
    
    if (!api.get().getEnabled())
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "API is disabled");

    ProgramEntity program = api.get().getBlueprint().getProgram();
    String method = api.get().getBlueprint().getMethod();
	  String outData = "";
	  HttpStatus status = HttpStatus.OK;
	  
	  // Get parameters
	  
    //Map<String, String[]> params = request.getParameterMap();
    
    logger.info("Executing "+program.getName()+"."+method);
	  	    
    // Execute
    
    if (!program.run(method, null, name, request)) {
      outData = program.getOutput();
      
      switch (program.getResult()) {
        case ProgramEntity.METHOD_NOT_FOUND:
          throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Method not found: "+method);
          //break;
          
        default:
          throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, outData);
          //break;
      }
    }
	      
    outData = program.getHTTPResponse();
    status = HttpStatus.valueOf(program.getHTTPStatus());
	        	   
		return new ResponseEntity<>(outData, status);
	}
	
	@PutMapping(value = "/api/{id}/enable", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<APIEntity> enableAPI(@PathVariable("id") String id) {
	  
    Optional<APIEntity> api = repository.findById(id);
    
    if (!api.isPresent())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "API not found");
    
	  logger.info("Enabling API "+api.get().getName());
	  
    APIService.setEnabled(api.get(), true);

		return (repository.findAll());
	}
	
	@PutMapping(value = "/api/{id}/disable", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<APIEntity> disableAPI(@PathVariable("id") String id) {
	  
    Optional<APIEntity> api = repository.findById(id);
    
    if (!api.isPresent())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "API not found");
    
	  logger.info("Disabling API "+api.get().getName());
	  
    APIService.setEnabled(api.get(), false);

		return (repository.findAll());
	}

}
