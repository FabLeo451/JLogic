package com.lionsoft.jlogic;

import java.util.concurrent.atomic.AtomicLong;
import java.lang.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.List;

import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.bind.annotation.RequestBody;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/*
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;*/
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.concurrent.locks.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
//@RequestMapping("/blueprint")
public class BlueprintController {

  Logger logger = LoggerFactory.getLogger(BlueprintController.class);

  @Autowired
  BlueprintService blueprintService;

  @Autowired
  CatalogService catalogService;

  @Autowired
  MainService mainService;

  // GET /blueprint/{id}
	@GetMapping(value = "/blueprint/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> getById(@PathVariable("id") String id) {
	  String result = "";
	  Optional<BlueprintEntity> blueprint = blueprintService.findById(id);

	  if (blueprint.isPresent()) {
      JSONObject jo = blueprintService.toJSON(blueprint.get());
      
      if (jo == null)
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Can't get blueprint json.");
        
      result = jo.toString();
      
	  } else {
	    throw new ResponseStatusException(HttpStatus.NOT_FOUND);
	  }

    return (new ResponseEntity<>(result, HttpStatus.OK));
	}

  // DELETE /blueprint/{id}
	@DeleteMapping(value = "/blueprint/{blueprintId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<ProgramEntity> delete(@PathVariable("blueprintId") String blueprintId,
	                                     @RequestParam(value = "tree", defaultValue = "0") String tree) {
	  Optional<BlueprintEntity> blueprint = blueprintService.findById(blueprintId);

	  if (blueprint.isPresent()) {
	    if (blueprint.get().getType() == BlueprintType.MAIN || blueprint.get().getType() == BlueprintType.EVENTS)
	      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Can't delete this blueprint");

      blueprintService.delete(blueprint.get());

	  } else {
	    throw new ResponseStatusException(HttpStatus.NOT_FOUND);
	  }

    return (tree.equals("0") ? null : catalogService.getPrograms());
	}

	// PUT /blueprint/{blueprintId}
	@PutMapping(value = "/blueprint/{blueprintId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<String> update(@PathVariable("blueprintId") String blueprintId,
	                        @RequestParam(value = "tree", defaultValue = "0") String tree,
	                        @RequestBody String content) {

	  Optional<BlueprintEntity> blueprint = blueprintService.findById(blueprintId);
    //System.out.println(content);

	  if (blueprint.isPresent()) {
      try {
        JSONParser jsonParser = new JSONParser();
        JSONObject jo = (JSONObject) jsonParser.parse(content);

        blueprint.get().setName((String) jo.get("name"));

        logger.info("Updating blueprint "+blueprint.get().toString());
        
        Code code = blueprintService.update(blueprint.get(), content);

        if (code != Code.SUCCESS)
          throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, mainService.getErrorMessage(code));
          
      } catch (ParseException e) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
      }
	  } else {
	    return new ResponseEntity<>("", HttpStatus.NOT_FOUND);
	  }

  	return (new ResponseEntity<>(tree.equals("0") ? "" : catalogService.getCatalog().toString(), HttpStatus.OK));
	}
	
	/**
	 * Clone blueprint
   * PUT /blueprint/{blueprintId}/clone
   */
	@PutMapping(value = "/blueprint/{blueprintId}/clone", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<ProgramEntity> clone(@PathVariable("blueprintId") String blueprintId,
	                                 @RequestParam(value = "tree", defaultValue = "0") String tree) {
	  Optional<BlueprintEntity> blueprint = blueprintService.findById(blueprintId);

	  if (blueprint.isPresent()) {
	    if (blueprint.get().getType() == BlueprintType.MAIN || blueprint.get().getType() == BlueprintType.EVENTS)
	      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Can't clone this blueprint");

      logger.info("Cloning blueprint "+blueprint.get().getName());
      
      if (blueprintService.clone(blueprint.get()) == null)
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to clone blueprint");

	  } else {
	    throw new ResponseStatusException(HttpStatus.NOT_FOUND);
	  }

    return (tree.equals("0") ? null : catalogService.getPrograms());
	}

}
