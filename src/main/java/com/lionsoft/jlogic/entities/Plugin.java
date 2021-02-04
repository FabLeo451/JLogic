package com.lionsoft.jlogic;

import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import eu.bitwalker.useragentutils.*;

import javax.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name="PLUGIN_DEPS")
public class Plugin {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String jarFile;
    private String groupId;
    private String artifactId;
    private String name;
    private String version;
    private String path;
    private String className;
    private String spec;
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="program_id")
    private ProgramEntity program;

    public Plugin() {
        version = "1.0.0";
    }

    public Plugin(String g, String a, String v) {
        groupId = g;
        artifactId = a;
        version = v;
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

    public ProgramEntity getProgram() { return(program); }
    public void setProgram(ProgramEntity program) { this.program = program; }

    public String getVersion() { return(version); }
    public void setVersion(String version) { this.version = version; }

    public String getDescription() { return(description); }
    public void setDescription(String description) { this.description = description; }

    public boolean equals(Plugin p) {
        return(p.getGroupId().equals(this.groupId) &&
               p.getArtifactId().equals(this.artifactId) &&
               p.getVersion().equals(this.version)
        );
    }

    public String toString() {
        return("Plugin [name="+name+" version="+version+" class="+className+"]");
    }
}
