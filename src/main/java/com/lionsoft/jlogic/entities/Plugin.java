package com.lionsoft.jlogic;

import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import eu.bitwalker.useragentutils.*;

public class Plugin {

    private String jarFile;
    private String groupId;
    private String artifactId;
    private String name;
    private String version;
    private String path;
    private String className;
    private String spec;

    public Plugin() {
        version = "1.0.0";
    }

    public String getJarFile() { return(jarFile); }
    public void setJarFile(String jarFile) { this.jarFile = jarFile; }

    public String getGroupId() { return(groupId); }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getArtifactId() { return(artifactId); }
    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }

    public String getClassName() { return(className); }
    public void setClassName(String className) { this.className = className; }

    public void setPath(String path) { this.path = path; }
    public String getPath() { return(path); }

    public String getName() { return(name); }
    public void setName(String name) { this.name = name; }

    public String getSpec() { return(spec); }
    public void setSpec(String spec) { this.spec = spec; }

    public String getVersion() { return(version); }
    public void setVersion(String version) { this.version = version; }

    public String toString() {
        return("Plugin [name="+name+" version="+version+" class="+className+"]");
    }
}
