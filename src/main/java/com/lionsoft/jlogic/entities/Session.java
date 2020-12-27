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
  public static int EXECUTING = 2;

  private String id = null;
  private Date loginTime;
  //private long lastAccessedTime = 0;
  private String user = null;
  //private String status;
  //private String agent = null;
  private String requestURI = null;
  private String remoteAddress = null;
  //private String programUnit;
  private boolean isWebApplication = false;

  public Session(HttpSession s) {
    id = s.getId();
    
    try {
      //status = (String) s.getAttribute("status");
      loginTime = new Date(s.getCreationTime());
      user = (String) s.getAttribute("user");
      //agent = (String) s.getAttribute("agent");
      isWebApplication = (boolean) (s.getAttribute("webApplication") != null ? s.getAttribute("webApplication") : false);
      //programUnit = (String) s.getAttribute("programUnit");
      remoteAddress = (String) s.getAttribute("remoteAddress");
    } catch (IllegalStateException e) {
      // Can occur on already invalidated sessions
    }
  }

  public Session(HttpServletRequest request) {
    HttpSession session = request.getSession();

    id = session.getId();
    loginTime = new Date(session.getCreationTime());
    requestURI = request.getRequestURI();
    //programUnit = "";
    update(request);
  }

  public void update(HttpServletRequest request) {
    HttpSession session = request.getSession();

    //lastAccessedTime = session.getLastAccessedTime();
    user = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "Unknown";
    
    /*
    status = "ACTIVE";

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
*/
    remoteAddress = request.getRemoteAddr();
  }

  public String getId() {
    return id;
  }

  public Date getLoginTime() {
    return loginTime;
  }
/*
  public long getLastAccessedTime() {
    return lastAccessedTime;
  }*/

  public String getUser() {
    return user;
  }
/*
  public void setStatus(String s) {
    status = s;
  }

  public String getStatus() {
    return status;
  }

  public String getAgent() {
    return agent;
  }
*/
  public String getRequestURI() {
    return requestURI;
  }

  public String getRemoteAddress() {
    return remoteAddress;
  }
/*
  public String getProgramUnit() {
    return programUnit;
  }

  public void setProgramUnit(String pu) {
    this.programUnit = pu;
  }
*/
  public boolean getWebApplication() {
    return isWebApplication;
  }

  public void setWebApplication(boolean wa) {
    isWebApplication = wa;
  }
  
  public String toString() {
      return("User="+user+" Address="+remoteAddress+" SessionId="+id);
  }
}
