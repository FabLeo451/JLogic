package com.lionsoft.jlogic;

import java.util.*;
import java.lang.StringBuffer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import java.security.Principal;
import eu.bitwalker.useragentutils.*;

public class Session {
  public static int IDLE = 0;
  public static int ACTIVE = 1;

  private String id = null;
  private Date creationTime;
  private long lastAccessedTime = 0;
  private String user = null;
  private int status = ACTIVE;
  private String agent = null;
  private String requestURI = null;
  private String remoteAddress = null;
  
  public Session(HttpServletRequest request) {
    HttpSession session = request.getSession();
    
    id = session.getId();
    creationTime = new Date(session.getCreationTime());
    requestURI = request.getRequestURI();
    update(request);
  }
  
  public void update(HttpServletRequest request) {
    HttpSession session = request.getSession();

    lastAccessedTime = session.getLastAccessedTime();
    user = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "Unknown";
    status = Session.ACTIVE;
    
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

  public String getId() {
    return id;
  }

  public Date getCreationTime() {
    return creationTime;
  }

  public long getLastAccessedTime() {
    return lastAccessedTime;
  }

  public String getUser() {
    return user;
  }

  public void setStatus(int s) {
    status = s;
  }

  public int getStatus() {
    return status;
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
