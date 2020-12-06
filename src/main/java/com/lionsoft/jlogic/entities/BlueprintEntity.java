package com.lionsoft.jlogic;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.CascadeType;
import javax.persistence.Transient;
import javax.persistence.ElementCollection;
import javax.persistence.CollectionTable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.io.IOException;

@Entity
@Table(name="BLUEPRINT")
public class BlueprintEntity {
    public final String tag = "BLUEPRINT";

    public static final int GENERIC = 0;
    public static final int MAIN = 1;

    @Transient
    Logger logger = LoggerFactory.getLogger(BlueprintEntity.class);

    @Id
    //@GeneratedValue
    private String id;

    @Column(name="internal_id")
    private Integer internalId = 0; // Unique id within the program

    @Column(name="name")
    private String name;

    @Column(name="method")
    private String method;

    @Column(name="update_time")
    private Date updateTime;

    @Column(name="owner")
    private String owner;

    @Enumerated(EnumType.STRING)
    @Column(name="type")
    private BlueprintType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="program_id")
    private ProgramEntity program;

    @OneToMany(cascade = CascadeType.ALL,
            fetch = FetchType.EAGER,
            mappedBy = "blueprint")
    private List<APIEntity> apis;
/*
    @Column(name="content", columnDefinition = "TEXT")
    String content;
*/
    @ElementCollection
    @CollectionTable(name = "jar", joinColumns = @JoinColumn(name = "blueprint_id"))
    @Column(name="jar")
    List<String> jarList = new ArrayList<String>();//Collections.emptyList();

    @ElementCollection
    @CollectionTable(name = "classpath", joinColumns = @JoinColumn(name = "blueprint_id"))
    @Column(name="classpath")
    List<String> classPathList = new ArrayList<String>();//Collections.emptyList();

    @JsonIgnore
    @Transient
    private List<Variable> variables = Collections.emptyList();

    public BlueprintEntity() {

    }

    public BlueprintEntity(String id, String name, BlueprintType type) {
      setUpdateTime(new Date());
      setId(id);
      setType(type);
      setName(name);
    }

    //Setters and getters

    @Override
    public String toString() {
        return "BlueprintEntity [id=" + id + " ("+internalId+"), name=" + name + ", type=" + type + "]";
    }

    public String getTag() {
      return (tag);
    }

    public String getId() {
      return (id);
    }

    public void setId(String id) {
      this.id = id;
    }

    public Integer getInternalId() {
      return (internalId);
    }

    public void setInternalId(Integer id) {
      this.internalId = id;
    }

    public String getName() {
      return (name);
    }

    public void setName(String name) {
      this.name = name;
      //setMethod(type == BlueprintType.MAIN ? "_main" : "method_"+name.replace(" ", "_"));
      switch (type) {
        case MAIN:
          setMethod("_main");
          break;
        case EVENTS:
          setMethod("_event");
          break;
        default:
          setMethod("method_"+name.replace(" ", "_"));
          break;
      }
    }

    public String getMethod() {
      return (method);
    }

    public void setMethod(String method) {
      this.method = method;
    }

    public Date getUpdateTime() {
      return (updateTime);
    }

    public void setUpdateTime(Date time) {
      this.updateTime = time;
    }

    public String getOwner() {
      return (owner);
    }

    public void setOwner(String owner) {
      this.owner = owner;
    }

    public BlueprintType getType() {
      return (type);
    }

    public void setType(BlueprintType type) {
      this.type = type;
    }

    @JsonIgnore
    public boolean isMain() {
      return (this.type == BlueprintType.MAIN);
    }

    @JsonIgnore
    public ProgramEntity getProgram() {
      return (program);
    }

    @JsonProperty
    public String getProgramId() {
      return (program != null ? program.getId() : null);
    }

    @JsonProperty
    public String getProgramName() {
      return (program != null ? program.getName() : "");
    }

    public void setProgram(ProgramEntity program) {
      this.program = program;
      this.internalId = program.getNewInternalId();
    }

    @JsonIgnore
    public String getBaseFilename() {
      return ("BP_"+getId()+".json");
    }

    @JsonIgnore
    public String getFilename() {
      return (program != null ? program.getMyDir()+"/"+getBaseFilename() : null);
    }

    @JsonIgnore
    public List<APIEntity> getAPIs() {
      return apis;
    }

    //@JsonIgnore
    public List<Variable> getVariables() {
      return variables;
    }

    public void setVariables(List<Variable> vars) {
      variables = vars;
    }

    @JsonIgnore
    public JSONObject loadJson() {
      JSONParser jsonParser = new JSONParser();

      try {
        String text = new String (Files.readAllBytes(Paths.get(getFilename())));
        return ((JSONObject) jsonParser.parse(text));
      } catch (IOException e) {
        logger.error(e.toString());
      } catch (ParseException e) {
        logger.error(e.toString());
      }

      return null;
    }

    public void addDependency(List<String> list, JSONArray ja, String path) {
      for (int j=0; ja != null && j<ja.size(); j++) {
        String dep = (String) ja.get(j);

        if (path != null)
          dep = dep.replace("{path}", path);

        if (!list.contains(dep)) {
          //System.out.println("  "+(String) jar.get(j));
          list.add(dep);
        }
      }
    }

    public void setDependecies(String content) {
      JSONObject jcontent;
      JSONParser jsonParser = new JSONParser();

      //this.content = content;
      jarList = new ArrayList<String>();
      classPathList = new ArrayList<String>();

      try {
        jcontent = (JSONObject) jsonParser.parse(content);
        
        // First check overall classpath
        if (jcontent.containsKey("classpath")) {
          addDependency(classPathList, (JSONArray) jcontent.get("classpath"), "");
        }
        
        // Overall jar list
        if (jcontent.containsKey("jar")) {
          addDependency(jarList, (JSONArray) jcontent.get("jar"), "");
        }
/*
        // Get dependencies from every node
        JSONArray jnodes = (JSONArray) jcontent.get("nodes");

        for (int i=0; i<jnodes.size(); i++) {
          JSONObject jn = (JSONObject) jnodes.get(i);

          //System.out.println("Node: "+(String) jn.get("name"));

          // If path is present maybe we have "jar":"{path}/jarname.jar"
          String path = null;

          if (jn.containsKey("data")) {
            JSONObject jdata = (JSONObject) jn.get("data");
            path = jdata.containsKey("path") ? (String) jdata.get("path") : null;
          }

          if (jn.containsKey("jar"))
            addDependency(jarList, (JSONArray) jn.get("jar"), path);

          if (jn.containsKey("classpath"))
            addDependency(classPathList, (JSONArray) jn.get("classpath"), path);
        }
        */
      } catch (ParseException e) {
        logger.error(e.getMessage());
      }
    }
    /*
    public String getContent() {
      return content;
    }
    */

    @JsonIgnore
    public List<String> getJARList() {
      return jarList;
    }

    @JsonIgnore
    public List<String> getClassPath() {
      return classPathList;
    }

    public boolean referencesVariable(Variable v) {
      JSONObject jo = loadJson();
      //System.out.println("Checking "+v.getName()+" in "+getName());

      if (jo != null) {
        //System.out.println("Getting variable data...");
        JSONArray ja = (JSONArray) jo.get("variables");

        for (int k=0; k<ja.size(); k++) {
          JSONObject jvar = (JSONObject) ja.get(k);

          //System.out.println(v.getId()+" <-> "+(String) jvar.get("id"));

          if (v.getId().toString().equals((String) jvar.get("id"))) {
            //System.out.println("FOUND. Referenced = "+(Long) jvar.get("referenced"));
            return ((Long) jvar.get("referenced") > 0L);
          }
        }
      }

      return false;
    }
}
