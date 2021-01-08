package com.lionsoft.jlogic;

import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import eu.bitwalker.useragentutils.*;

public class Plugin {

  private String path;
  private String className;
  private String name;
  private String version;

  public Plugin() {
  }

  public String getClassName() {
    return(className);
  }

  public String getPath() {
    return(path);
  }

  public String getName() {
    return(name);
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return(version);
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String toString() {
      return("Plugin [name="+name+" version="+version+" class="+className+"]");
  }
}
