'use strict';

class NodeBase {

  constructor() {
    this.name = "New node";
    this.icon = null;
    this.id = 1;
    this.version = 0;
    this.plugin = null;
    this.function_name = null;
    this.connectors = [ ];
    this.x = this.y = 0;
    this.hasHeader = true;
    this.header = null;
    this.canDelete = true;
    this.selected = false;
    this.overValue = false;
    this.zoom = 1;
    this.inventory = true;
    this.centerColumn = null;
    this.addInputJson = null;
    this.addOutputJson = null;

    this.import = null;
    this.jar = null;
    this.classpath = null;
    this.symbol = null;
    this.java = null;
    this.data = null;
    this.options = null;
  }

  getElement() {
    return (this.nodeElem);
  }

  getName () {
    return (this.name);
  }

  setIcon (i) {
    this.icon = i;

    if (this.header !== null)
      this.header.innerHTML = i !== null ? i+' '+this.getName () : this.getName ();
  }

  setZoom (zoom) {
    this.zoom = zoom;
    setElementZoom (this.getElement(), this.zoom, -this.x, -this.y);
  }

  setID (id) {
    this.id = id;

    if (this.nodeElem)
      this.nodeElem.setAttribute('id', id);

    if (this.header)
      this.header.setAttribute('id', id+"header");
  }

  setText (text) {
    this.centerColumn.innerHTML = text;
  }

  moveTo (x, y) {
    //console.log ("[moveTo] "+x+" "+y);
    //setPosition(this.nodeElem, x, y);
    this.nodeElem.style.left = `${x}px`;
    this.nodeElem.style.top = `${y}px`;

    this.x = x;
    this.y = y;

    //blueprint.redrawEdges ();
    var p = { x:x, y:y };
    blueprint.trigger (BPEvent.NODE_MOVED, this, p);
  }

  moveDelta (dx, dy) {
    this.moveTo (this.x - dx, this.y - dy);
  }

  setSelected (selected) {
    if (selected) {
      this.nodeElem.classList.remove("nodeunselected");
      this.nodeElem.classList.add('nodeselected');
    }
    else {
      this.nodeElem.classList.remove("nodeselected");
      this.nodeElem.classList.add('nodeunselected');
    }

    this.selected = selected;
  }

  getSelected () { return (this.selected); }

  disconnect () {
    for (var i=0; i<this.connectors.length; i++) {
      var c = this.connectors[i];
      c.disconnectAll();
    }
  }

  getMaxId () {
    var m = 0;

    for (var i=0; i<this.connectors.length; i++)
      if (this.connectors[i].id > m)
        m = this.connectors[i].id;

    return (m);
  }

  addConnectorFromJson (direction, c) {
    var connector;
    //var isArray;
    var dim;
    var hasValue = c.hasOwnProperty('value') && c['value'] != null;
    var pinType, dataType;

    if (c.hasOwnProperty('type'))
      pinType = dataType = c.type;
    else {
      pinType = c.pinType;
      dataType = c.dataType;
    }

    //console.log ('[addConnectorFromJson] '+JSON.stringify(c));
/*
    if (c.hasOwnProperty('array'))
      isArray = c['array'];
*/
    dim = c.hasOwnProperty('dimensions') ? c.dimensions : Dimensions.SCALAR;

    //this.addConnector (direction, c["type"], c["label"], c["value"], c["enum"], isArray);

    connector = new Connector();

    if (c.hasOwnProperty('single_line'))
      connector.single_line = c['single_line'];

    if (c.hasOwnProperty('password')) {
      connector.password = c['password'];

      if (connector.password)
        connector.single_line = true;
    }

    if (c.hasOwnProperty('java'))
      connector.java = c['java'];

    console.log ('Creating connector for '+c.label+' '+c.dataType);

    connector.create(this, direction, pinType, dataType, /*isArray*/ dim, c["label"], hasValue);

    if (c.hasOwnProperty('enum')) {
      connector.setEnum (c['enum']);
      //conn.setValue (c["value"]);
    }

    //blueprint.connectorsCount ++;
    //connector.setID (blueprint.connectorsCount);
    connector.setID (blueprint.getNewConnectorId(this.getMaxId ()+1));

    if (direction == BPDirection.INPUT)
      this.leftColumn.appendChild(connector.getElement());
    else
	    this.rightColumn.appendChild(connector.getElement());

    if (hasValue) {
      //console.log ("[addConnectorFromJson] Setting value to "+connector.getLabel());
      connector.setValue (c['value']);
    }

	  this.connectors.push(connector);

	  //console.log ('Added connector '+connector.getId()+" "+connector.getLabel());

	  return (connector);
  }

  addConnector (direction, /*type,*/pinType, dataType, label, value, jenum, /*isArray*/dim) {
	  var c1 = {"dimensions":0 };
	  //c.type = type;
	  c1.pinType = pinType;
	  c1.dataType = dataType;
	  c1.label = label;
	  c1.value = value;
    c1.dimensions = dim;

	  if (jenum)
	    c1.enum = jenum;
/*
	  if (isArray)
	    c["array"] = isArray;
*/
	  //console.log ('dim = '+dim);
	  //console.log (c1);

	  //console.log ('Adding connector for '+c1.label+' '+c1.dimensions);

	  return (this.addConnectorFromJson(direction, c1));
  }

  deleteConnector (conn) {
    conn.remove ();
    this.connectors.splice (this.connectors.indexOf(conn), 1);
  }

  getConnector (id) {
    for (var i=0; i<this.connectors.length; i++) {
      //console.log ("Node "+this.id+" "+this.connectors[i].id+" == "+id);
      if (this.connectors[i].id == id)
        return (this.connectors[i]);
    }

    return (null);
  }

  makeDraggable() {
    var nodeRef = this;
    var elmnt = this.nodeElem;
    var deltaX = 0, deltaY = 0, x = 0, y = 0;
/*
    if (document.getElementById(elmnt.id + "header")) {
      // if present, the header is where you move the DIV from:
      document.getElementById(elmnt.id + "header").onmousedown = dragMouseDown;
      document.getElementById(elmnt.id + "header").oncontextmenu = nodeContextMenu;
    } else {
      // otherwise, move the DIV from anywhere inside the DIV:
      elmnt.onmousedown = dragMouseDown;
    }
*/
    elmnt.onmousedown = dragMouseDown;

    function dragMouseDown(e) {
      if (nodeRef.overValue)
        return;

      // Remove entry focus if any
      var entry = document.activeElement;
      if (entry) {
        //console.log (x);
        entry.blur ();
      }

      e = e || window.event;
      e.preventDefault();
      e.stopPropagation();

      endEdit (); // refocus blueprint div

      // get the mouse cursor position at startup:
      x = e.clientX;
      y = e.clientY;

      if (e.button == 0) {
        document.onmouseup = closedragNode;
        // call a function whenever the cursor moves:
        document.onmousemove = elementDrag;
        blueprint.trigger (BPEvent.NODE_SELECTED, nodeRef, e);
        blueprint.setStatus (BPStatus.MOVING_SELECTION);
      }
    }

    function elementDrag(e) {
      e = e || window.event;
      e.preventDefault();
      // calculate the new cursor position:
      deltaX = x - e.clientX;
      deltaY = y - e.clientY;
      x = e.clientX;
      y = e.clientY;
      // set the element's new position:
      //nodeRef.moveTo ((elmnt.offsetLeft - deltaX), (elmnt.offsetTop - deltaY));
      blueprint.trigger (BPEvent.SELECTION_MOVED, {dx:deltaX, dy:deltaY}, null);
    }

    function closedragNode() {
      // stop moving when mouse button is released:
      document.onmouseup = null;
      document.onmousemove = null;
      blueprint.setStatus (BPStatus.READY);
      blueprint.trigger (BPEvent.SELECTION_MOVED_COMPLETED, null, null);
    }

    function nodeContextMenu(e) {
      e.preventDefault();
      e.stopPropagation();

      //nodeRef.disconnect();
    }
  }

  signalConnected(connector) {
    console.log(connector.getLabel()+' connected');

    if (connector.any) {
      var connectedId = connector.connected[0];
      var typeId = blueprint.getConnector(connectedId).getPinType().id;

      for (var i=0; i<this.connectors.length; i++) {
        if (this.connectors[i].any) {
          this.connectors[i].setType(typeId);
        }
      }
    }
  }

  signalDisconnected(connector) {
    console.log(connector.getLabel()+' disconnected');

    if (connector.any) {
      var nAnyConneted = 0;

      // Check if all 'any' connector are disconnected
      for (var i=0; i<this.connectors.length; i++) {
        if (this.connectors[i].any) {
          if (this.connectors[i].isConnected())
            nAnyConneted ++;
        }
      }

      if (nAnyConneted == 0) {
        // Restore to 'any'
        for (var i=0; i<this.connectors.length; i++) {
          if (this.connectors[i].any) {
            console.log(connector.getLabel()+' back to ANY');
            this.connectors[i].setType(BPTypeID.ANY);
          }
        }
      }
    }
  }

  remove () {
    this.getElement().parentElement.removeChild(this.getElement());
  }

  refresh () {
    //console.log ("[node.refresh]");
    for (var i=0; i<this.connectors.length; i++) {
      this.connectors[i].refresh();
    }
  }

  toJSON () {
    var jo = { };

    jo.id = this.id;
    jo.version = this.version;
    jo.type = this.type;
    jo.name = this.name;
    jo.x = this.x;
    jo.y = this.y;

    if (this.plugin)
      jo.plugin = this.plugin;

    if (this.function_name)
      jo.function_name = this.function_name;

    if (this.canDelete !== null)
      jo.can_delete = this.canDelete;

    if (this.icon !== null)
      jo.icon = this.icon; //.replace (/"/g,'\\"');
/*
    if (this.type == BPNodeTypeID.BLUEPRINT)
      jo.blueprintId = this.blueprintId;
 */
    if (this.centerColumn && this.centerColumn.innerHTML)
      jo.inner_text = this.centerColumn.innerHTML; //.replace (/"/g,'\\"');

    /*if (this.type == BPNodeTypeID.OPERATOR)
      s += '"operatorId": '+this.operatorId +', ';*/

    if (this.addInputJson)
      jo.addInput = this.addInputJson;

    if (this.addOutputJson)
      jo.addOutput = this.addOutputJson;

    if (this.symbol)
      jo.symbol = this.symbol;

    if (this.java)
      jo.java = this.java;

    if (this.data)
      jo.data = this.data;

    if (this.options)
      jo.options = this.options;

    if (this.import)
      jo.import = this.import;

    if (this.jar)
      jo.jar = this.jar;

    if (this.classpath)
      jo.classpath = this.classpath;

    jo.input = [];
    jo.output = [];

    for (var i=0; i<this.connectors.length; i++) {
      if (this.connectors[i].direction == BPDirection.INPUT) {
        jo.input.push(this.connectors[i].toJSON());
      }
      else {
        jo.output.push(this.connectors[i].toJSON());
      }
    }

    return (jo);
  }

  toString() {
    //return (JSON.stringify(this.toJSON()));
    return ("Node [id="+this.id+", name="+this.name+", type="+this.type+"]");
  }

  createMainElement (name) {
    this.name = name;
    this.nodeElem = document.createElement('div');
    this.nodeElem.classList.add('node');
    this.nodeElem.classList.add('nodeunselected');
  }

  createRow () {
	  /* Row for left and right connectors */
	  this.row = document.createElement('div');
	  this.row.classList.add('row');
	  //this.row.classList.add('w3-container');
	  this.nodeElem.appendChild(this.row);
	}

	createOutputContainer () {
	  this.rightColumn = document.createElement('div');
	  this.rightColumn.classList.add('column');
	  this.rightColumn.classList.add('outcol');
    this.rightColumn.classList.add('w3-right');
	  this.row.appendChild(this.rightColumn);
	}

  createFromJson (j, preserveId = true) {
    this.version = typeof j["version"] !== 'undefined' ? j["version"] : this.version;
    this.inventory = typeof j["inventory"] !== 'undefined' ? j["inventory"] : true;
    var id = typeof j["id"] !== 'undefined' ? j["id"] : "0";
/*
    if (j["blueprintId"])
      this.blueprintId = j["blueprintId"];
*/
    this.plugin = typeof j["plugin"] !== 'undefined' ? j["plugin"] : this.plugin;
    this.function_name = typeof j["function_name"] !== 'undefined' ? j["function_name"] : this.function_name;
    this.canDelete = typeof j["can_delete"] !== 'undefined' ? j["can_delete"] : this.canDelete;
    this.icon = typeof j["icon"] !== 'undefined' ? j["icon"] : this.icon;

    this.import = j.hasOwnProperty("import") ? j.import : this.import;
    this.jar = j.hasOwnProperty("jar") ? j.jar : this.jar;
    this.classpath = j.hasOwnProperty("classpath") ? j.classpath : this.classpath;
    this.symbol = j.hasOwnProperty("symbol") ? j.symbol : this.symbol;
    this.java = j.hasOwnProperty("java") ? j.java : this.java;
    this.data = j.hasOwnProperty("data") ? j.data : this.data;
    this.options = j.hasOwnProperty("options") ? j.options : this.options;
  }
}

class Node extends NodeBase {

  constructor() {
    super();
  }

  create(type, name) {
    //var header;

    //console.log ("Creating node "+name);

    this.type = type;
    this.name = name;

    this.nodeElem = document.createElement('div');
    //this.nodeElem.setAttribute('id', id);
    this.nodeElem.classList.add('node');
    this.nodeElem.classList.add('nodeunselected');

    if (type == BPNodeTypeID.GET) {
      this.nodeElem.classList.add('var');
    }
    else if (type == BPNodeTypeID.OPERATOR || type == BPNodeTypeID.JUNCTION) {
      // No header
    }
    else {
      /* Header */
      this.header = document.createElement('div');
      this.header.setAttribute('id', this.nodeElem.id+"header");
      this.header.classList.add('nodeheader');
      this.header.innerHTML = name;
      //this.header.style.backgroundColor = nodeTypeColor[type];
      this.header.style.backgroundColor = blueprint.getNodeType(type).color;
      this.nodeElem.appendChild(this.header);
    }

	  /* Row for left and right connectors */
	  this.row = document.createElement('div');
	  this.row.classList.add('row');
	  this.nodeElem.appendChild(this.row);

	  if (type != BPNodeTypeID.GET) {
	    /* Input container */
	    this.leftColumn = document.createElement('div');
	    this.leftColumn.classList.add('column');
	    this.row.appendChild(this.leftColumn);
	  }

    /* Center container */
    this.centerColumn = document.createElement('div');
    this.centerColumn.classList.add('column');
    this.centerColumn.classList.add('n-text');
    this.row.appendChild(this.centerColumn);

	  /* Output container */
	  this.rightColumn = document.createElement('div');
	  this.rightColumn.classList.add('column');
	  this.rightColumn.classList.add('outcol');
	  this.rightColumn.classList.add('w3-right');
    /*this.rightColumn.style.cssFloat = 'right';*/
	  this.row.appendChild(this.rightColumn);
  }

  createFromJson (j, preserveId = true) {
    super.createFromJson (j, preserveId);

    this.create(j["type"], j["name"]);

    if (this.centerColumn)
      this.centerColumn.innerHTML = j["inner_text"] ? j["inner_text"] : " ";

    if (this.header !== null)
      this.setIcon (this.icon);

    //this.operatorId = j["operatorId"];

    if (j["input"]) {
      for (var i = 0; i < j["input"].length; i++) {
        var c = j["input"][i];
/*
        var isArray, single_line;

        if (c.hasOwnProperty('array'))
          isArray = c['array'];
*/
        var conn = this.addConnectorFromJson (BPDirection.INPUT, c);

        if (preserveId && c.hasOwnProperty('id'))
          conn.setID (j["input"][i]["id"]);

        if (c.hasOwnProperty('canDelete'))
          conn.canDelete = c["canDelete"];

        if (c.hasOwnProperty('references')) {
          var v = blueprint.getVariable (c["references"]);
          conn.references = v;
        }

        if (c.hasOwnProperty('must_connect')) {
          conn.mustConnect = c.must_connect;
          console.log ('conn.mustConnect = '+conn.mustConnect);
          console.log (conn.toString());
        }

        if (c.hasOwnProperty('not_null'))
          conn.notNull = c.not_null;

        if (c.hasOwnProperty('any'))
          conn.any = c.any;
      }
    }

    if (j["output"]) {
      for (var i = 0; i < j["output"].length; i++) {
        var c = j["output"][i];
        //var isArray = c["array"] === "undefined" ? false : c["array"];

        var conn = this.addConnectorFromJson (BPDirection.OUTPUT, c);

        if (preserveId && c.hasOwnProperty('id'))
          conn.setID (c["id"]);

        /*if (c.hasOwnProperty('type'))
          c.pinType = c.dataType = c.type;*/

        if (c["canDelete"] !== null)
          conn.canDelete = c["canDelete"];

        if (c["references"]) {
          var v = blueprint.getVariable (c["references"]);
          conn.references = v;
        }

        if (c.hasOwnProperty('any'))
          conn.any = c.any;
      }
    }

    var nodeSelf = this;

    if (j["addInput"]) {
      //console.log ("[node.createFromJson] Adding input button");
      this.addInputJson = j["addInput"];
      this.addInputButton = document.createElement('button');
      this.addInputButton.classList.add('btnAddPin');
      this.addInputButton.innerHTML = this.addInputJson.hasOwnProperty('button') ? this.addInputJson.button : 'Add input';

      //console.log ("[node.createFromJson] "+JSON.stringify(this.addInputJson));

      var pinType, dataType;

      if (this.addInputJson.hasOwnProperty('type'))
        pinType = dataType = this.addInputJson.type;
      else {
        pinType = this.addInputJson.pinType;
        dataType = this.addInputJson.dataType;
      }

      this.addInputButton.onclick = function (ev) {
        var conn = nodeSelf.addConnector (BPDirection.INPUT, pinType, dataType, j["addInput"]["label"], j["addInput"]["value"], null, Dimensions.SCALAR);
        conn.canDelete = true;
      }

      this.nodeElem.appendChild(this.addInputButton);
    }

    if (j["addOutput"]) {
      //console.log ("[node.createFromJson] Adding input button");
      this.addOutputJson = j["addOutput"];
      this.addOutputButton = document.createElement('button');
      this.addOutputButton.classList.add('btnAddPin');
      this.addOutputButton.innerHTML = this.addOutputJson.hasOwnProperty('button') ? this.addOutputJson.button : 'Add output';
      this.addOutputButton.style.cssFloat = 'right';

      //console.log ('[create] '+JSON.stringify(this.addOutputJson));

      var pinType, dataType;

      if (this.addOutputJson.hasOwnProperty('type'))
        pinType = dataType = this.addOutputJson.type;
      else {
        pinType = this.addOutputJson.pinType;
        dataType = this.addOutputJson.dataType;
      }

      var enumObj = j.addOutput.hasOwnProperty ('enum') ? j.addOutput['enum'] : null;

      this.addOutputButton.onclick = function (ev) {
        var conn = nodeSelf.addConnector (BPDirection.OUTPUT, pinType, dataType, j["addOutput"]["label"], j["addOutput"]["value"], enumObj, Dimensions.SCALAR);
        conn.canDelete = true;
      }

      this.nodeElem.appendChild(this.addOutputButton);
    }
  }
}

class NodeGet extends NodeBase {

  constructor() {
    super();
    this.type = BPNodeTypeID.GET;
  }

  create() {
    super.createMainElement('Get');

    this.nodeElem.classList.add('var');

	  /* Row for connector columns (only one in this case) */
	  super.createRow ();

	  /* Output container */
	  super.createOutputContainer ();
  }

  createFromJson (j, preserveId = true) {
    super.createFromJson (j, preserveId);
    this.create();

    for (var i = 0; i < j["output"].length; i++) {
      var c = j["output"][i];
      //var isArray = c["array"] === "undefined" ? false : c["array"];

      /*if (c.hasOwnProperty('type'))
        c.pinType = c.dataType = c.type;*/

      //console.log (JSON.stringify(c));

      var conn = this.addConnectorFromJson (BPDirection.OUTPUT, c);

      if (preserveId && c.hasOwnProperty('id'))
        conn.setID (c["id"]);

      if (c["canDelete"] !== null)
        conn.canDelete = c["canDelete"];

      if (c["references"]) {
        var v = blueprint.getVariable (c["references"]);
        conn.references = v;
      }
    }
  }

  // Overrides
  toString() {
    return ("Node [id="+this.id+", name="+this.name+", type="+this.type+", references="+this.connectors[0].references.getName()+"]");
  }

}

class NodeBlueprint extends Node {

  constructor() {
    super();
    this.type = BPNodeTypeID.BLUEPRINT;

    //this.method = null;
    this.blueprintId = null;
  }

  createFromJson (j, preserveId = true) {
    //this.method = j.method;
    this.blueprintId = j.blueprintId;
    //this.name = programIndex[this.blueprintId].name;
    this.name = j.name;
    j.name = this.name;
    super.createFromJson (j, preserveId);

    var nodeSelf = this;

    this.nodeElem.ondblclick = function (ev) {
        //console.log ('Open blueprint '+nodeSelf.blueprintId);
        //window.open('edit.htm?id='+nodeSelf.blueprintId);
        window.open('/blueprint/'+nodeSelf.blueprintId+'/edit');
    }

    console.log ("Creating blueprint node "+this.name+" "+this.blueprintId);
  }

  toJSON() {
    var jo = super.toJSON();
    //jo.method = this.method;
    jo.blueprintId = this.blueprintId;
    console.log ("blueprintId = "+this.blueprintId);
    return (jo);
  }
}

class NodeEvent extends Node {

  constructor() {
    super();
    this.type = BPNodeTypeID.EVENT;
    this.event = null;
  }

  createFromJson (j, preserveId = true) {
    this.event = j.event ;
    super.createFromJson (j, preserveId);

    console.log ("Creating event node "+this.name+" "+this.event);
  }

  toJSON() {
    var jo = super.toJSON();
    jo.event = this.event;
    return (jo);
  }

  // Overrides
  toString() {
    return ("Node [id="+this.id+", name="+this.name+", type="+this.type+", event="+this.event+"]");
  }
}
