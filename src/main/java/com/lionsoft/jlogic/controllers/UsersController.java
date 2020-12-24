package com.lionsoft.jlogic;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Autowired;
import java.lang.*;
import java.io.FileNotFoundException;
import java.util.*;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@RestController
public class UsersController {

    Logger logger = LoggerFactory.getLogger(UsersController.class);
  
	@Autowired
	private UserRepository userRepository;
  
	@Autowired
	private UserService userService;
  
	// GET /users
	@GetMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<User> get() {
		return userRepository.findAll();
	}

  // POST /user
	@PostMapping(value = "/user", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> create(@RequestBody User user) {
	  logger.info("Creating user "+user.getUsername());
	  
	  Optional<User> userCheck = userRepository.findByUsername(user.getUsername());

    if (userCheck.isPresent())
      return new ResponseEntity<>("Existing user", HttpStatus.BAD_REQUEST);
      
    // Encode plain password
    user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));
    
    // Re-set role id to set single authorities
    user.setRoleSet(user.getRoleSet());
    
		userRepository.save(user);
		  	  
		return new ResponseEntity<>("", HttpStatus.OK);
	}

  // PUT /user/{id}
  // Administrator updates other user's profile
	@PutMapping(value = "/user/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> update(HttpServletRequest request, @PathVariable("id") Long id, @RequestBody User user) {
	  logger.info("Updating user "+user.getUsername());
	  
	  Optional<User> userDB;
	  /*
	  if (id.equals("me")) {
	    userDB = userRepository.findByUsername(request.getUserPrincipal().getName());
	    user.setId(userDB.get().getId());
	  }
	  else*/
	    userDB = userRepository.findById(id);

    if (!userDB.isPresent())
      return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);

    if (userDB.get().getReserved())
      return new ResponseEntity<>("Reserved user", HttpStatus.BAD_REQUEST);

    // Password remais the same
    user.setPassword(userDB.get().getPassword());
        
		userRepository.save(user);
		  	  
		return new ResponseEntity<>("", HttpStatus.OK);
	}

  // PUT /user
  // Current user updates his own profile
	@PutMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> updateMe(HttpServletRequest request, @RequestBody User user) {
	  logger.info("Updating user "+user.getUsername());
	  
	  Optional<User> userDB;

    userDB = userRepository.findByUsername(request.getUserPrincipal().getName());

    if (!userDB.isPresent())
      return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);

    if (userDB.get().getReserved())
      return new ResponseEntity<>("Reserved user", HttpStatus.BAD_REQUEST);

    user.setId(userDB.get().getId());

    // Password remais the same
    user.setPassword(userDB.get().getPassword());
    
    if (user.getRoleSet() == -1)
      user.setRoleSet(userDB.get().getRoleSet());
        
		userRepository.save(user);
		  	  
		return new ResponseEntity<>("", HttpStatus.OK);
	}

  // PUT /password
  // Current user updates his own profile
	@PutMapping(value = "/password", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> chabgePassword(HttpServletRequest request, @RequestBody User user) {
        logger.info("Updating password of user "+request.getUserPrincipal().getName());

        Optional<User> userDB = userRepository.findByUsername(request.getUserPrincipal().getName());

        if (user.getPassword() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing password");

        // Password remais the same
        userDB.get().setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));
        userRepository.save(userDB.get());
              
        return new ResponseEntity<>("", HttpStatus.OK);
	}
	
	/**
     * Lock user
     */
	@PostMapping(value = "/user/{id}/lock", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> lock(@PathVariable("id") Long id) {
        Optional<User> user = userRepository.findById(id);

        if (!user.isPresent())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: "+id);

        if (user.get().getReserved())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User "+user.get().getUsername()+" is reserved");
        
        logger.info("Locking user "+user.get().getUsername());
        
        userService.setLocked(user.get(), true);

        return new ResponseEntity<>("", HttpStatus.OK);
	}
	
	/**
     * Unlock user
     */
	@PostMapping(value = "/user/{id}/unlock", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> unlock(@PathVariable("id") Long id) {
        Optional<User> user = userRepository.findById(id);

        if (!user.isPresent())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: "+id);

        if (user.get().getReserved())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User "+user.get().getUsername()+" is reserved");
        
        logger.info("Unlocking user "+user.get().getUsername());
        
        userService.setLocked(user.get(), false);

        return new ResponseEntity<>("", HttpStatus.OK);
	}
}
