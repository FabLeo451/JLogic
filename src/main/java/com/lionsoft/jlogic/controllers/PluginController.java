package com.lionsoft.jlogic;

import org.springframework.beans.factory.annotation.Autowired;
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

import org.json.simple.JSONObject;

import java.util.concurrent.locks.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Properties;
import java.util.Enumeration;
import org.apache.commons.lang3.StringUtils;

@RestController
public class PluginController {

    Logger logger = LoggerFactory.getLogger(PluginController.class);

    @Autowired
    PluginService pluginService;

    /*
    @GetMapping(value = "/plugin", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Properties> get() {
      GlobalProperties gProp = GlobalProperties.getInstance();
    	return new ResponseEntity<>(gProp.getProperties(), HttpStatus.OK);
    }
    */
    /**
     * Import plugin
     */
    @PutMapping(value = "/plugin", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> importFromFile(@RequestBody Plugin plugin) {
        logger.info("Importing plugin "+plugin.getClassName());

        //JSONObject jplugin = pluginService.specFromClass(plugin);

        return new ResponseEntity<>("", HttpStatus.OK);
    }

    /**
     * Install a plugin from local file
     */
    @PostMapping(value = "/plugin/install", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> install(@RequestParam("jar") String jarFile) {
        logger.info("Installing "+jarFile);
        Result result = pluginService.install(jarFile);

        return new ResponseEntity<>(result.success() ? ((Plugin) result.getData()).toString() : result.getMessage(), 
                                    result.success() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
