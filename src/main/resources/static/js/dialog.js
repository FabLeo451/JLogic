'use strict';

const DialogResult = {
  OK: 0,
  CANCEL: 1
}

const DialogButtons = {
  NONE: 0,
  OK: 1,
  OK_CANCEL: 2,
  YES_NO: 3,
  STOP: 4
}

const DialogWidthMode = {
  DEFAULT: 0,
  MIN: 1
}

class Dialog {
  constructor() {
    this.widthMode = DialogWidthMode.DEFAULT;
    
    this.bgElem = null;
    this.windowElem = null;
    this.contentElem = null;
    
    this.buttonOK = null;
    this.buttonCancel = null;
    
    this.callbackOK = null;
    this.callbackCancel = function (dialog) { dialog.destroy(); };
    this.result = -1;
    
    this.buttons = DialogButtons.OK_CANCEL
  }
  
  setCallbackOK (f) { this.callbackOK = f; }
  setCallbackCancel (f) { this.callbackCancel = f; }
  
  getContentElement () { return (this.contentElem); }
  setBackground (color) { this.windowElem.style.backgroundColor = color; }
  
  setButtons (b) { this.buttons = b; }
  
  create (title) {
    var textOK = null, textCancel = null;

    /* Background object */
    this.bgElem = document.createElement('div');
    this.bgElem.classList.add('modal');
    
    /* Dialog window */
    this.windowElem = document.createElement('div');
    this.windowElem.classList.add('dialog-content');
    this.windowElem.classList.add('center');
    this.setWidth();
    
    this.bgElem.style.display = 'block';
    this.bgElem.appendChild(this.windowElem);
    
    /* Title */
    var t = document.createElement('div');
    t.innerHTML = '<center><b>'+title+'</b><center><hr style="margin:5px">';
    this.windowElem.appendChild(t);
    
    /* Empty content */
    this.contentElem = document.createElement('div');
    this.contentElem.style.marginBottom = '4em';
    this.windowElem.appendChild(this.contentElem);
    
    /* Buttons */
    switch (this.buttons) {
      case DialogButtons.OK:
        textOK = 'OK';
        break;
        
      case DialogButtons.OK_CANCEL:
        textOK = 'OK';
        textCancel = 'Cancel';
        break;
        
      case DialogButtons.YES_NO:
        textOK = 'Yes';
        textCancel = 'No';
        break;
        
      case DialogButtons.STOP:
        textOK = 'Stop';
        break;
        
      default:
        break;        
    }
    
    var buttonsContainer = document.createElement('div');
    buttonsContainer.style.position = 'absolute';
    buttonsContainer.style.bottom = '10px';
      
    var hrElem = document.createElement('hr');
    hrElem.style.margin = "5px";
    buttonsContainer.appendChild(hrElem);
    
    if (textOK) {
      this.buttonOK = document.createElement('button');
      this.buttonOK.classList.add('btnApp');
      this.buttonOK.style.minWidth = '6em';

      this.buttonOK.innerHTML = textOK;
      buttonsContainer.appendChild(this.buttonOK);
      
      var selfDialog = this;
      
      this.buttonOK.onclick = function (ev) {
        this.result = DialogResult.OK;
        if (selfDialog.callbackOK)
          selfDialog.callbackOK (selfDialog);
      }
    }
    
    if (textCancel) {
      this.buttonCancel = document.createElement('button');
      this.buttonCancel.classList.add('btnApp');
      this.buttonCancel.style.minWidth = '6em';
      this.buttonCancel.innerHTML = textCancel;
      buttonsContainer.appendChild(this.buttonCancel);
      
      this.buttonCancel.onclick = function (ev) {
        this.result = DialogResult.CANCEL;
        if (selfDialog.callbackCancel)
          selfDialog.callbackCancel (selfDialog);
      }
    }
    
    this.windowElem.appendChild(buttonsContainer);
    
    document.body.appendChild(this.bgElem);
 

    /*this.appMainElem.style.filter = "blur(1px)";
    this.appMainElem.style.WebkitFilter = "blur(1px)";*/

    return (this);
  }
  
  destroy () {
    document.body.removeChild(this.bgElem);
    /*this.appMainElem.style.filter = "none";
    this.appMainElem.style.WebkitFilter = "none";*/
  }
  
  setWidthMode(m) {
    this.widthMode = m;
    console.log('Now this.widthMode = '||this.widthMode);
  }
  
  setWidth() {
    console.log('this.widthMode = '||this.widthMode);
    switch (this.widthMode) {
      case DialogWidthMode.MIN:
        if (this.windowElem)
          this.windowElem.style.minWidth = '40%';
        break;
        
      default:
        if (this.windowElem)
          this.windowElem.style.minWidth = '';
        break;
    }
  }
}

const DialogIcon = {
  NONE:      0,
  INFO:      1,
  QUESTION:  2,
  WARNING:   3,
  ERROR:     4,
  RUNNING:   5
}

function dialogMessage (title, message, buttons, icon, callbackOK) {
  var dialog = new Dialog ();
  
  dialog.setButtons (buttons);
  dialog.callbackOK = callbackOK;
  dialog.callbackCancel = function (dialog) { dialog.destroy(); };
  //dialog.setWidthMode(DialogWidthMode.FIT);
  dialog.create(title);
  
  var content = dialog.getContentElement ();
  
  var iconClass;
  
  switch (icon) {
    case DialogIcon.INFO:
      iconClass = 'i-info w3-text-blue';
      break;
      
    case DialogIcon.QUESTION:
      iconClass = 'i-question w3-text-green';
      break;
      
    case DialogIcon.WARNING:
      iconClass = 'i-exclamation-triangle w3-text-orange';
      break;
      
    case DialogIcon.ERROR:
      iconClass = 'i-exclamation w3-text-red';
      break;
      
    case DialogIcon.RUNNING:
      iconClass = 'i-spinner w3-spin';
      break;
      
    default:
      iconClass = 'i-info w3-text-blue';
      break;      
  }
  
  content.innerHTML = `
      <div id="_glg_icon" class="w3-container w3-cell w3-cell-top"><i class="icon `+iconClass+` w3-xxlarge"></i></div>
      <div id="_dlg_msg" class="w3-container w3-cell w3-cell-middle">`+message+`</div>
  `;
  
  return (dialog);
}

function dialogInfo (message) {
  dialogMessage ("Info", message, DialogButtons.OK, DialogIcon.INFO, function (dialog) {
      dialog.destroy();
    }
  );
}

function dialogError (message) {
  dialogMessage ("Error", message, DialogButtons.OK, DialogIcon.ERROR, function (dialog) {
      dialog.destroy();
    }
  );
}

const DataType = {
  INTEGER: 1,
  NUMBER:  2,
  STRING:  3,
  BOOLEAN: 4,
  JSON:    5
}

const DataFlags = {
  SHOW_KEY:  1, // Not used
  EDIT_KEY:  2,
  TIMESTAMP: 4, // Integrer timestamp (seconds since...)
  
  DEFAULT:   3,
  EDIT_ONLY_VALUE:   1
}

class DialogData extends Dialog {
  constructor() {
    super();
    
    this.type = 'Integer';
    this.key = 'i';
    this.value = 0;
    this.flags = DataFlags.DEFAULT;
    this.is_timestamp = false;
  }
  
  isTimestamp () { return(this.is_timestamp); }
  
  create (type, key, value, flags) {
    super.create("Edit data");

    var valueHTML, valueLabel;
    
    console.log ('type = '+type);
    
    this.type = type;
    this.key = key;
    this.value = value;
    this.flags = flags;
    this.is_timestamp = flags & DataFlags.TIMESTAMP;
    
    /* Prepare value HTML */
    
    switch (type) {
      case 'Integer':
        if (this.is_timestamp) {
          valueLabel = 'Date and time';
          
          console.log ('value = '+value);
          
          var t = (value == null || value == 0) ? new Date() : new Date(value * 1000);
          var day = secondsToDay (value);
          var time = secondsToTime (value);
 
          console.log (day+' '+time);
          
          valueHTML = `
            <input id="date" class="w3-input w3-border" type="date" name="bdaytime" value="`+day+`">
            <input id="time" class="w3-input w3-border" type="time" name="bdaytime" value="`+time+`">
          `;
        }
        else {
          valueLabel = 'Numeric value';
          valueHTML = `
            <input id="dataValue" class="w3-input w3-border" value="`+(value != null ? value : 0)+`" style="width: 10em;">
          `;
        }
        break;

      case 'Double':
        valueLabel = 'Double value';
        valueHTML = `
          <input id="dataValue" class="w3-input w3-border" value="`+(value != null ? value : 0)+`" style="width: 10em;">
        `;
        break;
        
      case 'Boolean':
        var checked = value ? "checked" : "";
        valueLabel = 'Boolean value';
        valueHTML = `
          <input id="dataValue" class="w3-check" type="checkbox" `+checked+`>
        `;
        break;
        
      case 'String':
        valueLabel = 'String value';
        valueHTML = `
          <textarea class="w3-border data" id="dataValue" rows="10" cols="30" style="font-size:12px;">`+(value != null ? value : "")+`</textarea>
        `;
        break;
/*
      case DataType.JSON:
        valueLabel = 'Json';
        valueHTML = `
          <textarea class="w3-border data" id="dataValue" rows="10" cols="30" style="font-size:12px;">`+(value != null ? JSON.stringify(value, null, 2) : "{ }")+`</textarea>
        `;
        break;
*/       
      default:
        break;
    }
    
    var key_read_only = "", grayed_key = "";
    
    //console.log ("flags = "+flags+" flags & DataFlags.EDIT_KEY = "+(flags & DataFlags.EDIT_KEY));
    
    if ((flags & DataFlags.EDIT_KEY) == 0) {
      key_read_only = "readonly";
      grayed_key = 'background-color:#CCCCCC;';
    }

    //console.log ("key_read_only = "+key_read_only);
    
    this.contentElem.innerHTML = `
      <div class="w3-container w3-padding-small">
        <label>Key</label>
        <input id="dataKey" class="w3-input w3-border  type="text" value="`+key+`" style="width: 40em;`+grayed_key+`" `+key_read_only+`>
        <input id="dataType" type="hidden" value="`+type+`">
        <br>
        <label>`+valueLabel+`</label>
        `+valueHTML+`
        <div id="dataResponse" class="w3-container w3-padding-small">&nbsp;</div>
      </div>
    `;
    
    /* Set filters */
    
    var dataKey = document.getElementById("dataKey");
    var valueElem = document.getElementById("dataValue");
    
    setInputFilter(dataKey, function(value) { return /^[a-zA-Z0-9_.\- ]*$/.test(value); });

    switch (type) {
      case 'Integer':
        if (!this.is_timestamp)
           setInputFilter(valueElem, function(value) { return /^-?\d*$/.test(value); });
        break;
      case 'Double':
        setInputFilter(valueElem, function(value) { return /^-?\d*[.,]?\d*$/.test(value); });
        break;
        
      default:
        break;
    }
  }
  
  getData () {
    switch (this.type) {
      case 'Integer':
        if (this.is_timestamp) {
          var d = document.getElementById("date").value;
          var t = document.getElementById("time").value;
          console.log (d +"T"+ t);
          var s = new Date (d +"T"+ t).getTime() / 1000;
          return s;
        }
        else
          return (Number(document.getElementById("dataValue").value));
        break;
        
      case 'Double':
        return (Number(document.getElementById("dataValue").value));
        break;
        
      case 'String':
        return(document.getElementById("dataValue").value);
        break;
        
      case 'Boolean':
        return(Boolean(document.getElementById("dataValue").checked));
        break;
/*        
      case DataType.JSON:
        return(JSON.parse(document.getElementById("dataValue").value));
*/      
      default:
        return (null);
        break;
    }
  }
  
  checkData () {
    var j;
    
    switch (this.type) {
      case DataType.JSON:
        try {
          j = JSON.parse(document.getElementById("dataValue").value);
          return (true);
        }
        catch (err) {
          return (false);
        }
      
      default:
        return (true);
        break;
    }
  }
  
  showError (text) {
    var dataResponseDiv = document.getElementById("dataResponse");
    dataResponseDiv.innerHTML = '<span class="w3-text-red"><i class="icon i-exclamation-triangle"></i></span> '+text;
  }
}

/*
<ul id="myUL">
  <li><span class="caret">Beverages</span>
    <ul class="nested">
      <li>Water</li>
      <li>Coffee</li>
      <li><span class="caret">Tea</span>
        <ul class="nested">
          <li>Black Tea</li>
          <li>White Tea</li>
        </ul>
      </li>  
    </ul>
  </li>
</ul>
*/
/*
function onBlueprintSelected (ev) {
  var id = ev.target.getAttribute ('bp_id');
  console.log (id);
  
  var prev = document.querySelector('.tree-item-selected');
  
  if (prev)
    prev.classList.toggle ("tree-item-selected");
  
  var li = document.getElementById("li_"+id)
  li.classList.add("tree-item-selected");
}

function addToTree (jcatalog, parentElem, root) {
  var key, pkgElem, ul, li;
  
  for (key in root) {
    li = document.createElement('li');
    li.classList.add('tree-item');

    parentElem.appendChild(li);

    //if (typeof root[key] === 'object') {
    if (!root[key].hasOwnProperty("type")) {
      // Package
      pkgElem = document.createElement('span');
      pkgElem.classList.add('caret');
      pkgElem.innerHTML = '<i class="icon i-box-open w3-text-orange"></i> '+jcatalog[key].name; 
      li.appendChild(pkgElem); 
         
      ul = document.createElement('ul');
      ul.classList.add('nested');
      li.appendChild(ul);    
      
      addToTree (jcatalog, ul, root[key]);
    }
    else {
      // Blueprint
      
      var margin = parentElem.classList.contains('tree') ? 'style="margin-left:1em;"' : '';
      
      li.innerHTML = '<i bp_id="'+key+'" class="icon i-project-diagram w3-text-blue-gray" '+margin+'></i> '+jcatalog[key].name;
      li.style.cursor = 'pointer';
      li.setAttribute('id', 'li_'+key);
      li.setAttribute('bp_id', key);
      li.onclick = onBlueprintSelected;
    }
  }
}
*/
class DialogChooseBlueprint extends Dialog {
  constructor() {
    super();
    
    this.setButtons (DialogButtons.OK_CANCEL);
  }

  create (jindex) {
    super.create("Choose blueprint");
    
    //this.windowElem.style.height = "60%";
    
    /* Create tree */
    /*
    var treeElem = document.createElement('ul');
    treeElem.classList.add('tree');
    treeElem.style.height = "100%";
    
    var jtree = jcatalog["_tree"];
    
    if (jtree) {
      addToTree (jcatalog, treeElem, jtree);
    }*/
    
    var selectElem = document.createElement('select');
    selectElem.classList.add('w3-select');
    selectElem.setAttribute('id', 'select-bp');
 
    for (var key in jindex) {
      var optionElem = document.createElement('option');
      optionElem.innerHTML = jindex[key].name;
      optionElem.value = jindex[key].id;
      selectElem.appendChild(optionElem);
    }
/* 
    for (var i in jindex) {
      var optionElem = document.createElement('option');
      optionElem.innerHTML = jindex[i].name;
      optionElem.value = i;
      selectElem.appendChild(optionElem);
    }*/
    
    this.contentElem.innerHTML = `
      <div id="container" class="w3-container w3-padding w3-transparent" style="overflow:auto; background:white; border: 1px solid lightgrey; height:100%">
        <div class="w3-row">
          <div class="w3-col s6"><div class="w3-container">Select blueprint</div></div><div id="combo" class="w3-col s6"></div>
        </div>
      </div>
    `;
    
    //this.contentElem.style.height = "70%";
    document.getElementById("combo").appendChild(selectElem);
    /*
    var toggler = document.getElementsByClassName("caret");
    var i;

    for (i = 0; i < toggler.length; i++) {
      toggler[i].addEventListener("click", function() {
        this.parentElement.querySelector(".nested").classList.toggle("expanded");
        this.classList.toggle("caret-down");
      });
    }*/
  
  }
  
  getSelected () {
    //var selected = document.querySelector('.tree-item-selected');
    var selected = document.getElementById("select-bp").value;
    
    return (selected);
  }
}

