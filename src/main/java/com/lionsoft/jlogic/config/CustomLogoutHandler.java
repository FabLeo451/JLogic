package com.lionsoft.jlogic;

import org.springframework.beans.factory.annotation.Autowired;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import java.io.IOException;
import javax.servlet.ServletException;
import org.springframework.security.core.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomLogoutHandler implements LogoutHandler {
    
    static Logger logger = LoggerFactory.getLogger(CustomLogoutHandler.class);
    
    @Autowired
    private SessionService sessionService;
    
    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        HttpSession httpSessions = request.getSession(false);
        String id = httpSessions.getId();
        String username = (String) httpSessions.getAttribute("user");

        logger.info(username+" "+id+" logging out");
        // sessionService.deleteSession(id); Deleted in LogoutListener
    }
}
