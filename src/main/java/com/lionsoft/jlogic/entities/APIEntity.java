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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.*;
 
@Entity
@Table(name="API")
public class APIEntity {

    @Id
    private String id;
     
    @Column(name="name")
    private String name;
     
    @Column(name="path")
    private String path;
     
    @Column(name="method")
    private String method;
    
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
      setMethod("GET");
      setPath("foo");
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
    
    public String getPath() {
      return (path);
    }
    
    public void setPath(String path) {
      this.path = path;
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
    
    public Map<String, String> mapURI(String uri) {
      Map<String, String> map = new HashMap();
      
      if (path.endsWith("/") && !uri.endsWith("/"))
        uri += "/";
      
      if (!path.endsWith("/") && uri.endsWith("/"))
        path += "/";
        
      //System.out.println("path = "+path);
      //System.out.println("uri  = "+uri);
      
      String[] partsApi = path.split("/");
      String[] parts = uri.split("/");
      
      if (partsApi.length != parts.length)
        return null;
      
      for (int i=0; i<partsApi.length; i++) {
        boolean isParameter = partsApi[i].substring(0,1).equals("{");
        
        //System.out.println("["+i+"] "+partsApi[i]+" = "+parts[i]);
        
        if (isParameter) {
          String paramName = partsApi[i].substring(1).replace("}", "");
          String ParamValue = parts[i];
          
          map.put(paramName, ParamValue);
          //System.out.println(paramName+" = "+ParamValue);
        } else {
          if (!parts[i].equals(partsApi[i]))
            return null;
        }
      }
      
      return(map);
    }
}
