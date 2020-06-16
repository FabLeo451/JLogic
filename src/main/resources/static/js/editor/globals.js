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
  
  switch (type) {
    case "Integer":
      i = 'i-hashtag';
      break;
      
    case "Double":
      i = 'i-calculator';
      break;
      
    case "JSONObject":
      i = 'i-code';
      break;

    case "Boolean":
      i = 'i-check-square';
      break;

    case "String":
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
  constructor () {
    this.id = 0;
    this.name = 'varName';
    this.type = 1;
    this.dimensions = 0;
    this.value = null;
    this.global = false;
    
    this.referenced = 0;
    
    this.element = null;
  }
  
  create (name, type, dimensions) {
    this.name = name;
    this.type = type;
    this.dimensions = dimensions;
  }
  
  setGlobal(g) {
    this.global = g;
  }
  
  isGlobal() {
    return this.global;
  }
  
  fromJSON(jo) {
    this.id = jo.id;
    this.name = jo.name;
    this.type = jo.type;
    this.dimensions = jo.dimensions;
    this.value = jo.hasOwnProperty("value") ? jo.value : null;
    this.global = jo.hasOwnProperty("global") ? jo.global : false;
    this.referenced = jo.hasOwnProperty("referenced") ? jo.referenced : 0;

    this.element = null;  
  }
  
  reset () {
    switch (this.type) {
      case 'Booelan':
        this.value = true;
        break;
        
      case 'Integer':
      case 'Double':
        this.value = 0;
        break;
        
      case 'String':
        this.value = "";
        break;
/*        
      case 'JSONObject:
        this.value = { };
        break;
*/        
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
  
  setName (n) {
    this.name = n;
  }
  
  getName () {
    return (this.name);
  }
  
  setType (t) {
    this.type = t;
  }
  
  getType () {
    return (this.type);
  }
  
  ref () { this.referenced ++; }
  
  unref () { 
    if (this.referenced) 
      this.referenced --; 
  }
  
  toJSON () {
    var jo = { "id":this.id, "name":this.name, "type":this.type, "dimensions":this.dimensions, "referenced":this.referenced };
    
    if (this.global)
      jo.global = true;
    
    if (this.value != null) {
       
      switch (this.type) {
        case 'Booelan':
        case 'Integer':
        case 'Double':
          jo.value = this.value;
          break;
        case 'String':
          //actual = ', "value": "'+this.value.replace (/"/g,'\\"')+'"';
          jo.value = this.value;
          break;
        /*case BPTypeID.JSON:
          jo.value = this.value;
          break;*/
        default:
          //console.log ("Type without value: "+this.type);
          //actual = '';
          break;
      }
    }
        
    return (jo);
  }
  
  stringDump () {
    return (JSON.stringify(this.toJSON()));
  }
  
  toString () {
    return ("Variable [id="+this.id+" name="+this.name+" type="+this.type+" dimensions="+this.dimensions+" referenced="+this.referenced+"]");
  }
}
