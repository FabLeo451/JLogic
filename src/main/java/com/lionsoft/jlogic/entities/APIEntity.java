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
import javax.persistence.Transient;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Optional;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
 
@Entity
@Table(name="API")
public class APIEntity {

    @Id
    private String id;
     
    @Column(name="name")
    private String name;
    
    @Column(name="update_time")
    private Date updateTime;
    
    @Column(name="owner")
    private String owner;
    
    @Column(name="enabled")
    private boolean enabled;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="blueprint_id")
    private BlueprintEntity blueprint;
    
    /*@Transient
    @JsonIgnore
    String blueprintId;*/
    
    public APIEntity() {
      enabled = true;
    }
    
    public APIEntity(String id, String name, boolean enabled) {
      setUpdateTime(new Date());
      setId(id);
      setName(name);
      setEnabled(enabled);
    }
     
    //Setters and getters
 
    @Override
    public String toString() {
        return "APIEntity [id=" + id + ", name=" + name + ", blueprint="+(blueprint != null ? blueprint.getName() : "none")+"]";
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
    
    public boolean getEnabled() {
      return (enabled);
    }
    
    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
    
    @JsonProperty
    public BlueprintEntity getBlueprint() {
      return (blueprint);
    }
    
    public void setBlueprint(BlueprintEntity blueprint) {
      this.blueprint = blueprint;
    }
    /*
    @JsonProperty
    public String getBlueprintId() {
        return blueprint != null ? blueprint.getId() : null;
    }*/
}
