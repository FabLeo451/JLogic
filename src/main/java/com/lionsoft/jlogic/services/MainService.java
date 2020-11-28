package com.lionsoft.jlogic;

import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MainService {
	
	Logger logger = LoggerFactory.getLogger(MainService.class);
	static ApplicationHome home = new ApplicationHome(MainService.class);

	public MainService() {
	}
	
	public String getErrorMessage(Code code) {
	  switch (code) {
      case SUCCESS:
        return("Success");
        
      case ERR_IO:
        return("I/O Error");
        
      case ERR_PARSE:
        return("Parsing error");
        
      // Blueprint errors
      case ERR_BP_GLOBAL_EXISTS:
        return("Global variable already exists");
        
      case ERR_BP_CREATE_GLOBAL:
        return("Can't create global variable");
        
      case ERR_BP_MODIFY_REFERENCED:
        return("Can't modify a referenced variable");
        
      case ERR_BP_UPDATE_VAR:
        return("Can't update variable");
        
      case ERR_BP_GLOBAL_TO_LOCAL:
        return("Can't change a variable from global to local");
  
      default:
	      return("Generic error");
	  }
	}

}
