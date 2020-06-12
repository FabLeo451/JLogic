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

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

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

  // GET /blueprint/{id}
	@GetMapping(value = "/blueprint/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> getById(@PathVariable("id") String id) {
	  String result = "";
	  Optional<BlueprintEntity> blueprint = blueprintService.findById(id);
	  
	  if (blueprint.isPresent()) {
      String programId = blueprint.get().getProgram().getId();
      String programName = blueprint.get().getProgram().getName();
      String filename = blueprint.get().getFilename();

      System.out.println("Loading "+filename);
      
      try {
        String content = new String (Files.readAllBytes(Paths.get(filename)));

        // Add data
        try {
          JSONObject jo = new JSONObject(content);
          
          jo.put("id", id);
          jo.put("programId", programId);
          jo.put("programName", programName);
          
          result = jo.toString();

        }
        catch (JSONException e) {
          throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
      }
      catch (IOException e) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
      }
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
	    if (blueprint.get().isMain())
	      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Can't delete Main blueprint");
	    
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
	  
	  if (blueprint.isPresent()) {
      try {
        JSONObject jo = new JSONObject(content);
        
        blueprint.get().setName((String) jo.get("name"));
        
        logger.info("Updating blueprint "+blueprint.get().toString());

        blueprintService.update(blueprint.get(), content);
      } catch (JSONException e) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
      }     
	  } else {
	    return new ResponseEntity<>("", HttpStatus.NOT_FOUND);
	  }            
	
  	return (new ResponseEntity<>(tree.equals("0") ? "" : catalogService.getCatalog().toString(), HttpStatus.OK));
	}
}
