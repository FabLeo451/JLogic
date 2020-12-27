'use strict';

var blueprint, blueprint_id;
var undo;
var actionsEnabled = true;
const debug = false;
var substitutions = []; // used to keep track of original ids replaced by new ones

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
  WHILE_LOOP: 13,
  EVENT: 14
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
  READY:            0,
  CONNECTING:       1,
  BP_DRAGGING:      2,
  MOVING_SELECTION: 3,
  SELECTING:        4 /* Selection box */
};

const status_str = [ "Ready", "Connecting", "Dragging", "Moving", "Selecting" ];

/* Blueprint events */
const BPEvent = {
  CONNECTED: 0,
  DISCONNECTED: 1,
  SELECTION_MOVED: 2,
  SELECTION_MOVED_COMPLETED: 3,
  NODE_MOVED: 4,
  NODE_SELECTED: 5
};

/* Variable scope */
const Scope = {
  ALL: 0,
  LOCAL: 1,
  GLOBAL: 2
};

class BPType {
  constructor() {
    this.id = 0;
    this.name = "Generic type";
    this.color = "green";
    this.exec = false;

    this.import = null;
    this.classpath = null;
    this.jar = null;
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
  
  getArray() { return this.dimensions; }
  setArray(a) { this.dimensions = a; }

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
    return ("Variable [id="+this.id+" name="+this.name+" type="+this.type+" dimensions="+this.dimensions+" global="+this.global+" referenced="+this.referenced+"]");
  }
}

class Rect {
    constructor (svg) {
        this.svg = svg;
        this.p0 = { x:0, y:0 };
        this.p1 = { x:0, y:0 };
        this.elem = document.getElementById("selection-box");
        this.selection = [];
    }
  
    setP0(p) {
        this.p0 = { x:p.x, y:p.y };
    }
    
    getP0() { return this.p0; }
  
    setP1(p) {
        this.p1 = { x:p.x, y:p.y };
    }
    
    getP1() { return this.p1; }
    
    contains(p) {
        var x0 = this.p0.x < this.p1.x ? this.p0.x : this.p1.x;
        var y0 = this.p0.y < this.p1.y ? this.p0.y : this.p1.y;
        var x1 = this.p0.x < this.p1.x ? this.p1.x : this.p0.x;
        var y1 = this.p0.y < this.p1.y ? this.p1.y : this.p0.y;
        
        return(p.x <= x1 && p.x >= x0 && p.y >= y0 && p.y <= y1);
    }
    
    add(x) {
         if (x && !this.selection.includes(x))
            this.selection.push(x);
    }
    
    remove(x) {
        if (x && !this.selection.includes(x))
            this.selection.splice(this.selection.indexOf(x), 1);
    }
    
    getSelection() { return this.selection; }
    
    redraw() {
        if (!this.elem) {
            this.elem = document.createElementNS(this.svg.ns, 'rect');
            this.elem.setAttributeNS(null, 'id', 'selection-box');
            this.elem.setAttributeNS(null, 'stroke', 'silver');
            this.elem.setAttributeNS(null, 'fill', 'silver');
            this.elem.setAttributeNS(null, 'fill-opacity', '0.3');
            this.svg.appendChild(this.elem);
        }
        
        var x = this.p0.x < this.p1.x ? this.p0.x : this.p1.x;
        var y = this.p0.y < this.p1.y ? this.p0.y : this.p1.y;
        var width = this.p1.x > this.p0.x ? this.p1.x - this.p0.x : this.p0.x - this.p1.x;
        var height = this.p1.y > this.p0.y ? this.p1.y - this.p0.y : this.p0.y - this.p1.y;
        
        this.elem.setAttributeNS(null, 'x', x);
        this.elem.setAttributeNS(null, 'y', y);
        this.elem.setAttributeNS(null, 'width', width);
        this.elem.setAttributeNS(null, 'height', height);
    }
    
    destroy() {
        if (this.elem) {
            this.elem.parentElement.removeChild(this.elem);
            this.elem = null;
        }
    }
    
    toString() {
        return("Rect ("+this.p0.x+","+this.p0.y+") ("+this.p1.x+","+this.p1.y+")");
    }
}
