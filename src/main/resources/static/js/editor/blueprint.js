'use strict';

const BlueprintType = {
  GENERIC : "GENERIC",
  MAIN    : "MAIN",
  EVENTS  : "EVENTS"
}

function bpAddNode (id, data) {
  if (id == MenuID.INCLUDE_BLUEPRINT) {
    var d = dialogMessage ('Editor', 'Getting blueprints...', DialogButtons.NONE, DialogIcon.RUNNING, null);

    callServer ("GET", "/program/"+_jbp.programId+"/index", null, function (xhttp) {
        if (xhttp.readyState == 4) {
          d.destroy();

          if (xhttp.status == 200) {
            var jindex = JSON.parse(xhttp.responseText);

            // Choose blueprint
            var dialog = new DialogChooseBlueprint ();

            dialog.callbackOK = function (dialog) {
              var id = dialog.getSelected();
              var bp = jindex[id];

              dialog.destroy();

              console.log ('Including blueprint '+bp.name+' ('+bp.method+') '+bp.id+' '+bp.internalId);
              console.log (bp);

              bp.type = BPNodeTypeID.BLUEPRINT;
              bp.blueprintId = bp.id;
              bp.blueprintInternalId = bp.internalId;

              var node = blueprint.addNodeFromJson(bp, false);
              //node.blueprintId = bp.id;
              node.setIcon ('<i class="icon i-project-diagram"></i>');
            };

            dialog.callbackCancel = function (dialog) { dialog.destroy(); };
            dialog.create(jindex);
          }
          else {
            showSnacknar(BPResult.ERROR, xhttp.statusText);
          }


        }
      }
    );
  }
  else
    blueprint.addNodeFromJson(blueprint.getAsset()["nodes"][id]);

  endEdit();
}

function bpGetSet (id, data) {
  var node;

  console.log ("[bpGetSet] id="+id+" data="+data);

  var v = blueprint.getVariable (data);

  if (id == 0)
    node = blueprint.addGetNode (v);
  else
    node = blueprint.addSetNode (v);

  node.moveTo (blueprint.mouse.x, blueprint.mouse.y);
  blueprint.onModified();
}

function edgeMouseEnter (e) {
  //console.log("[edgeMouseEnter]");
  e.darken ();

  for (var i=0; i<blueprint.edges.length; i++) {
    if (e != blueprint.edges[i])
      blueprint.edges[i].lighten();
  }
}

function edgeMouseLeave (e) {
  //console.log("[edgeMouseLeave]");
  for (var i=0; i<blueprint.edges.length; i++) {
    blueprint.edges[i].toNormalColor();
  }
}

class Blueprint {

  constructor() {
    this.name = "Blueprint";
    this.method = null;
    this.type = 1;
    //this.id = null;
    this.asset = { };
    this.types = [];
    this.nodeTypes = [];
    this.currentEdge = null;
    this.nodes = [];
    this.edges = [];
    this.selection = [];
    this.mouse = { x:0, y:0 };
    this.variables = [];
    this.entryPointNode = null;
    this.returnNode = null;
    this.zoom = 1.0;
    this.zoomFactor = 0.2;
    this.x0 = this.y0 = 0;
    this.bgElement = null;
    this.snapToGrid = true;
    this.grid = 20;
    this.blueprintList = null;
    this.callbackBeginModify = null;
    this.callbackModified = null;
    this.clipboard = null;
    this.code = 0;
    this.message = null;
  }
  
  setMessage(m) {
    this.message = m;
  }
  
  getMessage() {
    return(this.message);
  }
  
  getCode() {
    return(this.code);
  }
  
  setResult(c, m) {
    this.code = c;
    this.setMessage(m);
  }

  clear () {
    //console.log ("Blueprint.clear");
    while (this.nodes.length)
      this.deleteNodeForce (this.nodes[0]);

    this.currentEdge = null;
    //this.nodeCount = 0;
    //this.connectorsCount = 0;
    this.nodes = [];
    this.edges = [];
    this.selection = [];
    this.variables = [];
  }

  setConnectingEdge(edge) {
    this.currentEdge = edge;

    /*if (edge == null)
      //console.log ("Current edge resetted");*/
  }

  getConnectingEdge() {
    return (this.currentEdge);
  }

  setName(name) {
    this.name = name;
    document.title = name;
  }
/*
  setID(id) {
    this.id = id;
  }*/

  getName() {
    return (this.name);
  }

  getHeight () {
    return (this.bpDiv.offsetHeight);
  }

  setHeight (h) {
    this.bpDiv.style.height = h;
  }

  getTypes() {
    return (this.types);
  }

  getElement() {
    return (this.bpDiv);
  }

  returns() {
    return (this.returnNode.connectors.length > 1);
  }

  setCallbackBeginModify(f) {
    this.callbackBeginModify = f;
  }

  setCallbackModified(f) {
    this.callbackModified = f;
  }

  onBeginModify () {
    if (this.callbackBeginModify)
      this.callbackBeginModify ();
  }

  onModified () {
    if (this.callbackModified)
      this.callbackModified ();
  }
  
  getVariables() {
    return(this.variables);
  }

  setOrigin (x, y) {
    this.x0 = x;
    this.y0 = y;

    this.bgElement.style.backgroundPosition = blueprint.x0+'px '+blueprint.y0+'px';
  }

  setAsset(a) {
    this.asset = a;

    /* Store data types */
    var types = a["types"];

    for (var i=0; i<types.length; i++) {
      var t = new BPType();
      t.id = types[i]["id"];
      t.name = types[i]["name"];
      t.color = types[i]["color"];
      t.exec = types[i]["exec"] != null ? types[i]["exec"] : false;
      t.init = types[i].hasOwnProperty("init") ? types[i].init : null;
      //t.t_html_input_type = types[i]["html_input_type"];
      this.types.push (t);
      //console.log ("Added type "+t.name+" "+t.color + " "+t.exec);
    }

    /* Store node types */
    var nodeTypes = a["node_types"];
    for (var i=0; i<nodeTypes.length; i++) {
      var t = new BPNodeType();
      t.id = nodeTypes[i]["id"];
      t.name = nodeTypes[i]["name"];
      t.color = nodeTypes[i]["color"];
      this.nodeTypes.push (t);
      //console.log ("Added node type "+t.id+" "+t.name+" "+t.color);
    }

    /* Store link to entry point and return json */
    for (var i = 0; i < a["nodes"].length; i++) {
      var node = a["nodes"][i];

      if (node.type == BPNodeTypeID.ENTRY_POINT)
        this.jsonEntryPoint = node;

      else if (node.type == BPNodeTypeID.RETURN)
        this.jsonReturn = node;
    }

    this.jsonEntryPoint.x = 2 * this.grid;
    this.jsonEntryPoint.y = 2 * this.grid;

    this.jsonReturn.x = 15 * this.grid;
    this.jsonReturn.y = this.jsonEntryPoint.y;
  }

  getAsset() {
    return (this.asset);
  }

  getType (key) {
    for (var i=0; i<this.types.length; i++) {
      var found = (typeof key == "string") ? this.types[i].name == key: this.types[i].id == key;

      if (found)
        return (this.types[i]);
    }

    return (null);
  }

  getNodeType (id) {
    for (var i=0; i<this.nodeTypes.length; i++) {
      if (this.nodeTypes[i].id == id)
        return (this.nodeTypes[i]);
    }

    return (null);
  }

  setStatus (s) {
    this.status = s;

    if (debug == 1)
      this.StatusElement.innerHTML = "Status: "+status_str[s];
  }

  getStatus () {
    return (this.status);
  }

  coordsAbsToRel (p) {
    var x = p.x - this.bpDiv.offsetLeft;
    var y = p.y - this.bpDiv.offsetTop;

    //x *= this.zoom;
    //y *= this.zoom;

    return ({x:x, y:y});
  }

  clearSelection () {
    for (var i=0; i<this.selection.length; i++) {
      this.selection[i].setSelected (false);
    }

    this.selection.length = 0;
  }

  addToSelection (node) {
    if (!this.selection.includes (node)) {
      node.setSelected (true);
      this.selection.push (node);
    }
  }

  copySelection () {
    var jdata = {}, jnodes = [], jedges = [], /*copiedNodes = [],*/ copiedConnectors = [];

    for (var i=0; i<this.selection.length; i++) {
      var node = this.selection[i];

      if (node.type != BPNodeTypeID.ENTRY_POINT && node.type != BPNodeTypeID.RETURN && node.type != BPNodeTypeID.EVENT) {
        jnodes.push(node.toJSON());
        //copiedNodes.push(this.selection[i]);
        //console.log (node.getName()+ " has "+node.getConnectors().length+" connectors");
        copiedConnectors = copiedConnectors.concat(node.getConnectors());
      }
    }

    //console.log (copiedConnectors);

    for (var i=0; i<this.edges.length; i++) {
      var c1 = this.edges[i].getConnector1();
      var c2 = this.edges[i].getConnector2();

      if (copiedConnectors.includes(c1) && copiedConnectors.includes(c2))
        jedges.push(this.edges[i].toJSON());
    }

    console.log (jedges);

    jdata.nodes = jnodes;
    jdata.edges = jedges;

    console.log ("Copying to clipboard...");

    this.clipboard = JSON.stringify(jdata);
    copyStringToClipboard (JSON.stringify(jdata));

    //console.log (JSON.stringify(jdata));
  }

  paste (cbfunction) {
    var text, result = 0;

    console.log ("Paste...");
    this.setResult(0, 'OK');

    /*if (!this.clipboard)
      return;

    text = this.clipboard;*/
    
    //var selfBlueprint = this;

    navigator.clipboard.readText()
      .then(text => {
          // WARNING: this process is asyncronous
          
          var jdata = JSON.parse(text);
          var jnodes = jdata.nodes, jedges = jdata.edges;
          var addedNodes = [];

          if (jnodes) {

            substitutions = []; // reset substitutions

            // Nodes
            for (var i=0; i<jnodes.length; i++) {
              //console.log('Paste node '+jnodes[i].name);
              
              var pasteOK = true;
              
              switch (jnodes[i].type) {
                case BPNodeTypeID.GET:
                  // Check if variable exists
                  if (blueprint.getVariable(jnodes[i].output[0].references) == null) {
                    console.log('Node '+jnodes[i].name+' '+jnodes[i].references+' will not be pasted');
                    pasteOK = false;
                  }
                  break;
                
                case BPNodeTypeID.SET:
                  // Check if variable exists
                  if (blueprint.getVariable(jnodes[i].input[1].references) == null) {
                    console.log('Node '+jnodes[i].name+' '+jnodes[i].references+' will not be pasted');
                    pasteOK = false;
                  }
                  break;
              }
              
              if (pasteOK) {
                var node = blueprint.addNodeFromJson(jnodes[i], false);

                /* Change ids */
                node.setID (blueprint.getNewNodeId());
                node.moveDelta(20, 20);
                addedNodes.push(node);
              }
            }

            //console.log(substitutions);

            // Edges
            for (var i=0; i<jedges.length; i++) {
              var from = substitutions[jedges[i]["from"]];
              var to = substitutions[jedges[i]["to"]];
              
              if (from && to)
                this.connectByID (from, to);
            }

            blueprint.clearSelection();

            for (var i=0; i<addedNodes.length; i++)
              blueprint.addToSelection (addedNodes[i]);
              
            // Don't call this.onModified() here since this routine is asyncronous
            // Notify end of job
            if (cbfunction)
              cbfunction();
          }
 
      })
      .catch(err => {
        // maybe user didn't grant access to read from clipboard
        console.error('Paste error: ', err);
        blueprint.setResult(1, err);
      });
  }

  deleteSelection() {
    var n = 0;

    while (this.selection.length) {
      this.deleteNode (this.selection[0]);
      this.selection.splice (this.selection.indexOf(this.selection[0]), 1);
      n ++;
    }

    if (n)
      this.onModified();
  }

  selectAll () {
    this.clearSelection ();
    for (var i=0; i<this.nodes.length; i++) {
      this.addToSelection (this.nodes[i]);
    }
  }

  createNodeInstance (type) {
    var node;

    switch (type) {
      case BPNodeTypeID.GET:
        node = new NodeGet();
        break;

      case BPNodeTypeID.BLUEPRINT:
        node = new NodeBlueprint();
        break;

      case BPNodeTypeID.EVENT:
        node = new NodeEvent();
        break;

      default:
        node = new Node();
    }

    node.setID (this.getNewNodeId());

    return (node);
  }

  addNode (node) {
    node.setID (node.id); // For header
    node.setZoom(this.zoom);
    this.bpDiv.appendChild(node.nodeElem);
    node.moveTo (this.mouse.x, this.mouse.y);

    if (node.innerText) {
      node.setText (innerText);
    }

    // Make node draggable (after it has been added to page)
    node.makeDraggable();

    this.nodes.push(node);
    this.clearSelection ();
    this.addToSelection (node);
  }

  addNodeFromJson (j, preserveId = true) {
    console.log ("Adding node "+j.type+" "+j.name);

    var node = this.createNodeInstance (j.type);
    node.createFromJson(j, preserveId);
    this.addNode (node);

    if (j["x"] && j["y"])
      node.moveTo (j["x"], j["y"]);

    if (node.type == BPNodeTypeID.ENTRY_POINT)
      this.entryPointNode = node;

    if (node.type == BPNodeTypeID.RETURN)
      this.returnNode = node;

    this.onModified ();

    console.log("Created "+node.toString())

    return (node);
  }

  addGetNode (v) {
    var node = this.createNodeInstance (BPNodeTypeID.GET);

    //node.create(BPNodeTypeID.GET, "Get");
    node.create("Get");

    console.log ('Creating Get for '+v.name+' '+v.type+' '+v.dimensions);

    // Add an output connector
    var conn = node.addConnector (BPDirection.OUTPUT, v.type, v.type, v.name, null, v.enum, v.dimensions);
    node.moveTo (this.mouse.x, this.mouse.y);
    this.addNode (node);

    // Link to variable
    conn.references = v;
    v.ref ();

    return (node);
  }

  addSetNode (v) {
    var node = this.createNodeInstance (BPNodeTypeID.SET);
    node.create(BPNodeTypeID.SET, "Set");

    // Input connectors
    var conn = node.addConnector (BPDirection.INPUT, 'Exec', 'Exec', '', null, v.enum, Dimensions.SCALAR);
    var connVar = node.addConnector (BPDirection.INPUT, v.type, v.type, v.name, '', v.enum, v.dimensions);

    connVar.references = v;
    v.ref ();

    // Output connectors
    node.moveTo (this.mouse.x, this.mouse.y);
    var conn = node.addConnector (BPDirection.OUTPUT, 'Exec', 'Exec', '', null, v.enum, Dimensions.SCALAR);
    conn = node.addConnector (BPDirection.OUTPUT, v.type, v.type, '', null, v.enum, v.dimensions);
    this.addNode (node);

    return (node);
  }

  fromJson (j) {
    //console.log ('[blueprint] [fromJson] Start');
    //var variables = j["variables"]; //Added by application
    
    for (var i=0; i<j["variables"].length; i++) {
      var v = new Variable();
      v.fromJSON(j.variables[i]);

      console.log ("Adding variable "+v.getName());
      this.addVariable (v);
    }
    
    var nodes = j["nodes"];
    var edges = j["edges"];

    this.name = j["name"];
    this.type = j.hasOwnProperty("type") ? j.type : BlueprintType.GENERIC;

    // If method is missing, leave null. See toString() / toJSON()
    this.method = j.hasOwnProperty("method") ? j.method : null;

    console.log ('[blueprint] [fromJson] Adding nodes');

    for (var i=0; i<nodes.length; i++)
      this.addNodeFromJson(nodes[i]);

    console.log ('[blueprint] [fromJson] Adding edges');

    for (var i=0; i<edges.length; i++)
      this.connectByID (edges[i]["from"], edges[i]["to"]);

    if (j.hasOwnProperty('x0') && j.hasOwnProperty('y0'))
      this.setOrigin (j.x0, j.y0);
    else
      this.setOrigin (0, 0);

    console.log ('[blueprint] [fromJson] End');
  }

  deleteNodeForce (node) {
      node.disconnect ();
      node.remove ();
      this.nodes.splice (this.nodes.indexOf(node), 1);
  }

  deleteNode (node) {
    if (node.canDelete) {
      //console.log ("Deleting node "+node.getname());

      //if (node.type == BPNodeTypeID.GET)
      //  node.connectors[0].references.unref();
      for (var i=0; i<node.connectors.length; i++) {
        if (node.connectors[i].references) {
          node.connectors[i].references.unref();
          node.connectors[i].references = null;
        }
      }

      this.deleteNodeForce (node);
    }
  }

  selectNode (nodeSet, callback, includeBP) {
    this.contextMenu.remove();

    var menu = { "items": [ ] };
    var itemStr;

    //console.log(JSON.stringify(nodeSet));

    for (var i = 0; i < nodeSet.length; i++) {
      var node = nodeSet[i];
      var inventory = node["inventory"] != null ? node["inventory"] : true;
      //var id = node["id"] != null ? node["id"] : i;

      if (!inventory)
        continue;

      //console.log(JSON.stringify(node));

      itemStr = '{ "id": '+i+', "item": "'+node["name"].replace (/"/g,'\\"')+'" }';
      //console.log(itemStr);
      menu["items"].push (JSON.parse(itemStr));
    }

    menu["items"].sort(function(a, b) {
      return a.item.localeCompare(b.item);
    });


    if (includeBP)
      menu["items"].push ({ "id": MenuID.INCLUDE_BLUEPRINT, "item": "Blueprint..." });

    this.contextMenu.enableSearch(true);
    this.contextMenu.createFromJson(menu);

    this.contextMenu.setCallback(callback);
    beginEdit();
    this.contextMenu.show(this.x, this.y);
    contextMenu = this.contextMenu; /* Global object */
    //this.x = x;
    //this.y = y;
  }

  hideContextMenu () {
    this.contextMenu.remove();
    endEdit();
  }
  /*
  translatePoint (p) {
    p.x *= this.zoom;
    p.y *= this.zoom;

    return(p);
  }
  */
  createEdge (p1, p2) {
    var e = new Edge ();
    e.setZoom(this.zoom);
    e.create (this.svg, p1, p2);
    //e.create (this.svg, this.translatePoint(p1), this.translatePoint(p2));
    return (e);
  }

  getConnector (id) {
    for (var i=0; i<this.nodes.length; i++) {
      //console.log('Searching connector '+id+' in '+this.nodes[i].getName());
      var c = this.nodes[i].getConnector(id);

      if (c)
        return (c);
    }

    return (null);
  }

  getNode (id) {
    for (var i=0; i<this.nodes.length; i++) {
      if (this.nodes[i].id == id)
        return (this.nodes[i]);
    }

    return (null);
  }

  getNewNodeId () {
    var i = 1;

    while (true) {
      if (!this.getNode(i))
        return (i);

      i++;
    }
  }

  getNewConnectorId (startFrom) {
    var i = startFrom;

    while (true) {
      if (!this.getConnector(i))
        return (i);

      i++;
    }
  }

  getEdge (c1, c2) {
    for (var i=0; i<this.edges.length; i++) {
      if (this.edges[i].connector1 == c1 && this.edges[i].connector2 == c2)
        return (this.edges[i]);
    }

    return (null);
  }


  addEdge (c1, c2) {
    //console.log("addEdge");
    var edge = this.createEdge (c1.getConnectionPoint(), c2.getConnectionPoint());
    edge.setConnector1 (c1);
    edge.setConnector2 (c2);
    edge.setLocked (false);
    edge.setColor (c1.getColor());
    edge.setWidth (c1.getPinType().name == 'Exec' ? 3.8 : 2.8);
    edge.setCallbackMouseEnter (edgeMouseEnter);
    edge.setCallbackMouseLeave (edgeMouseLeave);
    this.edges.push (edge);
  }

  removeEdge (edge) {
    this.edges.splice (this.edges.indexOf(edge), 1);
    edge.remove ();
  }

  connectByID (id1, id2) {
    //console.log("Connecting "+id1+" to "+id2);
    var c1 = this.getConnector(id1);
    var c2 = this.getConnector(id2);

    //this.addEdge (c1, c2);
    c1.connectTo(c2);
  }

  redrawEdges () {
    for (var i = 0; i < this.edges.length; i++) {
      this.edges[i].redraw ();
    }
  }

  trigger (eventID, userData1, userData2) {
    //console.log ("[trigger "+eventID+"]");

    switch (eventID) {
      case BPEvent.CONNECTED:
        var c1 = userData1; //.direction == BPDirection.OUTPUT ? userData1 : userData2;
        var c2 = userData2; //.direction == BPDirection.INPUT ? userData2 : userData1;
        this.addEdge (c1, c2);

        if (this.getConnectingEdge())
          this.getConnectingEdge().remove();

        this.setConnectingEdge(null);
        blueprint.redrawEdges ();
        this.onModified ();
        break;

      case BPEvent.DISCONNECTED:
        var c1 = userData1;
        var c2 = userData2;
        var e = this.getEdge (c1, c2);
        if (e)
          this.removeEdge (e);
        else {
          e = this.getEdge (c2, c1);
          if (e)
            this.removeEdge (e);
        }
        this.onModified ();
        break;

      case BPEvent.NODE_MOVED:
        var node = userData1;
        break;

      case BPEvent.SELECTION_MOVED:
        /* User is moving selection but not released yet */
        var delta = userData1;

        for (var i=0; i<this.selection.length; i++) {
          this.selection[i].moveDelta(delta.dx, delta.dy);
        }

        this.redrawEdges ();
        break;

      case BPEvent.SELECTION_MOVED_COMPLETED:
        /* Selection moved */
        for (var i=0; i<this.selection.length; i++) {
          if (this.snapToGrid) {
            // Cell size
            var g = Math.round (this.grid * this.zoom);

            var x = this.selection[i].x - this.x0;
            x = Math.round (x / g) * g;
            x += this.x0;

            var y = this.selection[i].y - this.y0;
            y = Math.round (y / g) * g;
            y += this.y0;

            console.log('Snapping to '+x+', '+y);

            this.selection[i].moveTo (x, y);
          }
        }

        this.redrawEdges ();
        this.onModified ();
        break;

      case BPEvent.NODE_SELECTED:
        if (!event.ctrlKey && !userData1.getSelected ())
          this.clearSelection ();

        this.addToSelection (userData1);
        //console.log (this.toString());
        break;

      default:
        break;
    }
  }

  addInput (type, name, value) {
    //addConnector (direction, type, name, value, jenum, isArray)
    var conn = this.entryPointNode.addConnector (BPDirection.OUTPUT, type, type, name, value, null, Dimensions.SCALAR);
    this.redrawEdges();
    return (conn);
  }

  addOutput (type, name, value) {
    var conn = this.returnNode.addConnector (BPDirection.INPUT, type, type, name, value, null, Dimensions.SCALAR);
    //conn.setDefaultValue ();
    this.redrawEdges();
    return (conn);
  }

  getVariable (id) {
    for (var i=0; i<this.variables.length; i++) {
      if (this.variables[i].id == id)
        return (this.variables[i]);
    }

    return (null);
  }

  getVariableByNameAndScope (name, scope) {
    for (var i=0; i<this.variables.length; i++) {
      var v = this.variables[i];
      
      if (!v.getName().localeCompare(name)) {
        if (scope == Scope.ALL)
          return (v);
          
        if ((scope == Scope.LOCAL && !v.isGlobal()) || (scope == Scope.GLOBAL && v.isGlobal())) {
          console.log("Found "+v.getName()+" "+scope+" "+v.isGlobal());
          return (v);
        }
      }
    }

    return (null);
  }

  getVariableByName (name) {
    return(this.getVariableByNameAndScope(name, Scope.ALL));
  }

  getAvailableVariableName () {
    var v, name;
    var i = 0;

    while (true) {
      name = "Variable_"+i;
      v = this.getVariableByName (name);

      if (!v)
        return (name);

      i ++;
    }
  }

  getVarNewId () {
    var i = 1;

    while (true) {
      var v = this.getVariable (i);

      if (!v)
        return (i);

      i ++;
    }
  }

  addVariable (v) {
    if (!this.getVariable(v.id)) {
      console.log ("Adding "+v.toString());
      this.variables.push(v);
    }
    else {
      console.log ("Variable "+v.getName()+" already in blueprint");
    }

    return (v);
  }

  addNewVariable (global = false) {
    var v = new Variable ();
    v.create (blueprint.getAvailableVariableName (), 'Integer', Dimensions.SCALAR);
    v.id = uuidv4(); //this.getVarNewId ();
    v.setGlobal(global);
    v.reset();
    this.addVariable (v);

    return (v);
  }

  deleteVariable (v) {
    //console.log ("Deleting "+v.id+" "+v.name);
    this.variables.splice (this.variables.indexOf(v), 1);

  }

  refreshNodes() {
    for (var i=0; i<this.nodes.length; i++) {
      this.nodes[i].refresh();
    }

    blueprint.redrawEdges();
  }

  setVariableName (v, newNameIn) {
    var renamed = false;
    var newName = newNameIn.trim();

    if (newName.length > 0) {
      var vtemp = this.getVariableByName (newName);

      if (!vtemp || vtemp.isGlobal() != v.isGlobal()) {
        v.name = newName;
        renamed = true;
      }
    }

    if (v.referenced) {
      this.refreshNodes()
    }

    return (renamed);
  }

  setVariableType (v, newType) {
    v.type = newType;
    return (v);
  }

  setVariableValue (v, value) {
    v.set(value);
    return (v);
  }

  moveDelta (dx, dy) {
    for (var i=0; i<this.nodes.length; i++) {
      this.nodes[i].moveDelta(dx, dy);
    }

    this.redrawEdges ();

    this.x0 -= dx;
    this.y0 -= dy;

    this.bgElement.style.backgroundPosition = this.x0+'px '+this.y0+'px';
  }

  init() {
    //this.bpDiv = document.getElementById(id);
    this.bpDiv = document.getElementsByClassName('blueprint')[0];
    this.bgElement = document.getElementsByClassName('bp-bg')[0];
    this.bgElement.style.visibility = 'visible';
    this.contextMenu = new ContextMenu();
    this.contextMenu.init();

    //this.bpDiv.style.border = "thick solid #0000FF";

    /* SVG */
    this.svg = document.createElementNS("http://www.w3.org/2000/svg", 'svg');
    this.svg.setAttribute('id', "svg");
    //this.svg = document.getElementById('svg');
    this.svg.ns = this.svg.namespaceURI;
    this.bpDiv.appendChild(this.svg);

    if (debug == 1) {
      this.StatusElement = document.getElementById('status');
      //this.StatusElement = document.createElement('div');
      //this.StatusElement.id = "status";
      this.StatusElement.innerHTML = "Status";
      this.bpDiv.appendChild (this.StatusElement);
      this.mouseElement = document.getElementById('mouse');
      //this.mouseElement = document.createElement('div');
      //this.mouseElement.id = "mouse";
      this.mouseElement.innerHTML = "";
      this.bpDiv.appendChild (this.mouseElement);
    }

    this.setStatus (BPStatus.READY);

    var blueprintSelf = this;

    this.bpDiv.ondragover = function(e) {
      e.preventDefault();
      blueprint.mouse = blueprint.coordsAbsToRel({ x:e.pageX, y:e.pageY });
    }

    this.bpDiv.ondrop = function(e) {
      e.preventDefault();

			var data = e.dataTransfer.getData("data");
      console.log (data);

      try {
        var jdata = JSON.parse (data);

        if (jdata.tag == 'VARIABLE') {
          var v = blueprintSelf.getVariable (jdata.id);

          if (v) {
            //console.log ("[bpDiv.ondrop] Dropped variable "+v.id+ " "+v.name);

            var json = `
              {
                "items": [
                   { "id": 0, "item": "Get", "data":"`+v.id+`" },
                   { "id": 1, "item": "Set", "data":"`+v.id+`" }
                ]
              }
            `;

            //console.log (json);

            blueprintSelf.contextMenu.remove();
            blueprintSelf.contextMenu.enableSearch (false);
            blueprintSelf.contextMenu.createFromJson(JSON.parse(json));
            blueprintSelf.contextMenu.setCallback(bpGetSet);
            beginEdit();
            blueprintSelf.contextMenu.show(e.x, e.y);
          }
          else
            console.error("Variable "+varId+" not found");
        }
      } catch (err) {
        console.error(err.message);
      }
    }

    this.bpDiv.onmouseenter = function (ev) {
      //console.log ("Blueprint has focus");
    }

    this.bpDiv.onmouseleave = function (ev) {
      //console.log ("Blueprint lost focus");

      ev.preventDefault();
      this.mouse = { x:ev.offsetX, y:ev.offsetY };

      switch (blueprint.getStatus()) {
        case BPStatus.BP_DRAGGING:
          blueprint.setStatus(BPStatus.READY);
          break;
        default:
          break;
      }
    }

    this.bpDiv.oncontextmenu = function(e) {
      e.preventDefault();

      blueprint.onBeginModify ();

      blueprint.x = e.pageX;
      blueprint.y = e.pageY;
      blueprint.selectNode (blueprint.asset["nodes"], bpAddNode, true);
    }

    this.bpDiv.onmousedown = function(e) {
      console.log ("[onmousedown] Blueprint");

      blueprint.mouse = blueprint.coordsAbsToRel({ x:e.pageX, y:e.pageY });
            //blueprint.mouse.x *= 1 / blueprint.zoom;
            //blueprint.mouse.y *= 1 / blueprint.zoom;
      //e.preventDefault();

      switch (e.button) {
        case 0:
          blueprint.clearSelection ();
          break;
        case 1:
          //console.log ("Drag");
          blueprint.setStatus (BPStatus.BP_DRAGGING);
          this.dragX = e.pageX;
          this.dragY = e.pageY;
          break;
        default:
          //console.log ("e.button = "+e.button);
          break;
      }

      blueprint.hideContextMenu ();
    }

    this.bpDiv.onmousemove = function(e) {
      blueprint.mouse = blueprint.coordsAbsToRel({ x:e.pageX, y:e.pageY });

      if (debug)
        blueprint.mouseElement.innerHTML = blueprint.mouse.x+" "+blueprint.mouse.y;

      switch (blueprint.getStatus()) {
        case BPStatus.CONNECTING:
          e.preventDefault();
          if (blueprint.currentEdge) {
            //console.log ("Moving mouse "+e.offsetX+" "+e.offsetY);
            //var end = this.mouse;
            var end = blueprint.coordsAbsToRel({ x:e.pageX, y:e.pageY });
            //end.x *= 1 / blueprint.zoom;
            //end.y *= 1 / blueprint.zoom;
            blueprint.currentEdge.setTrackingPoint (end);
            //blueprint.currentEdge.redraw();
          }
          break;

        case BPStatus.BP_DRAGGING:
          var dx = this.dragX - e.pageX;
          var dy = this.dragY - e.pageY;

          blueprint.moveDelta (dx, dy);

          this.dragX = e.pageX;
          this.dragY = e.pageY;
          break;

        default:
          break;
      }
    }

    this.bpDiv.onmouseup = function(e) {
      e.preventDefault();
      this.mouse = { x:e.offsetX, y:e.offsetY };

      switch (blueprint.getStatus()) {
        case BPStatus.CONNECTING:
          if (blueprint.currentEdge) {
            if (!blueprint.currentEdge.connector1.isConnected ())
              blueprint.currentEdge.connector1.setFilled (false);

            blueprint.currentEdge.remove();
            blueprint.setConnectingEdge(null);
          }

          blueprint.setStatus(BPStatus.READY);
          break;

        case BPStatus.BP_DRAGGING:
          blueprint.setStatus(BPStatus.READY);
          break;
        default:
          break;
      }
    }

    this.bpDiv.onwheel = function(e) {
      //console.log (event.deltaY);
      if (event.deltaY > 0) {
        if (blueprint.zoom > 0.4)
          blueprint.zoomOut();
      }
      else {
        if (blueprint.zoom < 1)
          blueprint.zoomIn();
      }
    }

    this.bpDiv.onfocus = function(e) {
      //console.log ("Blueprint has focus");
    }

    this.bpDiv.onblur = function(e) {
      //console.log ("Blueprint lost focus");
    }

  }

  setZoom (x) {
    //console.log ("[setZoom] x = "+x);
    this.zoom = x.toFixed(2);

    for (var i=0; i<this.nodes.length; i++) {
      this.nodes[i].setZoom(this.zoom);
      //setElementZoom (this.nodes[i].getElement(), this.zoom, -this.nodes[i].x, -this.nodes[i].y);
    }

    for (var i = 0; i < this.edges.length; i++) {
      this.edges[i].setZoom (this.zoom);
    }

    this.redrawEdges ();

    this.bgElement.style.backgroundSize = (100 * this.zoom)+'px '+(100 * this.zoom)+'px';
  }

  zoomIn () {
    this.setZoom (Number(this.zoom) + Number(this.zoomFactor));
  }

  zoomOut () {
    this.setZoom (Number(this.zoom) - Number(this.zoomFactor));
  }

  /* Used when running from web */
  getInputArray () {
    var input = [];

    input[0] = { "type": 'Exec',  "label": "" };

    for (var i=1; i<this.entryPointNode.connectors.length; i++) {
      if (!this.entryPointNode.connectors[i].exec) {

        input[i] = {"label":this.entryPointNode.connectors[i].getLabel(), "type":this.entryPointNode.connectors[i].dataType.name, "dimensions":this.entryPointNode.connectors[i].dimensions};

        if (this.entryPointNode.connectors[i].dimensions == 0) {
          switch (this.entryPointNode.connectors[i].dataType.name) {
            case 'Integer':
              input[i].value = 0;
              break;
            case 'Double':
              input[i].value = 0.0;
              break;
            case 'Boolean':
              input[i].value = true;
              break;
            case 'String':
              input[i].value = "";
              break;
            case 'JSONObject':
              input[i].value = { };
              break;
            default:
              break;
          }
        }
        else
          input[i].value = [];
      }
    }

    return (input);
  }

  /* Used when running from web */
  getInputArrayAsString () {
    return (JSON.stringify(this.getInputArray ()));
  }

  toJson () {
    var jo = {
      "tag":"BLUEPRINT",
      //"id":this.id,
      "x0":this.x0, "y0":this.y0,
      "type":this.type, // Keep before name (see BlueprintEntity.setName())
      "name":this.name,
      "method":this.method,
    };

    /* Variables and types */
    jo.variables = [];
    jo.types = [];

    //console.log (this.variables);

    for (var i=0; i<this.variables.length; i++) {
      /*if (this.variables[i].isGlobal())
        continue;*/

      var jv = this.variables[i].toJSON();

      var t = this.getType (jv.type);
      jv.typeName = t.name;
      jo.types.push(t);

      jo.variables.push(jv);

      //console.log (jo.variables);
    }

    /* Input */
    jo.input = [];

    var input = '"input": [', /*k = 0,*/ value;

    // Check if returns a value
    //if (!this.returns())
      jo.input.push({ "type": "Exec",  "label": "" });

    if (this.entryPointNode) {
    for (var i=0; i<this.entryPointNode.connectors.length; i++) {
      if (!this.entryPointNode.connectors[i].exec) {
          //input += (this.returns() ? '' : ', ');

        //input += this.entryPointNode.connectors[i].toString();
        var jconn = this.entryPointNode.connectors[i].toJSON();
        jconn.value = this.entryPointNode.connectors[i].getDefaultValue();

        jo.input.push(jconn);
        //k ++;
      }
    }
    }

    /* Output */
    jo.output = [];

    if (this.returnNode) {
    if (true/*!this.returns()*/) {
      var jexec = { "type": "Exec", "label": ""};
      jo.output.push(jexec);
    }

    for (var i=0; i<this.returnNode.connectors.length; i++) {
      if (!this.returnNode.connectors[i].exec) {
        var jconn = this.returnNode.connectors[i].toJSON();

        jconn.java = { "references": {"type": this.returnNode.connectors[i].getDataType().name, "name":"out_"+i }};

        if (jconn.hasOwnProperty('value'))
          delete jconn.value;

        //console.log (jconn);

        jo.output.push(jconn);
      }
    }
    }

    /* Nodes */
    jo.nodes = [];

    for (var i=0; i<this.nodes.length; i++) {
      //console.log ('Saving '+this.nodes[i].toString());
      jo.nodes.push(this.nodes[i].toJSON());
    }

    /* Edges */
    jo.edges = [];

    for (var i=0; i<this.edges.length; i++) {
      jo.edges.push(this.edges[i].toJSON());
    }

    return(jo);
  }

  toString () {
    return(JSON.stringify(this.toJson()));
  }
}


function bpLoadCallback (xhttp) {
  modalHide();

  if (xhttp.readyState == 4) { // Request finished and response is ready

    //console.log (xhttp.responseText);

    if (xhttp.status == 200) {
      var j = JSON.parse(xhttp.responseText);
      appLoadBlueprint (j);
      setStatus (BPEditStatus.DEPLOYED);
      //blueprint.getElement().innerHTML =  '';
      showSnacknar(BPResult.SUCCESS, 'Blueprint '+j["name"]+' loaded', 2000);
    }
    else {
      blueprint.getElement().innerHTML = xhttp.statusText;
    }
  }
  else if (xhttp.readyState == 3) { // Processing request
    //blueprint.getElement().innerHTML = "Getting blueprint...";
  }
  else {
    //blueprint.getElement().innerHTML = xhttp.statusText;
  }
}


function bpStart(id)
{
  blueprint = new Blueprint ();
  blueprint.init ();

  // Get blueprint id
  var args = window.location.pathname.split("/");
  blueprint_id = args[2];
  console.log ('blueprint_id = '+blueprint_id);
}
