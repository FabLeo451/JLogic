package com.lionsoft.jlogic;

import org.springframework.boot.system.ApplicationHome;
import java.util.concurrent.atomic.AtomicLong;
import java.lang.*;
import java.util.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
//import java.util.Date;
//import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.bind.annotation.RequestBody;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

import java.util.concurrent.locks.*;
//import java.util.Properties;
//import java.util.Optional;

@RestController
public class ProgramController {

  Logger logger = LoggerFactory.getLogger(ProgramController.class);
  static ApplicationHome home = new ApplicationHome(ProgramController.class);

  @Autowired
  ProgramRepository programRepository;

  @Autowired
  ProgramService programService;

  @Autowired
  BlueprintService blueprintService;

  @Autowired
  CatalogService catalogService;

	// GET /programs
	@GetMapping(value = "/programs")
	public List<ProgramEntity> getPrograms() {
		return catalogService.getPrograms();
	}

	// GET /program/{id}
	@GetMapping(value = "/program/{id}")
	public ProgramEntity get(@PathVariable("id") String id) {
	  Optional<ProgramEntity> program = programService.findById(id);

	  if (!program.isPresent())
	    throw new ResponseStatusException(HttpStatus.NOT_FOUND);

		return program.get();
	}
/*
  private String createProgram(String parentId, String name) {

    logger.info("Creating program "+name);

	  ProgramEntity program = programService.create(name);

	  if (program != null) {
	    logger.info("Created "+program.toString());
	    BlueprintEntity blueprint = blueprintService.create(program, BlueprintType.MAIN, "Main");
	    //program.addBlueprint(blueprint);
	    programRepository.refresh(program);
	  }

    return (catalogService.getCatalog().toString());
  }*/

	// PUT /program
	@PutMapping(value = "/program", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<ProgramEntity> put(@RequestBody String data) {
	  String response;
	  JSONObject jo;
/*
    try {
      jo = new JSONObject(data);
    }
    catch (JSONException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
    }*/
    JSONParser jsonParser = new JSONParser();

    try {
      jo = (JSONObject) jsonParser.parse(data);
    } catch (ParseException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
    }

    //response = createProgram(null, jo.optString("name"));
    logger.info("Creating program "+ (String) jo.get("name"));

	  ProgramEntity program = programService.create( (String) jo.get("name"));

		return (catalogService.getPrograms());
	}

	// PUT /program/{programId}/blueprint/{name}
	@PutMapping(value = "/program/{programId}/blueprint/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> addBlueprint(@PathVariable("programId") String programId,
	                                           @PathVariable("name") String name,
	                                           @RequestParam(value = "tree", defaultValue = "0") String tree) {
	  BlueprintEntity blueprint = null;
	  Optional<ProgramEntity> program = programService.findById(programId);

	  if (program.isPresent()) {
	    blueprint = blueprintService.create(program.get(), BlueprintType.GENERIC, name);

	    if (blueprint != null) {
	      //logger.info("Created "+blueprint.toString());
	      programRepository.refresh(program.get());
	    } else {
	      logger.error("Can't create blueprint "+name);
	      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Can't create blueprint "+name);
	    }
	  } else {
	    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found: "+programId);
	  }

		return new ResponseEntity<>("{\"id\":\""+blueprint.getId()+"\"}", HttpStatus.OK);
	}
	
	/**
	 * Import a blueprint
	 * PUT /program/{programId}/import/blueprint
	 */
	@PutMapping(value = "/program/{programId}/import/blueprint", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<ProgramEntity> importBlueprint(@PathVariable("programId") String programId,
	                                           @RequestBody String content,
	                                           @RequestParam(value = "tree", defaultValue = "0") String tree) {
	  Optional<ProgramEntity> program = programService.findById(programId);
	  
	  if (!program.isPresent())
	    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found: "+programId);
	    
	  logger.info("Importing blueprint into "+program.get().getName()+"...");
	    
	  BlueprintEntity blueprint = programService.importBlueprint(program.get(), content);
	  
	  if (blueprint == null)
	    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Can't import blueprint");
	    
	  logger.info("Successfully imported "+blueprint.toString());
	  
	  return(tree.equals("0") ? null : catalogService.getPrograms());
	}

	// PUT /program/{id}/rename/{name}
	@PutMapping(value = "/program/{id}/rename/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<ProgramEntity> rename(@PathVariable("id") String id,
	                                     @PathVariable("name") String name,
	                                     @RequestParam(value = "tree", defaultValue = "0") String tree) {
	  Optional<ProgramEntity> program = programService.findById(id);

	  if (!program.isPresent())
	    throw new ResponseStatusException(HttpStatus.NOT_FOUND);

	  programService.rename(program.get(), name);

		return (tree.equals("0") ? null : catalogService.getPrograms());
	}

	// DELETE /program/{id}
	@DeleteMapping(value = "/program/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<ProgramEntity> delete(@PathVariable("id") String id) {
	  Optional<ProgramEntity> program = programService.findById(id);

	  if (!program.isPresent())
	    throw new ResponseStatusException(HttpStatus.NOT_FOUND);

	  programService.delete(program.get());

		return catalogService.getPrograms();
	}

	// POST /program/{id}/compile
	@PostMapping(value = "/program/{id}/compile", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> compile(@PathVariable("id") String id) {

	  HttpStatus responseStatus = HttpStatus.OK;
	  JSONObject jresponse = new JSONObject();

	  Optional<ProgramEntity> program = programService.findById(id);

	  if (program.isPresent()) {
	      logger.info("Compiling program "+program.get().getName());

        if (programService.compile(program.get())) {
          logger.info(program.get().getMessage());
        }
        else {
          responseStatus = HttpStatus.INTERNAL_SERVER_ERROR;
          logger.error(program.get().getMessage());
        }

        jresponse.put("status", program.get().getStatus().name());
        jresponse.put("message", program.get().getMessage());
        jresponse.put("output", program.get().getOutput());
	  } else {
	    return new ResponseEntity<>("", HttpStatus.NOT_FOUND);
	  }

	  return new ResponseEntity<>(jresponse.toString(), responseStatus);
	}

	// POST /program/{id}/jar
	@PostMapping(value = "/program/{id}/jar")
	public List<ProgramEntity> createJAR(@PathVariable("id") String id) {

	  HttpStatus responseStatus = HttpStatus.OK;
	  JSONObject jresponse = new JSONObject();

	  Optional<ProgramEntity> program = programService.findById(id);

	  if (!program.isPresent())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found");

    if (program.get().getStatus() != ProgramStatus.COMPILED)
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Program not compiled");

    if (program.get().createJAR()) {
      logger.info(program.get().getMessage());
    }
    else {
      logger.error(program.get().getMessage());
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, program.get().getMessage());
    }

    jresponse.put("status", program.get().getStatus().name());
    jresponse.put("message", program.get().getMessage());
    jresponse.put("output", program.get().getOutput());

		return (catalogService.getPrograms());
	}

	// GET /program/{id}/java
	@GetMapping(value = "/program/{id}/java")
	public ResponseEntity<String> getJava(@PathVariable("id") String id) {

	  String javaCode = "";

	  Optional<ProgramEntity> program = programService.findById(id);

	  if (program.isPresent()) {
	    javaCode = program.get().getJava();

	    if (javaCode == null)
	      return new ResponseEntity<>("Java code not found", HttpStatus.NOT_FOUND);
	  } else {
	    return new ResponseEntity<>("", HttpStatus.NOT_FOUND);
	  }

		return new ResponseEntity<>(javaCode, HttpStatus.OK);
	}

	// GET /program/{id}/jar
	@GetMapping(value = "/program/{id}/jar")
	public ResponseEntity<Resource> getJAR(@PathVariable("id") String id) {

	  Optional<ProgramEntity> program = programService.findById(id);

	  if (!program.isPresent())
	    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found");

    File file = new File(program.get().getJARFilename());
    ByteArrayResource resource;

    try {
      Path path = Paths.get(/*file.getAbsolutePath()*/program.get().getJARFilename());
      resource = new ByteArrayResource(Files.readAllBytes(path));
    } catch (FileNotFoundException e) {
      logger.error ("Not found: "+program.get().getJARFilename());
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
    }

    //Response.AppendHeader("content-disposition", "attachment; filename=\"" + fileName +"\"");
    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Type", "application/octet-stream");
    headers.set("Content-Disposition", "attachment; filename=\""+program.get().getName()+".jar\"");

    return ResponseEntity.ok()
            .headers(headers)
            .contentLength(file.length())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource);
	}

	// PUT /program/{id}/clean
	/*
	@PutMapping(value = "/program/{id}/clean")
	public ResponseEntity<String> clean(@PathVariable("id") String id) {

	  Optional<ProgramEntity> program = programService.findById(id);

	  if (program.isPresent()) {
	    program.get().clean();
	  } else {
	    return new ResponseEntity<>("", HttpStatus.NOT_FOUND);
	  }

		return new ResponseEntity<>("{\"status\":0}", HttpStatus.OK);
	}*/

	// POST /program/{id}/run/{method}
	@PostMapping(value = "/program/{id}/run/{method}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> run(HttpServletRequest request,
                                    @PathVariable("id") String id,
	                                  @PathVariable("method") String method,
	                                  @RequestBody String data) {

	  String outData = "";

	  Optional<ProgramEntity> program = programService.findById(id);

	  if (program.isPresent()) {
	      Optional<BlueprintEntity> blueprint = blueprintService.findByNameAndProgram(method, program.get());

	      if (blueprint.isPresent()) {
	          if (programService.run(program.get(), blueprint.get().getMethod(), data, null, request)) {
	            logger.info(program.get().getName()+"."+method+" successfully executed");
	            outData = program.get().getOutput();
	          }
	          else {
	            logger.error("Blueprint "+program.get().getName()+"."+method+" error: "+program.get().getResult());
	            outData = program.get().getOutput();

	            switch (program.get().getResult()) {
	              case ProgramEntity.METHOD_NOT_FOUND:
	                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Method Not Found");

	              default:
	                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, /*program.get().getMessage()*/program.get().getOutput());
	            }
	          }
	      } else {
	        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Method Not Found: "+method);
	      }

	  } else {
	    return new ResponseEntity<>("", HttpStatus.NOT_FOUND);
	  }

		return new ResponseEntity<>(outData, HttpStatus.OK);
	}

	// GET /program/{id}/status
	@GetMapping(value = "/program/{id}/status", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> getStatus(@PathVariable("id") String id) {
	  String outData = "";

	  Optional<ProgramEntity> program = programService.findById(id);

	  if (program.isPresent()) {
	    outData = "{\"status\":\""+program.get().getStatus()/*.ordinal()*/ + "\"}";
	  } else {
	    return new ResponseEntity<>("", HttpStatus.NOT_FOUND);
	  }

		return new ResponseEntity<>(outData, HttpStatus.OK);
	}

	// GET /program/{id}/index
	@GetMapping(value = "/program/{id}/index", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> getIndex(@PathVariable("id") String id) {
	  Optional<ProgramEntity> program = programService.findById(id);

	  if (!program.isPresent())
	    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found.");

    JSONObject jo = programService.getIndex(program.get());
    
    if (jo == null)
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to get program index.");

		return new ResponseEntity<>(jo.toString(), HttpStatus.OK);
	}

	@GetMapping(value = "/program/{id}/properties", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Properties> getProperties(@PathVariable("id") String id) {
	  Optional<ProgramEntity> program = programService.findById(id);

	  if (!program.isPresent())
	    return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);

		return new ResponseEntity<>(program.get().getProperties(), HttpStatus.OK);
	}

	@PostMapping(value = "/program/{id}/properties", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Properties> create(@PathVariable("id") String id, @RequestBody Properties prop) {
	  PropertiesManager pm = null;
	  Optional<ProgramEntity> program = programService.findById(id);

	  if (!program.isPresent())
	    return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);

    pm = program.get().getPropertiesManager();
    pm.merge(prop);
    pm.save();

		return new ResponseEntity<>(pm.getProperties(), HttpStatus.OK);
	}

	// DELETE /properties
	@DeleteMapping(value = "/program/{id}/properties", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Properties> deleteProperties(@PathVariable("id") String id, @RequestBody String data) {
    JSONArray ja = null;
	  PropertiesManager pm = null;
	  Optional<ProgramEntity> program = programService.findById(id);

	  if (!program.isPresent())
	    return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
/*
    try {
      ja = new JSONArray(data);
    }
    catch (JSONException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
    }
*/
    JSONParser jsonParser = new JSONParser();
    try {
      ja = (JSONArray) jsonParser.parse(data);
    } catch (ParseException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
    }

    pm = program.get().getPropertiesManager();

    for (int i=0; i<ja.size(); i++) {
      String key = (String) ja.get(i);
      logger.info("Deleting property "+key);
      pm.remove(key);
    }

    pm.save();

		return new ResponseEntity<>(pm.getProperties(), HttpStatus.OK);
	}

	// PUT /program/variable
	@PutMapping(value = "/program/{id}/variable", produces = MediaType.APPLICATION_JSON_VALUE)
	public Variable createVariable(@PathVariable("id") String id, @RequestBody Variable v) {
	  Optional<ProgramEntity> program = programService.findById(id);

	  if (!program.isPresent())
	    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found.");

	  logger.info("Creating variable "+v.getName()+" for program "+program.get().getName());

	  Variable newVar = programService.addVariable(program.get(), v);

	  if (newVar == null)
	    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can't create variable.");

		return newVar;
	}

	// POST /program/variable
	@PostMapping(value = "/program/{id}/variable", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> modifyVariable(@PathVariable("id") String id, @RequestBody Variable v) {
	  Optional<ProgramEntity> program = programService.findById(id);

	  if (!program.isPresent())
	    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found.");

	  logger.info("Updating variable "+v.getName()+" for program "+program.get().getName());

	  if (!program.get().hasVariable(v.getName()))
	    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Variable not found.");

	  if (!programService.updateVariable(program.get(), v))
	    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot update variable.");

		return new ResponseEntity<>(null, HttpStatus.OK);
	}

	// POST /program/variable/rename
	@PostMapping(value = "/program/{id}/variable/{oldName}/rename/{newName}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> modifyVariable(@PathVariable("id") String id, @PathVariable("oldName") String oldName, @PathVariable("newName") String newName) {
	  Optional<ProgramEntity> program = programService.findById(id);

	  if (!program.isPresent())
	    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found.");

	  logger.info("Renaming variable "+oldName+" to "+newName+" for program "+program.get().getName());

	  if (!program.get().hasVariable(oldName))
	    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Variable not found.");

	  if (!programService.renameVariable(program.get(), oldName, newName))
	    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot rename variable.");

		return new ResponseEntity<>(null, HttpStatus.OK);
	}

	// DELETE /program/variable
	@DeleteMapping(value = "/program/{id}/variable/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> deleteVariable(@PathVariable("id") String id, @PathVariable("name") String name) {
	  Optional<ProgramEntity> program = programService.findById(id);

	  if (!program.isPresent())
	    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found.");

	  logger.info("Deleting variable "+name+" from program "+program.get().getName());

	  if (!programService.deleteVariable(program.get(), name))
	    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, programService.getMessage());

		return new ResponseEntity<>(null, HttpStatus.OK);
	}

	/**
   * Describe Program.class
   */
	@GetMapping(value = "/program/{id}/class/desc", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> descClass(@PathVariable("id") String id) {
	  Optional<ProgramEntity> program = programService.findById(id);

	  if (!program.isPresent())
	    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found.");

    JSONObject jo = programService.getIndex(program.get());

		return new ResponseEntity<>(jo != null ? jo.toString() : null, jo != null ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/**
   * GET blueprint by internal id
   */
	@GetMapping(value = "/program/{id}/blueprint/{internalId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> descClass(@PathVariable("id") String id, @PathVariable("internalId") int internalId) {
	  Optional<ProgramEntity> program = programService.findById(id);

	  if (!program.isPresent())
	    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found.");

    BlueprintEntity b = programService.getBlueprint(program.get(), internalId);
    
    if (b == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Blueprint not found.");
      
    JSONObject jo = blueprintService.toJSON(b);
    
    if (jo == null)
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Can't get blueprint json.");

		return new ResponseEntity<>(jo.toString(), HttpStatus.OK);
	}

	/**
   * Export program
   */
	@GetMapping(value = "/program/{id}/export", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Resource> export(@PathVariable("id") String id) {
	  Optional<ProgramEntity> program = programService.findById(id);

	  if (!program.isPresent())
	    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found: "+id);

    // Pack files
    logger.info("Packing "+program.get().getName());
    
    JSONObject jo = null;
    String packFile = programService.pack(program.get());
    
    if (packFile == null)
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to pack progam for export.");
      
    // Export zip file
    File file = new File(packFile);
    ByteArrayResource resource;

    try {
      Path path = Paths.get(packFile);
      resource = new ByteArrayResource(Files.readAllBytes(path));
    } catch (FileNotFoundException e) {
      logger.error ("Not found: "+packFile);
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
    }

    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Type", "application/octet-stream");
    headers.set("Content-Disposition", "attachment; filename=\""+file.getName()+"\"");

    return ResponseEntity.ok()
            .headers(headers)
            .contentLength(file.length())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource);
	}
	
	/**
   * Import program
   */
	@PostMapping(value = "/program/import", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<ProgramEntity> importProgram(HttpServletRequest request,
	                                         @RequestParam(value = "tree", defaultValue = "0") String tree) {
    String importId = UUID.randomUUID().toString();
    String zipFile = programService.getTempDirectory()+"/import-"+importId+".zip";
    
    logger.info("Saving zip file to " + zipFile + " ...");
    
    try {
      Files.copy(request.getInputStream(), Paths.get(zipFile));
    } catch (IOException e) {
      logger.error ("Unable to write file "+zipFile);
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
    }
     
    //logger.info("Importing program ...");
    
    ProgramEntity program = programService.importProgramFromFile(importId, zipFile);
    
    return (tree.equals("0") ? null : catalogService.getPrograms());
	}
}
