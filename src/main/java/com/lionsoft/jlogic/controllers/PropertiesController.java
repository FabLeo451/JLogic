package com.lionsoft.jlogic;

import java.util.concurrent.atomic.AtomicLong;
import java.lang.*;
import java.io.FileNotFoundException;
import java.util.Date;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Properties;
import java.util.Enumeration;
import org.apache.commons.lang3.StringUtils;

@RestController
public class PropertiesController {

  Logger logger = LoggerFactory.getLogger(PropertiesController.class);
	
	// GET /properties
	@GetMapping(value = "/properties", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Properties> get() {
	  GlobalProperties gProp = GlobalProperties.getInstance();
		return new ResponseEntity<>(gProp.getProperties(), HttpStatus.OK);
	}

  // POST /properties
	@PostMapping(value = "/properties", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Properties> create(@RequestBody Properties prop) {
	  logger.info("Updating properties");
	  
	  GlobalProperties gProp = GlobalProperties.getInstance();
	  Lock lock = gProp.lockWrite();
    gProp.merge(prop);
    gProp.save();
	  lock.unlock();

		return new ResponseEntity<>(gProp.getProperties(), HttpStatus.OK);
	}
	
	// DELETE /property/{key}
	@DeleteMapping(value = "/property/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Properties> delete(@PathVariable("key") String key) {
	  logger.info("Deleting property "+key);
	  
	  GlobalProperties gProp = GlobalProperties.getInstance();
	  Lock lock = gProp.lockWrite();
    gProp.remove(key);
    gProp.save();
	  lock.unlock();

		return new ResponseEntity<>(gProp.getProperties(), HttpStatus.OK);
	}
	
	// DELETE /properties
	@DeleteMapping(value = "/properties", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Properties> deleteProperties(@RequestBody String data) {
	  JSONArray ja;
	  
    try {
      ja = new JSONArray(data);
    }
    catch (JSONException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
    }

	  GlobalProperties gProp = GlobalProperties.getInstance();
	  Lock lock = gProp.lockWrite();
	  
	  for (int i=0; i<ja.length(); i++) {
	    String key = ja.getString(i);
	    logger.info("Deleting property "+key);
      gProp.remove(key);
    }
    
    gProp.save();
	  lock.unlock();

		return new ResponseEntity<>(gProp.getProperties(), HttpStatus.OK);
	}
}
