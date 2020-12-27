package com.lionsoft.jlogic;

import java.util.*;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.security.core.userdetails.UserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class LogoutListener implements ApplicationListener<SessionDestroyedEvent> {
    
    Logger logger = LoggerFactory.getLogger(LogoutListener.class);

    @Autowired
    SessionService sessionService;

    @Override
    public void onApplicationEvent(SessionDestroyedEvent event)
    {
        /*
        List<SecurityContext> lstSecurityContext = event.getSecurityContexts();
        UserDetails ud;
        for (SecurityContext securityContext : lstSecurityContext)
        {
            ud = (UserDetails) securityContext.getAuthentication().getPrincipal();
            // ...
        }
        */
        
        //logger.info("Session destroyed "+event.getId());
        
        HttpSession s = sessionService.getSession(event.getId());
        
        if (s != null) {
            //logger.info(s.getAttribute("user")+" logged out ("+event.getId()+")");
            logger.info("Log out "+(new Session(s).toString()));
            sessionService.deleteSession(event.getId());
        }
    }

}
