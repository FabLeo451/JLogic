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

  public Plugin() {
  }

  public String getClassName() {
    return(className);
  }

  public String getPath() {
    return(path);
  }

  public String toString() {
      return("Plugin [class="+className+"]");
  }
}
