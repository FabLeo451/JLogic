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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.List;
 
@Entity
@Table(name="BLUEPRINT")
public class BlueprintEntity {

    public static final int GENERIC = 0;
    public static final int MAIN = 1;
  
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
}
