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
@Table(name="DEPENDENCIES")
public class Dependency {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String groupId;
    private String artifactId;
    private String version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="program_id")
    private ProgramEntity program;

    public Dependency() {
        version = "1.0.0";
    }

    public Dependency(String g, String a, String v) {
        groupId = g;
        artifactId = a;
        version = v;
    }

    public String getGroupId() { return(groupId); }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getArtifactId() { return(artifactId); }
    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }

    public String getVersion() { return(version); }
    public void setVersion(String version) { this.version = version; }

    public ProgramEntity getProgram() { return(program); }
    public void setProgram(ProgramEntity program) { this.program = program; }

    public boolean equals(Dependency d) {
        return(d.getGroupId().equals(this.groupId) &&
               d.getArtifactId().equals(this.artifactId) &&
               d.getVersion().equals(this.version)
        );
    }

    public String toString() {
        return("Dependency ["+groupId+"."+artifactId+"."+version+"]");
    }
}
