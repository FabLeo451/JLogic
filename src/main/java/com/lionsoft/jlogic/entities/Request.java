package com.lionsoft.jlogic;

import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import java.security.Principal;
import eu.bitwalker.useragentutils.*;

public class Request {

  private Date timestamp;
  private String user = null;
  private String agent = null;
  private String requestURI = null;
  private String remoteAddress = null;
  
  public Request(HttpServletRequest request) {
    HttpSession session = request.getSession();
    
    timestamp = new Date(session.getLastAccessedTime());
    requestURI = request.getRequestURI();
    update(request);
  }
  
  public void update(HttpServletRequest request) {
    HttpSession session = request.getSession();

    user = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "Unknown";
    agent = request.getHeader("User-Agent");
    
    UserAgent userAgent = UserAgent.parseUserAgentString(agent);
    
    if (userAgent != null) {
      Browser browser = userAgent.getBrowser();
      BrowserType type = browser.getBrowserType();
      
      if (type != BrowserType.TOOL) {
        String bwrVersion = "?";
        Version v = userAgent.getBrowserVersion();
        if (v != null)
          bwrVersion = userAgent.getBrowserVersion().getMajorVersion();

        String bwrName = browser.getName();
        
        OperatingSystem os = userAgent.getOperatingSystem();
        String osName = os.getName();
        //String deType = os.getDeviceType().getName();
      
        agent = bwrName+"/"+bwrVersion+" "+osName;
      }
    }
    
    remoteAddress = request.getRemoteAddr();
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public String getUser() {
    return user;
  }

  public String getAgent() {
    return agent;
  }

  public String getRequestURI() {
    return requestURI;
  }

  public String getRemoteAddress() {
    return remoteAddress;
  }
}
