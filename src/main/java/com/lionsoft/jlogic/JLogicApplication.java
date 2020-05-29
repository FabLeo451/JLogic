package com.lionsoft.jlogic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class JLogicApplication {

  private static final Logger logger = LoggerFactory.getLogger(JLogicApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(JLogicApplication.class, args);
		
		logger.info("READY!");
	}


}
