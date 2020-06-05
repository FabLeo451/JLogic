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
 
@Entity
@Table(name="BLUEPRINT")
public class BlueprintEntity {

    public static final int GENERIC = 0;
    public static final int MAIN = 1;
    
    @Transient
    Logger logger = LoggerFactory.getLogger(BlueprintEntity.class);
      
    @Id
    //@GeneratedValue
    private String id;
     
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
    
    @Column(name="content", columnDefinition = "TEXT")
    String content;
    
    @ElementCollection
    @CollectionTable(name = "jar", joinColumns = @JoinColumn(name = "blieprint_id"))
    @Column(name="jar")
    List<String> jarList = Collections.emptyList();
    
    public BlueprintEntity() {

    }
    
    public BlueprintEntity(String id, String name, BlueprintType type) {
      setUpdateTime(new Date());
      setId(id);
      setName(name);
      setType(type);
    }
     
    //Setters and getters
 
    @Override
    public String toString() {
        return "BlueprintEntity [id=" + id + ", name=" + name + ", type=" + type + "]";
    }
    
    public String getId() {
      return (id);
    }
    
    public void setId(String id) {
      this.id = id;
    }
    
    public String getName() {
      return (name);
    }
    
    public void setName(String name) {
      this.name = name;
      setMethod(type == BlueprintType.MAIN ? "_main" : "method_"+name.replace(" ", "_"));
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
    }

    @JsonIgnore
    public String getFilename() {
      return (program != null ? program.getMyDir()+"/BP_"+getId()+".json" : null);
    }
    
    @JsonIgnore
    public List<APIEntity> getAPIs() {
      return apis;
    }
    
    public void setContent(String content) {
      JSONObject jcontent;
      JSONParser jsonParser = new JSONParser();
      
      this.content = content;
      jarList = new ArrayList<String>();
      
      try {
        jcontent = (JSONObject) jsonParser.parse(content);
        
        // Get dependencies from every node
        
        JSONArray jnodes = (JSONArray) jcontent.get("nodes");
        
        for (int i=0; i<jnodes.size(); i++) {
          JSONObject jn = (JSONObject) jnodes.get(i);
          
          //System.out.println("Node: "+(String) jn.get("name"));
          
          if (jn.containsKey("jar")) {
            JSONArray ja = (JSONArray) jn.get("jar");

            // If path is present maybe we have "jar":"{path}/jarname.jar"
            String path = null;
            
            if (jn.containsKey("data")) {
              JSONObject jdata = (JSONObject) jn.get("data");
              path = jdata.containsKey("path") ? (String) jdata.get("path") : null;
            }
                  
            for (int j=0; ja != null && j<ja.size(); j++) {
              String jar = (String) ja.get(j);
              
              if (path != null)
                jar = jar.replace("{path}", path);
          
              if (!jarList.contains(jar)) {
                //System.out.println("  "+(String) jar.get(j));
                jarList.add(jar);
              }
            }
          }
        }
      } catch (ParseException e) {
        logger.error(e.getMessage());
      }
    }
    
    public String getContent() {
      return content;
    }
    
    public List<String> getJARList() {
      return jarList;
    }
}
