package com.lionsoft.jlogic;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.GenericGenerator;
import javax.persistence.*;
import java.util.UUID;

@Entity
class Variable {

  public final static int SCALAR = 0;
  public final static int ARRAY = 1;
  public final static int MATRIX = 2;

  @Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(
		name = "UUID",
		strategy = "org.hibernate.id.UUIDGenerator"
	)
  private UUID id;
   
  private String name;

  private String type;
  
  //private Object value;
  
  private int dimensions;
  
  boolean global;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="program_id")
  private ProgramEntity program;
 
  public Variable() {
    //value = null;
    dimensions = SCALAR;
    name = null;
    global = true;
  }
 
  public Variable(String type, int dimensions, String name) {
    this();
    
    this.type = type;
    this.dimensions = dimensions;
    this.name = name;
  }
 
  public Variable(Variable v) {
    this();   
    set(v);
  }
 
  public String toString() {
    return "Variable [id=" + id.toString() + ", name=" + name + ", type=" + type + "]";
  }
 
  public void set(Variable v) {
    this.type = v.getType();
    this.dimensions = v.getDimensions();
    this.name = v.getName();
  }
  
  @JsonIgnore
  public boolean isValid() {
    return (name != null && type != null &&
            !name.isEmpty() && !type.isEmpty() &&
            dimensions >= 0 &&
            dimensions <= 2
           );
  }
  
  public void setProgram(ProgramEntity p) {
      this.program = p;
  }
  
  @JsonIgnore
  public ProgramEntity getProgram() {
      return (this.program);
  }
  
  public UUID getId() {
    return id;
  }
  
  public void setName(String name) {
      this.name = name;
  }

  public String getName() {
    return (name);
  }
  
  public String getType() {
    return (type);
  }
  
  public void setType(String t) {
      this.type = t;
  }
  
  public int getDimensions() {
    return (dimensions);
  }
  
  public boolean isGlobal() {
    return (global);
  }

};

