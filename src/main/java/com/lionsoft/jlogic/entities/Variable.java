package com.lionsoft.jlogic;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;

@Entity
class Variable {

  public final static int SCALAR = 0;
  public final static int ARRAY = 1;
  public final static int MATRIX = 2;

  @Id
  @GeneratedValue
  private Long id;
   
  private String name;

  private String type;
  
  //private Object value;
  
  private int dimensions;
/*  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="program_id")*/
  //private ProgramEntity program;
 
  public Variable() {
    //value = null;
    dimensions = SCALAR;
    name = "unnamed";
  }
 
  public Variable(String type, int dimensions, String name) {
    this();
    
    this.type = type;
    this.dimensions = dimensions;
    this.name = name;
  }
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="program_id")
  private ProgramEntity program;
  
  public void setProgram(ProgramEntity p) {
      this.program = p;
  }
  
  @JsonIgnore
  public ProgramEntity getProgram() {
      return (this.program);
  }
/*  
  public Variable(String type, int dimensions, String name, Object value) {
    this(type, dimensions, name);
    setValue(value);
  }
 
  public void setValue(Object v) {
    value = v;
  }
  
  public Object getValue() {
    return (value);
  }
*/
  public String getName() {
    return (name);
  }
  
  public String getType() {
    return (type);
  }
  
  public int getDimensions() {
    return (dimensions);
  }

};

