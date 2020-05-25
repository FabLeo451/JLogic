package com.lionsoft.jlogic;

import org.springframework.boot.system.ApplicationHome;
import java.io.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.simple.*;
import org.json.simple.JSONObject;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import java.security.Principal;

@Service
public class CatalogService {

  @Autowired
  ProgramRepository programRepository;
  
  @Autowired
  BlueprintService blueprintService;
	
	Logger logger = LoggerFactory.getLogger(CatalogService.class);
	static ApplicationHome home = new ApplicationHome(CatalogService.class);

	public CatalogService() {

	}

  public JSONObject getCatalog() {
    JSONObject jo = new JSONObject();
    JSONObject jindex = new JSONObject();
    
    jo.put("_index", jindex);
    
    List<ProgramEntity> programs = programRepository.findAll();
    
    for (ProgramEntity p : programs) {
      //System.out.println(p.toString());
      
      JSONObject jprogram = new JSONObject();
      jprogram.put("name", p.getName());
      jprogram.put("type", 1);
      jindex.put(p.getId(), jprogram);
      
      jprogram = new JSONObject();
      JSONObject jchildren = new JSONObject();
      
      jprogram.put("id", p.getName());
      jprogram.put("name", p.getName());
      jprogram.put("type", 1);
      jprogram.put("status", p.getStatus().name());
      jprogram.put("updateTime", p.getUpdateTime() != null ? p.getUpdateTime().getTime()/1000 : null);
      jprogram.put("buildTime", p.getBuildTime() != null ? p.getBuildTime().getTime()/1000 : null);
      jprogram.put("children", jchildren);
      
      if (p.getBlueprints() != null) {
        for (BlueprintEntity b : p.getBlueprints()) {
          //System.out.println("   "+b.toString());
          
          JSONObject jbp = new JSONObject();
          jbp.put("id", b.getId());
          jbp.put("name", b.getName());
          jbp.put("type", 2);
          jbp.put("updateTime", b.getUpdateTime() != null ? b.getUpdateTime().getTime()/1000 : null);
          jchildren.put(b.getId(), jbp);
        }
      }
      
      jo.put(p.getId(), jprogram);
    }
    
    return (jo);
  }
}
