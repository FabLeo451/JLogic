'use strict';

var blueprint, blueprint_id;
var undo;
var actionsEnabled = true;
const debug = 0;

const MenuID = {
  INCLUDE_BLUEPRINT: 10000 /* Include another blueprint */
}

/* Connector directions */
const BPDirection = {
  INPUT:  1,
  OUTPUT: 2
}

/* Connector types */
const BPTypeID = {
 ANY:    -1,
 EXEC:    0,
 INTEGER: 1,
 FLOAT:   2,
 STRING:  3,
 BOOLEAN: 4
 //JSON:    5
};
/*
const types = [ "Bool", "Integer", "Float", "String", "Exec" ];
const typeColor = [ "red", "green", "yellow", "purple", "blue" ];
*/
/* Node types */

const BPNodeTypeID = {
  ENTRY_POINT: 0, 
  RETURN: 1, 
  SEQUENCE: 2, 
  BRANCH: 3, 
  INTERNAL_FUNCTION: 4, 
  OPERATOR: 5,
  GET: 6,
  SET: 7,
  BLUEPRINT: 8,
  FOR_LOOP: 9,
  EXIT: 10,
  JUNCTION: 11,
  SWITCH_INTEGER: 12,
  WHILE_LOOP: 13
};

class BPNodeType {
  constructor () {
    this.id = 0;
    this.name = "NodeType";
    this.color = "green";
  }
}

//const nodeTypes = [ "Internal", "Function", "Variable", "Entry point", "Return" ];
//const nodeTypeColor = [ "gray", "green", "yellow", "purple", "purple" ];

/* Blueprint status */
const BPStatus = {
  READY: 0,
  CONNECTING: 1,
  BP_DRAGGING: 2,
  MOVING_SELECTION: 3
};

const status_str = [ "Ready", "Connecting", "Dragging", "Moving" ];

/* Blueprint events */
const BPEvent = {
  CONNECTED: 0,
  DISCONNECTED: 1,
  SELECTION_MOVED: 2,
  SELECTION_MOVED_COMPLETED: 3,
  NODE_MOVED: 4,
  NODE_SELECTED: 5
};

class BPType {
  constructor() {
    this.id = 0;
    this.name = "Generic type";
    this.color = "green";
    this.exec = false;
    //this.t_html_input_type = "number";
  }
}

function getTypeIcon (type) {
  var i;
  
  switch (Number(type)) {
    case BPTypeID.INTEGER:
      i = 'i-hashtag';
      break;
      
    case BPTypeID.FLOAT:
      i = 'i-calculator';
      break;
      
    case BPTypeID.JSON:
      i = 'i-code';
      break;

    case BPTypeID.BOOLEAN:
      i = 'i-check-square';
      break;

    case BPTypeID.STRING:
      i = 'i-align-left';
      break;
      
    default:
      i = 'i-square';
      break;
  }

  return (i);
}

const Dimensions = {
 SCALAR: 0,
 ARRAY:  1,
 MATRIX: 2
};

class Variable {
  constructor (name, type, /*isArray*/dimensions) {
    this.id = 0;
    this.name = name;
    this.type = type;
    //this.isArray = isArray;
    this.dimensions = dimensions;
    //this.value = isArray;
    this.value = null;
    
    this.referenced = 0;
    
    this.element = null;
  }
  
  reset () {
    switch (this.type) {
      case BPTypeID.BOOLEAN:
        this.value = true;
        break;
        
      case BPTypeID.INTEGER:
      case BPTypeID.FLOAT:
        this.value = 0;
        break;
        
      case BPTypeID.STRING:
        this.value = "";
        break;
        
      case BPTypeID.JSON:
        this.value = { };
        break;
        
      default:
        this.value = null;
        break;
    }
  }
  
  set (value) {
    this.value = value;
  }
  
  get () {
    return (this.value);
  }
  
  getName () {
    return (this.name);
  }
  
  getType () {
    return (Number(this.type));
  }
  
  ref () { this.referenced ++; }
  
  unref () { 
    if (this.referenced) 
      this.referenced --; 
  }
  
  toJSON () {
    var jo = { "id":this.id, "name":this.name, "type":this.type, "dimensions":this.dimensions, "referenced":this.referenced };
    
    if (this.value != null) {
       
      switch (this.type) {
        case BPTypeID.BOOLEAN:
        case BPTypeID.INTEGER:
        case BPTypeID.FLOAT:
          jo.value = this.value;
          break;
        case BPTypeID.STRING:
          //actual = ', "value": "'+this.value.replace (/"/g,'\\"')+'"';
          jo.value = this.value;
          break;
        case BPTypeID.JSON:
          jo.value = this.value;
          break;
        default:
          //console.log ("Type without value: "+this.type);
          //actual = '';
          break;
      }
    }
        
    return (jo);
  }
  
  toString () {
    var s, actual;
    //s = '{ "id":'+this.id+', "name":"'+this.name+'", "type":'+this.type+', "isArray":'+this.isArray+', "referenced":'+this.referenced+' ';
    s = '{ "id":'+this.id+', "name":"'+this.name+'", "type":'+this.type+', "dimensions":'+this.dimensions+', "referenced":'+this.referenced+' ';
    
    //console.log ("Saving variable "+this.name+" = "+this.value);
    
    if (this.value != null) {
       
      switch (this.type) {
        case BPTypeID.BOOLEAN:
        case BPTypeID.INTEGER:
        case BPTypeID.FLOAT:
          actual = ', "value": '+this.value;
          break;
        case BPTypeID.STRING:
          //actual = ', "value": "'+this.value.replace (/"/g,'\\"')+'"';
          actual = ', "value": "'+stringForJson (this.value)+'"';
          break;
        case BPTypeID.JSON:
          actual = ', "value": '+JSON.stringify(this.value);
          break;
        default:
          //console.log ("Type without value: "+this.type);
          actual = '';
          break;
      }
      
      s += actual;
    }
    
    s += ' }';
    return (s);
  }
}
