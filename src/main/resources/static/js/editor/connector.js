'use strict';

function checkFieldNull (element) {
  if (element.value === "")
    element.classList.add('inputNull');
  else
    element.classList.remove('inputNull');
}

function editText (ev) {
  //var varName = ev.target.getAttribute('_name');
  var id = ev.target.getAttribute('_id');
  var conn = blueprint.getConnector (id);

  console.log ("[editText] _id="+id+ " "+conn.getLabel());

  var dialog = new DialogData ();

  dialog.callbackOK = function (dialog) {
    conn.setValue (dialog.getData ());
    dialog.destroy ();
    endEdit();
    blueprint.onModified ();
  }

  dialog.callbackCancel = function (dialog) { dialog.destroy(); endEdit(); };
  beginEdit();
  dialog.create('String', conn.getLabel(), conn.getValue(), DataFlags.EDIT_ONLY_VALUE);
}

class Connector {

  constructor() {
    this.id = 0;
    //this.type = ___BPType.INTEGER;

    //this.type = null;
    this.dataType = null;
    this.pinType = null;

    this.canDelete = false;
    this.direction = BPDirection.INPUT;
    this.multiconnect = true;
    this.element = null;
    this.labelStr = "Label";
    this.connected = [ ];
    this.value = null;
    this.single_line = false;
    this.password = false;
    this.hasValue = false;
    this.nullable = false;
    this.notNull = false;

    this.dimensions = Dimensions.SCALAR;
    this.java = null;

    // Attribute not from json
    this.valueElem = null;
    this.inputElem = null;
    this.owner = null; // Node
    this.any = false;
    this.elemArray = null;
  }

  getElement() {
    return (this.element);
  }

  getId() {
    return (this.id);
  }

  getLabel() {
    return (this.label.innerHTML);
  }

  setLabel(l) {
    this.label.innerHTML = l;
  }

  setColor (color) {
    this.pin.style.borderColor = color;
  }

  getColor () {
    return (this.pin.style.borderColor);
  }

  getOwner () {
    return (this.owner);
  }

  setFilled (filled) {
    this.pin.style.backgroundColor = filled ? this.getColor () : "rgba(0, 0, 0, 0.3)";
  }

  setID (id) {
    //console.log("Setting "+this.getLabel()+"'s id: "+id);
    this.id = id;
    this.getElement().setAttribute('id', id);
    this.pin.setAttribute('id', this.id);

    if (this.valueElem) {
      this.valueElem.setAttribute('id', "value_"+this.id);

      if (this.value)
        this.setValue (this.value);
    }

    /*if (this.editButton) {
      this.editButton.setAttribute('_id', this.id);
    }*/
  }

  setEnum (j) {
    //console.log("[setEnum] "+JSON.stringify(j));
    this.enumLabels = [];
    for (var i=0; i<j.length; i++) {
      this.enumLabels.push (j[i]);
      //console.log("[setEnum] "+this.enumLabels[i]);
    }
  }
/*
  setArray (a) {
    this.array = a;
  }
*/
  setArrayElemVisible(visible) {
    if (this.elemArray)
      this.elemArray.style.display = visible ? "inline" : "none";
  }
  
  setDimensions (d) {
    this.dimensions = d;
  
    if (this.elemArray && d > 0) {
      var iconName = this.dimensions == Dimensions.ARRAY ? 'ellipsis-h' : 'i-th';
      this.elemArray.innerHTML = "&nbsp;<i class=\"icon "+iconName+"\" style=\"color:darkgray\"></i>";
    }

    this.setArrayElemVisible(d > 0);
    this.showValue(d == 0);
  }
  
  getDimensions () {
    return(this.dimensions);
  }

  setValue (v) {
    //console.log("[connector] [setValue] "+this.labelStr+" this.hasValue = "+this.hasValue);

    if (/*this.array*/this.dimensions > Dimensions.SCALAR || /*!this.valueElem ||*/ !this.hasValue)
      return;

    this.value = v;

    // https://stackoverflow.com/questions/469357/html-text-input-allow-only-numeric-input
    switch (this.dataType.name) {
      case 'Integer':
        if (this.enumLabels) {

          var combo = '<select class="select-css" onchange="blueprint.onModified ();">';

          for (var i=0; i<this.enumLabels.length; i++) {
            var eVal = typeof this.enumLabels[i] === 'string' ? i : this.enumLabels[i].value;
            var eLabel = typeof this.enumLabels[i] === 'string' ? this.enumLabels[i] : this.enumLabels[i].label;
            var selected = eVal == v ? ' selected="selected"' : '';

            combo += '<option value="'+eVal+'" '+selected+'>'+eLabel+'</option>';
          }
          combo += '</select>';

          this.valueElem.innerHTML = combo;
        }
        else {
          this.valueElem.innerHTML = '<input class="inputConnector" value="'+v+'" onfocus="beginEdit();" onblur="endEdit();" onchange="blueprint.onModified ();">';
          setInputFilter(this.valueElem.childNodes[0], function(value) { return /^-?\d*$/.test(value); });
        }
        break;

      case 'Double':
        this.valueElem.innerHTML = '<input class="inputConnector" value="'+v+'" onfocus="beginEdit();" onblur="endEdit();" onchange="blueprint.onModified ();">';
        setInputFilter(this.valueElem.childNodes[0], function(value) { return /^-?\d*[.,]?\d*$/.test(value); });
        break;

      case 'String':
        var multi_line_flags = this.single_line ? '' : 'onclick="editText(event);" readonly';
        var single_line_flags = this.single_line ? 'onfocus="beginEdit();" onblur="endEdit();"' : '';
        var multi_line = this.single_line ? '' : 'onclick="editText(event);" readonly';
        var text_type = this.password ? 'password' : 'text';

        this.valueElem.innerHTML = '<input id="str_'+this.id+'" _id="'+this.id+'" class="inputConnector" type="'+text_type+'" value="'+v.replace(/"/g,'&quot;')+'"  oninput="checkFieldNull(event.target);" onchange="blueprint.onModified ();" '+multi_line_flags+' '+single_line_flags+'>';
        checkFieldNull (this.valueElem.childNodes[0]);
        break;

      case 'Boolean':
        var checked = v ? "checked" : "";
        this.valueElem.innerHTML = '<input class="w3-check" type="checkbox" style="top:3px;width:18px;height:18px;" onchange="blueprint.onModified ();" '+checked+'>';
        break;

      default:
        this.valueElem.innerHTML = "";
        break;
    }

    //this.valueElem.innerHTML = '<input id="input_'+this.id+'" type="'+this.type.t_html_input_type+'" name="'+this.labelStr+'" value="'+v+'">';
  }

  getValue () {
    //console.log("[getValue] getting value of input_"+this.id+" "+this.name+" type.id = "+this.type.id);

    if (!this.valueElem)
      return (null);

    var inputElem = this.valueElem.childNodes[0];

    if (!inputElem)
      return (null);

    switch (this.dataType.name) {
      case 'Boolean':
        //console.log ("Boolean = "+inputElem.checked);
        return (inputElem.checked);
        break;
      case 'Integer':
        return (inputElem.value ? parseInt(inputElem.value) : 0);
        break;
      case 'Double':
        return (inputElem.value ? parseFloat(inputElem.value) : 0);
        break;
      case 'String':
        return (this.single_line ? inputElem.value : this.value);
      default:
        return (null);
        break;
    }
  }

  showValue (show) {
    if (this.valueElem) {
      this.valueElem.style.display = (show === true /*&& this.value*/) ? "inline-block" : "none";

      /*if (this.editButton)
        this.editButton.style.display = (show === true) ? "inline-block" : "none";*/
    }
  }

  getConnectionPoint () {
    var p = getAttachPoint(this.pin);
    //p.x *= this.owner.zoom;
    //p.y *= this.owner.zoom;
    var p = blueprint.coordsAbsToRel(p);
    return (p);
  }

  addConnection (c) {
    if (!this.multiconnect)
      this.disconnectAll ();

    this.connected.push (c.id);
    this.setFilled (true);

    if (this.direction == BPDirection.INPUT)
      this.showValue (false);

    //console.log("[addConnection] "+this.id+" connected to "+c.id);
  }

  connectTo (c) {
    if (!this.isConnectedTo (c)) {
      this.addConnection (c);
      c.addConnection (this);
      blueprint.trigger (BPEvent.CONNECTED, c, this);
    }
    //console.log("[connectTo] "+this.id+" already connected to "+c.id);
  }

  isConnected () {
    return (this.connected.length > 0);
  }

  isConnectedTo (c) {
    return (this.connected.includes (c.id));
  }

  deleteConnection (c) {
    this.connected.splice (this.connected.indexOf(c.id), 1);
    //console.log("[deleteConnection] "+(this.isConnected () ? "still" : "not")+" connected");

    this.setFilled (this.isConnected ());
    this.showValue (!this.isConnected ());

    // Signal
    this.owner.signalDisconnected(this);
  }

  disconnectFrom (c) {
    this.deleteConnection (c);
    c.deleteConnection (this);

    // Signal
    blueprint.trigger (BPEvent.DISCONNECTED, this, c);
  }

  disconnectAll () {
    //console.log("[disconnectAll] connected.length = "+this.connected.length);

    undo.begin();

    while (this.isConnected ()) {
      var c = blueprint.getConnector (this.connected[0]);
      this.disconnectFrom (c);
    }

    undo.end();
    blueprint.onModified();
  }

  canConnectTo (c) {
    if ((this.owner == c.owner) || (this.direction == c.direction) || (this.dimensions != c.dimensions))
      return (false);

    if ((this.exec && !c.exec) || (!this.exec && c.exec))
      return (false);

    //console.log("this.pinType = "+this.pinType.id+" c.pinType = "+c.pinType.id+" ("+BPTypeID.ANY+")");
    //if (this.pinType.id == BPTypeID.ANY ^ c.pinType.id == BPTypeID.ANY)
    console.log(this.getLabel()+".any = "+this.any+" "+c.getLabel()+".any = "+c.any);

    var onlyOneisAny = this.any ^ c.any;
    var atLeastOneisAny = this.any | c.any;

    if (onlyOneisAny)
      return (true);

    if (this.pinType != c.pinType && atLeastOneisAny)
      return (true);

    if (this.pinType != c.pinType)
      return (false);

    return (true);
  }

  getDefaultValue () {
    var value = null;

    switch (this.dataType.name) {
      case 'Integer':
      case 'Double':
        value = 0;
        break;
      case 'String':
        value = "";
        break;
      case BPTypeID.BLOOLEAN:
        value = true;
        break;
      default:
        break;
    }

    return (value);
  }

  setDefaultValue () {
    if (!this.hasValue)
      return;

    this.setValue (this.getDefaultValue ());
  }

  setDataType (typeId) {
    this.dataType = blueprint.getType(typeId);
    //this.setColor (this.dataType.color);
  }

  setPinType (typeId) {
    this.pinType = blueprint.getType(typeId);
    this.setColor (this.pinType.color);
  }

  getPinType () {
    return(this.pinType);
  }

  getDataType () {
    return(this.dataType);
  }

  setType (typeId) {
    this.setDataType (typeId);
    this.setPinType (typeId);
  }

  createValueElement () {
    this.valueElem = document.createElement('div');
    this.valueElem.className = "value";
    //this.element.appendChild(this.valueElem);

    var selfConn = this;

    this.valueElem/*.childNodes[0]*/.onmousedown = function (e) {
      //console.log('input onmousedown');
      //e.preventDefault();
      e.stopPropagation();
    }

    /* Signal that mouse is over the connector */
    this.valueElem.onmouseover = function (e) {
      selfConn.owner.overValue = true;
    }

    this.valueElem.onmouseout = function (e) {
      selfConn.owner.overValue = false;
    }

    return (this.valueElem);
  }

  create(owner, direction, pinType, dataType, /*isArray*/dimensions, name, withValue) {
    //console.log('pinType = '+pinType+' name = '+name);

    this.owner = owner; /* Node */
    this.direction = direction;
    //this.type = blueprint.getType(type);
    this.labelStr = name;
    this.hasValue = withValue;

    this.element = document.createElement('div');
    this.element.className = "connector";

    this.pin = document.createElement('div');
    this.pin.className = "pin";
    //this.pin.tabIndex = "0"; /* Important for event.relatedTarget */

    //this.setType (type);
    this.setPinType (pinType);
    this.setDataType (dataType);
    this.element.title = this.getPinType().name;
    this.exec = this.pinType.exec ? this.pinType.exec : false;
    //this.setArray (isArray === true);
    this.dimensions = dimensions;
    //console.log ('[connector.create] '+name+' isArray = '+isArray);

    if (pinType == BPTypeID.ANY || pinType == "Any")
      this.any = true;

    this.label = document.createElement('div');
    this.label.className = "label";
    this.label.innerHTML = name;

    //console.log ("[Connector] [create] "+this.labelStr+" "+this.type.name +" hasValue = "+this.hasValue);

    if (this.pinType.exec) {
      this.pin.style.borderTopLeftRadius = 0;
      this.pin.style.borderBottomLeftRadius = 0;

      this.multiconnect = (direction == BPDirection.INPUT);
    }
    else {
      this.multiconnect = (direction == BPDirection.OUTPUT);
    }

    if (direction == BPDirection.INPUT) {
      //this.element.style.paddingRight = "15px";

      this.element.appendChild(this.pin);

      //if (this.array) {
/*
      if (this.dimensions > Dimensions.SCALAR) {
        var iconName = this.dimensions == Dimensions.ARRAY ? 'ellipsis-h' : 'i-th';
        var elemArray = document.createElement('div');
        elemArray.className = "label";
        elemArray.innerHTML = "<i class=\"icon "+iconName+"\" style=\"color:gray\"></i>&nbsp;";
        this.element.appendChild(elemArray);
      }
*/
      this.elemArray = document.createElement('div');
      this.elemArray.className = "label";
      this.element.appendChild(this.elemArray);
      this.setDimensions(this.dimensions);

      this.element.appendChild(this.label);

      //if (!this.array) {
      if (this.dimensions == Dimensions.SCALAR) {
        this.element.appendChild(this.createValueElement ());
      }
    }
    else {
      this.element.style.paddingLeft = "15px";
	    this.element.style.textAlign = "right";
      this.element.appendChild(this.label);

      //var iconName = this.dimensions == Dimensions.ARRAY ? 'ellipsis-h' : 'i-th';
      this.elemArray = document.createElement('div');
      this.elemArray.className = "label";
      //this.elemArray.innerHTML = "&nbsp;<i class=\"icon "+iconName+"\" style=\"color:darkgray\"></i>";
      this.element.appendChild(this.elemArray);
      this.setDimensions(this.dimensions);

      if (this.dimensions == Dimensions.SCALAR) {
        if (withValue)
          this.element.appendChild(this.createValueElement ());
      }

      this.element.appendChild(this.pin);
	    //this.rightColumn.appendChild(this.element);
    }

    var connectorSelf = this;

    this.pin.onmousedown = function (e) {
	    e.preventDefault();
	    e.stopPropagation();

      switch (blueprint.getStatus()) {
        case BPStatus.READY:
	        if (e.button == 0) {
            //console.log ("Connector clicked "+connectorSelf.id+" ("+connectorSelf.labelStr+")");

            /*if (!connectorSelf.multiconnect)
              connectorSelf.disconnectAll ();*/

            blueprint.setStatus (BPStatus.CONNECTING);

            //var start = getAttachPoint(this);
            var start = blueprint.coordsAbsToRel(getAttachPoint(connectorSelf.pin));
            var end = { x:start.x, y:start.y };
            //console.log ('start = ('+start.x+', '+start.y+') ('+start.x * blueprint.zoom+', '+start.y * blueprint.zoom+')');

            var edge = blueprint.createEdge (start, end);
            edge.setColor(connectorSelf.getColor());
            edge.setConnector1 (connectorSelf);
            connectorSelf.setFilled (true);

            blueprint.setConnectingEdge (edge);
            //console.log ("Connector1: "+this.id+ " of node "+this.parentNode.parentNode.id);
          }
          break;

        default:
          break;
      }
    }

    this.pin.onmouseover = function(e) {
      e.preventDefault();

      switch (blueprint.getStatus()) {
        case BPStatus.READY:
          this.style.cursor = "pointer";
          break;

        case BPStatus.CONNECTING:
          var edge = blueprint.getConnectingEdge();

          if (edge) {
            //console.log ("PIN "+getAttachPoint(this));
            //console.log ("Check compatibility with "+edge.connector1.getLabel());

            //if (blueprint.compatible (connectorSelf.id, edge.connectorID1)) {
            if (connectorSelf.canConnectTo (edge.connector1)) {
              this.style.cursor = "crosshair";
              //edge.setPoint2 (getAttachPoint(this));
              edge.setTrackingPoint (blueprint.coordsAbsToRel(getAttachPoint(this)));
              edge.setLocked (true);
              edge.setConnector2(connectorSelf);
              connectorSelf.setFilled (true);
            }
            else {
              this.style.cursor = "not-allowed";
            }
          }

          break;

        default:
          break;
      }
    }

    this.pin.onmouseout = function(e) {
      e.preventDefault();

      switch (blueprint.getStatus()) {
        case BPStatus.CONNECTING:
          this.style.cursor = "pointer";
          var edge = blueprint.getConnectingEdge();

          if (edge) {
            //console.log ("PIN "+getAttachPoint(this));
            edge.setLocked (false);

            if (!connectorSelf.isConnected () && connectorSelf != edge.connector1)
              connectorSelf.setFilled (false);
          }

        default:
          break;
      }
    }

    this.pin.onmouseup = function(e) {
      e.preventDefault();

      switch (blueprint.getStatus()) {
        case BPStatus.CONNECTING:
          if (e.button == 0) {
            var edge = blueprint.getConnectingEdge();

            if (edge) {
              // Connect
              if (connectorSelf.canConnectTo (edge.connector1)) {
                edge.connector1.connectTo (connectorSelf);

                // Signal connection
                owner.signalConnected(connectorSelf);

                // Add edge
                edge.connector1.getOwner().signalConnected(edge.connector1);
              }
            }
          }
          break;

        default:
          break;
      }
    }

    this.pin.oncontextmenu = function(e) {
      e.preventDefault();
      e.stopPropagation();
      var json

      switch (blueprint.getStatus()) {
        case BPStatus.READY:
          //connectorSelf.disconnectAll();
          json = ' {  "items": [ ';
          json += '{ "id": 0, "item": "Unlink", "data":"`+connectorSelf.id+`" }';
          json += connectorSelf.canDelete ? ', { "id": 1, "item": "Remove", "data":"'+connectorSelf.id+'" }' : '';
          json += '] }';

          blueprint.contextMenu.remove();
          blueprint.contextMenu.enableSearch (false);
          blueprint.contextMenu.createFromJson(JSON.parse(json));
          blueprint.contextMenu.callback = function (itemId, data) {
            //console.log ("Clicked "+itemId);
            switch (itemId) {
              case 0:
                connectorSelf.disconnectAll();
                break;
              case 1:
                connectorSelf.owner.deleteConnector (connectorSelf);
                break;
              default:
                break;
            }
          }
          blueprint.contextMenu.show(e.x, e.y);
          break;
        default:
          break;
      }
    }

    //console.log ('Created connector for '+this.name+' '+this.dimensions);

    /*if (debug) {
      //console.log ("Created pin "+name+" for "+this.owner.getTitle()+ " (multiconnect: "+(this.multiconnect ? "Yes" : "No" )+")");
    }*/
  }

  remove () {
    this.disconnectAll();
    this.getElement().parentElement.removeChild(this.getElement());
  }

  refresh () {
    //console.log ("[refresh]");
    if (this.references)
      this.label.innerHTML = this.references.name;
  }

  toJSON () {
    var jo = {};

    jo.id = this.id;
    /*
    jo.dataType = this.dataType.id;
    jo.dataTypeName = this.dataType.name;
    jo.pinType = this.pinType.id;
    jo.pinTypeName = this.pinType.name;
    */
    jo.dataType = this.dataType.name;
    jo.pinType = this.pinType.name;

    jo.label = this.getLabel();
    jo.exec = this.exec;
    jo.dimensions = this.dimensions;

    if (this.any)
      jo.any = this.any;

    if (this.canDelete)
      jo.canDelete = this.canDelete;

    if (this.single_line)
      jo.single_line = this.single_line;

    if (this.password)
      jo.password = this.password;

    if (this.java)
      jo.java = this.java;

    if (this.enumLabels) {
      var a = [];

      for (var i=0; i<this.enumLabels.length; i++) {
        a.push(this.enumLabels[i]);
      }

      jo.enum = a;
    }

    if (this.hasValue && this.valueElem) {
      jo.value = this.getValue();
    }

    if (this.references)
      jo.references = this.references.id;

    if (this.nullable)
      jo.nullable = true;

    if (this.notNull)
      jo.not_null = true;

    return (jo);
  }

  toString () {
    return (JSON.stringify(this.toJSON()));
  }

}
