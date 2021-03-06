'use strict';

const MenuItems = {
 BP:             9,
 BP_HOME:      901,
 BP_HELP:      902,
 BP_DEBUG:     903,

 FILE:             1,
 FILE_OPEN:      101,
 FILE_SAVE_AS:   102,

 EDIT:              2,
 EDIT_UNDO:       200,
 EDIT_REDO:       201,
 EDIT_COPY:       203,
 EDIT_PASTE:      204,
 EDIT_SELECT_ALL: 205,
 EDIT_DELETE:     210,

 VIEW:                 3,
 VIEW_SHOW_GRID:     300,
 VIEW_SNAP_TO_GRID:  301,
 VIEW_CONSOLE:       302,
 VIEW_ZOOM_IN:       310,
 VIEW_ZOOM_OUT:      311,
 VIEW_ZOOM_1_1:      312,

 BLUEPRINT:                4,
 BLUEPRINT_COMPILE:       400,
 //BLUEPRINT_VALIDATE:     401,
 BLUEPRINT_RUN:          402,
 BLUEPRINT_REVERT:       403,
 //BLUEPRINT_COMMAND_LINE: 404
};

const AppParameterType = {
  INPUT:  1,
  OUTPUT: 2,
  VARIABLE: 3
}

var bpConsole, g_filename = null;
var dialogWorking;
//var programIndex = null;
var program = null;
var lastInput = null;

function appKeyPressed(e) {
  if (!actionsEnabled)
    return;

  //e.preventDefault();
  //e.stopPropagation();

  var key = e.which || e.keyCode;

  //console.log ("[appKeyPressed] key = "+key);

  var a = menubar.matchAction (key, e.ctrlKey || e.metaKey, e.altKey, e.shiftKey);

  if (a) {
  e.preventDefault();
  e.stopPropagation();

    if (a.keypressed)
      a.callback(a);
  }
}

function appKeyReleased(e) {
  if (!actionsEnabled)
    return;

  //e.preventDefault();
  //e.stopPropagation();

  var key = e.which || e.keyCode;

  //console.log ("[appKeyReleased] key = "+key);

  var a = menubar.matchAction (key, e.ctrlKey || e.metaKey, e.altKey, e.shiftKey);

  if (a) {
    //console.log ("[appKeyReleased] "+a.toString()+" -> "+a.id);
  e.preventDefault();
  e.stopPropagation();
    if (a.keyreleased)
      a.callback(a);
  }
}

/**
 * Refresh application variables from blueprint variables
 */
function appRefreshVariables() {
  appClearVariables ();
  var vars = blueprint.getVariables();
  
  for (var i=0; i<vars.length; i++) {
    //console.log("Adding variable to app: "+vars[i].getName());
    appAddVariable(vars[i]);
  }
}

function appLoadBlueprint (j) {
  var connectors;

  console.log ("Start");

  appClearAll ();
  blueprint.clear();

  /* Build blueprint */
  blueprint.fromJson(j);
  
  console.log ("Adding variables...");
  appRefreshVariables();

  //console.log ("Adding input");

  /* Input */
  if (blueprint.entryPointNode) {
    connectors = blueprint.entryPointNode.connectors;

    for (var i=0; i<connectors.length; i++) {
      if (connectors[i].pinType.name != 'Exec')
        appAddInOut (AppParameterType.INPUT, connectors[i]);
    }
  }

  //console.log ("[application] [appLoadBlueprint] Adding output");

  /* Output */
  if (blueprint.returnNode) {
    connectors = blueprint.returnNode.connectors;

    for (var i=0; i<connectors.length; i++) {
      if (connectors[i].pinType.name != 'Exec')
        appAddInOut (AppParameterType.OUTPUT, connectors[i]);
    }
  }

  blueprint.refreshNodes();

  var bpName = document.getElementById('bpName');
  bpName.value = blueprint.getName();
  document.title = blueprint.getName();

  blueprint.clearSelection();

  console.log ("[application] [appLoadBlueprint] End");
}

function readFile(e) {
  var file = e.target.files[0];
  //console.log("[readFile] file = "+e.target.value);
  if (!file) {
    return;
  }
  var reader = new FileReader();

  reader.onload = function(e) {
    var contents = e.target.result;

    try {
      var j = JSON.parse(contents);

      if (j.type == blueprint.type) {
        appLoadBlueprint (j);
        setStatus (BPEditStatus.MODIFIED);
      } else {
        dialogError ('Blueprint types are different.<br>This blueprint: '+blueprint.type+'<br>File blueprint: '+j.type);
      }
    } catch (err) {
      dialogError ('Not a valid JSON file.');
    }

    /* Reset so 'changed' event triggers again */
    document.getElementById('file-dialog').value="";
  };

  reader.readAsText(file);
}

function saveFileAs () {
  var text = blueprint.toString(),
      blob = new Blob([text], { type: 'text/plain' }),
      anchor = document.createElement('a');

  anchor.download = blueprint.getName() + ".json";
  anchor.href = (window.webkitURL || window.URL).createObjectURL(blob);
  anchor.dataset.downloadurl = ['text/plain', anchor.download, anchor.href].join(':');
  anchor.click();
}

function cbBeginModify () {
  console.log ("[cbBeginModify]");
  //undo.setBackup (blueprint.toJson());
}

function cbModified() {
  //console.log ("[application] [cbModified]");

  if (!blueprint)
    return;

  setStatus (BPEditStatus.MODIFIED);

  try {
    undo.setCurrent(blueprint.toJson());
  }
  catch (err) {
    //console.log (blueprint.toString());
  }

  //undo.dump();
}

function cbError(message) {
  showError(message);
}

function onNameChanged() {
  //document.getElementById('bp-title').innerHTML = document.getElementById('bpName').value;
  blueprint.onModified ();
}

function findAndDeleteNextWrongGlobal() {
  // Find global variables present in the blueprint but not in the program
  var index, found;

  for (var b=0; b<_jbp.variables.length; b++) {
    if (_jbp.variables[b].global) {

      // Check if it's in the program
      found = false;
      for (var p=0; p<program.variables.length; p++) {
        if (_jbp.variables[b].id == program.variables[p].id) {
          found = true;
          break;
        }
      }

      if (!found) {
        var v = _jbp.variables[b];

        if (!v.referenced) {
          bpConsole.append ("Blueprint global variable '"+v.name+"' not in program and will be removed.", BPConsoleTextType.WARNING);
          _jbp.variables.splice(b, 1);
          return true;
        }
        else {
          bpConsole.append ("Variable '"+v.name+"' should be removed but is used in blueprint", BPConsoleTextType.WARNING);
        }
      }
    }
  }
  
  return false;
}

function checkGlobals() {
  var updated = false;
  
  while(true) {
    var deleted = findAndDeleteNextWrongGlobal();
    
    if (deleted)
      updated = true;
    else
      break;
  }

  // Add program variables not present in the blueprint yet and update exsising
  for (var i=0; i<program.variables.length; i++) {
		var pv = program.variables[i];

    //console.log("Checking program variable:");
    //console.log(pv);

	  var found = false;
    for (var k=0; k<_jbp.variables.length; k++) {
      if (!_jbp.variables[k].global)
        continue;
      
      if (_jbp.variables[k].id == pv.id) {
        found = true;
        // Name could be changed by others blueprints
        //_jbp.variables[k].name = v.name;

        // Update blueprint variable
        var ref = _jbp.variables[k].referenced;
        _jbp.variables[k] = pv;
        _jbp.variables[k].referenced = ref;
        break;
      }
    }

		if (!found) {
      bpConsole.append('Found new global variable <b>'+pv.name+'</b>', BPConsoleTextType.WARNING);
      _jbp.variables.push(pv);
      updated = true;
    }
  }

  return updated;
}

function appStart () {

  console.log ('Initializing console...');

  bpConsole = new BPConsole ();
  bpConsole.linkToDiv ('myConsole');
  //bpConsole.textElem.onmouseenter = function () {
  bpConsole.textElem.onmousedown = function () {
    console.log ("Focus on console");
    beginEdit ();
  }
  
  // Enable variable drop on globals container
  document.getElementById("globals_main").ondragover = function(e) {
    e.preventDefault();
  }
  document.getElementById("globals_main").ondrop = function(e) {
    e.preventDefault();
		var data = e.dataTransfer.getData("data");
    console.log (data);
      try {
        var jdata = JSON.parse (data);

        if (jdata.tag == 'VARIABLE') {
          var v = blueprint.getVariable (jdata.id);

          if (v) {
            if (!blueprint.getVariableByNameAndScope(v.getName(), Scope.GLOBAL)) {
              undo.begin();
              v.setGlobal(true);
              appRefreshVariables();
              undo.end();
              cbModified();
            } else
              console.error("Global variable "+v.getName()+" exists");
          }
          else
            console.error("Variable "+jdata.id+" not found");
        }
      } catch (err) {
        console.error(err.message);
      }
  }
  
  // Enable variable drop on locals container
  document.getElementById("locals_main").ondragover = function(e) {
    e.preventDefault();
  }
  document.getElementById("locals_main").ondrop = function(e) {
    e.preventDefault();
		var data = e.dataTransfer.getData("data");
    console.log (data);
      try {
        var jdata = JSON.parse (data);

        if (jdata.tag == 'VARIABLE') {
          var v = blueprint.getVariable (jdata.id);

          if (v) {
            console.log(v.getName()+" is "+(v.isGlobal() ? "global" : "local"));
            if (!blueprint.getVariableByNameAndScope(v.getName(), Scope.LOCAL)) {
              undo.begin();
              v.setGlobal(false);
              appRefreshVariables();
              undo.end();
              cbModified();
            } else
              console.error("Local variable "+v.getName()+" exists");
          }
          else
            console.error("Variable "+jdata.id+" not found");
        }
      } catch (err) {
        console.error(err.message);
      }
  }

  bpConsole.clear();
  bpConsole.append('------------------------------------------------');
  bpConsole.append(__SERVER.package.application_name);
  bpConsole.append('Version '+__SERVER.package.version);
  bpConsole.append('------------------------------------------------');

  undo = new UndoRedo ();
  undo.setMaxDepth(50);
  undo.begin();

  console.log ('Initializing blueprint...');

  //bpStart (null);
  blueprint = new Blueprint ();
  blueprint.init ();
  blueprint.setAsset (_asset);
  setStatus (BPEditStatus.SUBMITTED);

  // Get blueprint id
  var args = window.location.pathname.split("/");
  blueprint_id = args[2];

  var dialog = dialogMessage (__SERVER.package.application_name, 'Getting blueprint...', DialogButtons.NONE, DialogIcon.RUNNING, null);

  callServer ("GET", "/blueprint/"+blueprint_id, null, function (xhttp) {
    if (xhttp.readyState == 4) {

      if (xhttp.status == 200) {
        console.log('Blueprint loaded.');

        _jbp = JSON.parse(xhttp.responseText);

        // Get program

        console.log('Getting program ...');

        callServer ("GET", "/program/"+ _jbp.programId, null, function (xhttp) {
            if (xhttp.readyState == 4) {
              if (xhttp.status == 200) {
                console.log ('Program OK');

                program = JSON.parse(xhttp.responseText);

                // Compare blueprint global variables with program global variables
                var updated = checkGlobals();

                appLoadBlueprint (_jbp);
                setStatus (updated ? BPEditStatus.MODIFIED : BPEditStatus.SUBMITTED);

                blueprint.setCallbackBeginModify(cbBeginModify);
                blueprint.setCallbackModified(cbModified);
                blueprint.setCallbackError(cbError);
								blueprint.setCallbackLog(function(m) {
										bpConsole.append(m);
									}
								);
                //setStatus (BPEditStatus.SUBMITTED);

                document.getElementById('programName').innerHTML = program.name;
                document.getElementById('bp-title').innerHTML = _jbp.name;

                // Check if it's a MAIN or EVEMTS blueprint

                if (_jbp.type == BlueprintType.MAIN || _jbp.type == BlueprintType.EVENTS) {
                  document.getElementById("bpName").readOnly = true;
                  document.getElementById('input-panel').style.display = 'none';
                  document.getElementById('output-panel').style.display = 'none';
                }

                // Set program status
                setProgramStatus(program.status);

                undo.end();
                undo.setCurrent(blueprint.toJson());
                undo.setCurrentAsSaved();
                undo.dump();

                dialog.destroy();

                showSnacknar(BPResult.SUCCESS, 'Blueprint '+_jbp["name"]+' loaded', 2000);
                bpConsole.append("Ready.");
              }
              else
                console.log(xhttp.responseText);
            }
          }
        );
      }
      else {
        dialog.destroy();
        dialogError (xhttp.responseText);
        //console.log(xhttp.responseText);
      }


    }
    else {
    }
  }
  );

  if (document.addEventListener) { // IE >= 9; other browsers
     document.addEventListener('keydown', appKeyPressed, false);
     document.addEventListener('keyup', appKeyReleased, false);
  } else { // IE < 9
      document.attachEvent('oncontextmenu', function() {
          alert("You're using IE < 9");
          window.event.returnValue = false;
      });
  }

  window.onclick = function(e) {
    //console.log ("window.onclick");
    if (!e.target.matches('.dropbtn') && !e.target.classList.contains('menu-element'))
      menubar.activate(false);
  }

  document.getElementById('file-dialog').addEventListener('change', readFile, false);
}

var dialogRunning;

function runCallback (xhttp) {
  switch (xhttp.readyState) {
    case 1:
      bpConsole.append ("Launching program...");
      dialogRunning = dialogMessage (__SERVER.package.application_name, "Executing blueprint...", DialogButtons.STOP, DialogIcon.RUNNING, function (dialog) {
          //dialog.destroy();
          document.getElementById("_dlg_msg").innerHTML = "Stopping...";

          callServer ("POST", "/stop/clientId/"+clientId, '', function (xhttp) {
            if (xhttp.readyState == 4) {
              if (xhttp.status == 200) {
                bpConsole.append ("Blueprint stopped by user.", BPConsoleTextType.WARNING);
              }
              else
                document.getElementById("_dlg_msg").innerHTML = "Unable to stop blueprint.";
            }
            else {
              //apiResponse.innerHTML = 'Saving...';
            }
          });
        }
      );
      break;

    case 2:
      //bpConsole.append ("Connected");
      break;

    case 3:
      bpConsole.append ("Getting response...");
      break;

    case 4:
      dialogRunning.destroy();

      var response = xhttp.responseText;

      if (!bpConsole.getVisible())
        toggleConsoleVisibility();

      if (xhttp.status == 200) {
        bpConsole.append ('<i class="icon i-check w3-text-green"></i> Successfully executed', BPConsoleTextType.SUCCESS);
        bpConsole.append (response);
      }
      else if (xhttp.status == 0)
        bpConsole.append ("Can't connect server", BPConsoleTextType.ERROR);
      else {
        //console.log ("xhttp.status = "+xhttp.status);
        var jo;

        try {
          jo = JSON.parse(response);
          bpConsole.append (jo.message, BPConsoleTextType.ERROR);
        }
        catch (err) {
          console.error(err.message);
          bpConsole.append (response);
        }

        bpConsole.append ('<i class="icon i-exclamation w3-text-red"></i> Execution failed', BPConsoleTextType.ERROR);
      }

      break;

    default:
      break;
  }
}

function run (dialog) {
  var input = document.getElementById('inputTextArea').value;
  //var trace = document.getElementById('_trace').checked ? '&trace=1' : '';
  dialog.destroy();

  //console.log('input = '+input);
  //callServer ("POST", "/program/"+_jbp.programId+"/run/"+blueprint.getName()+"?client_id="+__SERVER['client_id']+trace, input, runCallback);
  
  lastInput = input;

  callServer ("POST", "/program/"+_jbp.programId+"/run/"+blueprint.getName()+"?clientId="+clientId, input, runCallback);
  endEdit();
}

const ProgramStatus = {
  READY:    'READY',
  COMPILED: 'COMPILED',
  ERRORS:   'ERRORS'
}

const BPEditStatus = {
  SUBMITTED:  0,
  SUBMITTING: 1,
  MODIFIED:   2,
  ERROR:      3
}

var g_status, g_last_status;
var currentProgramStatus;

function getStatus () {
  return (g_status);
}

function setStatus (s) {
  var msg;

  g_last_status = getStatus ();
  g_status = s;

  switch (s) {
    case BPEditStatus.SUBMITTED:
      msg = '<i class="icon i-check w3-text-green"></i> Saved';
      break;
    case BPEditStatus.SUBMITTING:
      msg = '<i class="icon i-spinner w3-spin"></i> Saving';
      break;
    case BPEditStatus.MODIFIED:
      msg = '<i class="icon i-exclamation-triangle w3-text-orange"></i> Modified';
      break;
    case BPEditStatus.ERROR:
      msg = '<i class="icon i-exclamation w3-text-red"></i> Error';
      break;
    default:
      break;
  }

  document.getElementById('bpStatus').innerHTML = msg;
}

function getProgramStatus () {
  return (currentProgramStatus);
}

function setProgramStatus (s) {
  var msg;

  currentProgramStatus = s;

  switch (s) {
    case ProgramStatus.READY:
      msg = '<i class="icon i-check w3-text-green"></i> Ready';
      break;
    case ProgramStatus.COMPILED:
      msg = '<i class="icon i-check w3-text-green"></i> Compiled';
      break;
    case ProgramStatus.ERRORS:
      msg = '<i class="icon i-exclamation w3-text-red"></i> Error';
      break;
    default:
      break;
  }

  document.getElementById('programStatus').innerHTML = msg;
}

function compileEnd () {
  if (dialogWorking) {
    dialogWorking.destroy();
    dialogWorking = null;
  }
}

function compileProgram () {
  bpConsole.append ("Compiling program...");

  callServer ("POST", "/program/"+_jbp.programId+"/compile", null, function (xhttp) {
      if (xhttp.readyState == 4) {
        var message="", output;
      
        try {
          var jerr = JSON.parse (xhttp.responseText);
          message = jerr.message;
          output = jerr.output;
        }
        catch (err) {
          output = xhttp.responseText;
        }
            
        if (xhttp.status == 200) {
          //console.log(xhttp.responseText);
          var jo = JSON.parse(xhttp.responseText);

          bpConsole.append (output);
          bpConsole.append ("<i class=\"icon i-check w3-text-green\"></i> Program successfully compiled", BPConsoleTextType.SUCCESS);
          setProgramStatus (ProgramStatus.COMPILED);
          document.getElementById('bp-title').innerHTML = document.getElementById('bpName').value;
        }
        else {
          if (xhttp.status == 404)
            bpConsole.append ("Program not found", BPConsoleTextType.ERROR);
          else if (xhttp.status == 0)
            bpConsole.append ("Can't connect to server", BPConsoleTextType.ERROR);
          else {
              bpConsole.append (message, BPConsoleTextType.ERROR);
              bpConsole.append (output, BPConsoleTextType.ERROR);
          }

          setProgramStatus (ProgramStatus.ERRORS);
        }
        compileEnd ();
      }
    }
  );
}

function showError(m) {
    try {
      var j = JSON.parse(m);
      bpConsole.append ("Error: "+j.message, BPConsoleTextType.ERROR);
    } catch (err) {
      bpConsole.append ("Error : "+m, BPConsoleTextType.ERROR);
    }
}

function processHTTPError(xhttp) {
  if (xhttp.status == 404)
    bpConsole.append ("Resource not found", BPConsoleTextType.ERROR);
  else if (xhttp.status == 0)
    bpConsole.append ("Can't connect to server", BPConsoleTextType.ERROR);
  else {
    showError(xhttp.responseText);
    /*try {
      var j = JSON.parse(xhttp.responseText);
      bpConsole.append ("Error: "+j.message, BPConsoleTextType.ERROR);
    } catch (err) {
      bpConsole.append ("Error "+xhttp.status+": "+xhttp.responseText, BPConsoleTextType.ERROR);
    }*/
  }
}

function deployBlueprint () {
  var bpName = document.getElementById('bpName');
  blueprint.setName (bpName.value);
  bpConsole.append ("Submitting blueprint...");

  callServer ("PUT", "/blueprint/"+blueprint_id, blueprint.toString(), function (xhttp) {
      if (xhttp.readyState == 4) {

        if (xhttp.status == 200) {
          bpConsole.append ("<i class=\"icon i-check w3-text-green\"></i> Blueprint successfully submitted", BPConsoleTextType.SUCCESS);
          setStatus (BPEditStatus.SUBMITTED);
          undo.setCurrentAsSaved();

          compileProgram ();
        }
        else {
          processHTTPError(xhttp);
          compileEnd ();
        }
      }
    }
  );
}

function compileStart () {
  console.log("Compile");
  dialogWorking = dialogMessage (__SERVER.package.application_name, 'Compiling program...', DialogButtons.NONE, DialogIcon.RUNNING, null);

  if (getStatus() != BPEditStatus.SUBMITTED)
    deployBlueprint();
  else
    compileProgram();
}

var g_bp_height;

function toggleConsoleVisibility () {
  var visible = bpConsole.getVisible();
  var item = menubar.getMenuItem (MenuItems.VIEW_CONSOLE);
  var icon = item.childNodes[0];

  paned.collapse(1);
  //icon.innerHTML = visible ? '<i class="icon i-check"></i>' : "";
}

function getDefaultInput() {
  var jinput = blueprint.getInputArray();
  var k = 0, input = '{\n';

  for (var i=0; i<jinput.length; i++) {
    //console.log('type = '+jinput[i].type);
    if (jinput[i].type === 'Exec')
      continue;

    if (k > 0)
      input += ',\n';

    input += '  "'+jinput[i].label+'": '+JSON.stringify(jinput[i].value);
    k ++;
  }

  input += '\n}';
  
  return input;
}

function processAction (a) {
  if (!actionsEnabled)
    return;

  switch (a.id) {
    case MenuItems.BP_HOME:
      window.open("/home");
      break;

    case MenuItems.BP_HELP:
      var dialog = new Dialog ();

      dialog.setWidthMode(DialogWidthMode.MIN);
      dialog.setButtons(DialogButtons.OK);

      dialog.callbackOK = function (dialog) {
        dialog.destroy();
      }

      dialog.create('Informations');

      var content = dialog.getContentElement ();

      content.innerHTML = `
      <center>
        <img src="/img/logo-64.png"><br><br>`+
        `<b>`+__SERVER.package.application_name+` `+__SERVER.package.version+`<br>
        Blueprint editor</b><br><br>`+
        __SERVER.package.copyright+`
      <center>
      `;

      break;

    case MenuItems.BP_DEBUG:
      debugOnConsole();
      break;

    case MenuItems.FILE_OPEN:
      var input = document.getElementById('file-dialog');
      //console.log ("[processAction] "+input.name);
      input.click();
      break;

    case MenuItems.FILE_SAVE_AS:
      saveFileAs ();
      break;

    case MenuItems.EDIT_UNDO:
      if (blueprint.getStatus() == BPStatus.READY) {
        undo.begin ();
        var bp = undo.undo();

        if (bp) {
          console.log ('Undoing...');

          /* Save current position */
          var x0 = blueprint.x0, y0 = blueprint.y0;

          appLoadBlueprint (bp);
          blueprint.moveDelta (bp.x0 - x0, bp.y0 - y0);

					if (undo.isCurrentSaved())
            setStatus (BPEditStatus.SUBMITTED);
        }

        undo.end ();
      }
      break;

    case MenuItems.EDIT_REDO:
      if (blueprint.getStatus() == BPStatus.READY) {
        undo.begin ();
        var bp = undo.redo();

        if (bp) {
          console.log ('Redoing...');

          /* Save current position */
          var x0 = blueprint.x0, y0 = blueprint.y0;
          appLoadBlueprint (bp);
          blueprint.moveDelta (bp.x0 - x0, bp.y0 - y0);

					if (undo.isCurrentSaved())
            setStatus (BPEditStatus.SUBMITTED);
        }

        undo.end ();
      }
      break;

    case MenuItems.EDIT_COPY:
      if (blueprint.getStatus() == BPStatus.READY)
        blueprint.copySelection ();
      break;

    case MenuItems.EDIT_PASTE:
      if (blueprint.getStatus() == BPStatus.READY) {
        undo.begin();
        blueprint.paste(function() {
                undo.end();
                cbModified();       
              }
        );

      }
      break;

    case MenuItems.EDIT_DELETE:
      if (blueprint.getStatus() == BPStatus.READY) {
        //cbBeginModify ();
        undo.begin();
        blueprint.deleteSelection();
        undo.end();
        cbModified();
      }
      break;

    case MenuItems.EDIT_SELECT_ALL:
      if (blueprint.getStatus() == BPStatus.READY)
        blueprint.selectAll ();
      break;

    case MenuItems.VIEW_SHOW_GRID:
      blueprint.bgElement.style.visibility = blueprint.bgElement.style.visibility == 'visible' ? 'hidden' : 'visible';
      var item = menubar.getMenuItem (MenuItems.VIEW_SHOW_GRID);
      var icon = item.childNodes[0];
      icon.innerHTML = blueprint.bgElement.style.visibility == 'visible' ? '<i class="icon i-check"></i>' : "";
      break;

    case MenuItems.VIEW_SNAP_TO_GRID:
      blueprint.snapToGrid = !blueprint.snapToGrid;
      var item = menubar.getMenuItem (MenuItems.VIEW_SNAP_TO_GRID);
      var icon = item.childNodes[0];
      icon.innerHTML = blueprint.snapToGrid ? '<i class="icon i-check"></i>' : "";
      //console.log ("[processAction] blueprint.snapToGrid = "+blueprint.snapToGrid);
      break;

    case MenuItems.VIEW_CONSOLE:
      toggleConsoleVisibility ();
      break;

    case MenuItems.VIEW_ZOOM_IN:
      if (blueprint.zoom < 1)
        blueprint.zoomIn();
      break;

    case MenuItems.VIEW_ZOOM_OUT:
      if (blueprint.zoom > 0.4)
        blueprint.zoomOut();
      break;

    case MenuItems.VIEW_ZOOM_1_1:
        blueprint.setZoom(1);
      break;

    case MenuItems.BLUEPRINT_RUN:
      if (blueprint.type == 'EVENTS') {
        dialogInfo ("Events blueprint can't be executed.");
        break;
      }

      beginEdit ();
      var dialog = new Dialog ();
      dialog.setCallbackOK (run);
      dialog.callbackCancel = function (dialog) { dialog.destroy(); endEdit(); };
			dialog.setWidthMode(DialogWidthMode.MIN);
      dialog.create('Run');

      var content = dialog.getContentElement ();
      console.log(blueprint.getInputArrayAsString());
      
      input = lastInput ? lastInput : getDefaultInput();

      content.innerHTML = `<textarea class="w3-border" id="inputTextArea" rows="15" cols="30">`+input+`</textarea>
                           <button class="btnApp" onclick="document.getElementById('inputTextArea').value = getDefaultInput();">Reset</button>
                          `;
      break;

    case MenuItems.BLUEPRINT_COMPILE:
      compileStart();
      break;

    case MenuItems.BLUEPRINT_REVERT:
      dialogMessage ("Blueprint", "Revert to server version?<br>Changes will be lost.", DialogButtons.OK_CANCEL, DialogIcon.WARNING, function (dialog) {
          dialog.destroy();

          callServer ("GET", "/blueprint/"+blueprint_id+"?json=1", null,  function (xhttp) {
              if (xhttp.readyState == 4) {
                if (xhttp.status == 200) {
                  var j = JSON.parse(xhttp.responseText);
                  undo.begin();
                  appLoadBlueprint (j);
                  undo.end();
                  setStatus (BPEditStatus.SUBMITTED);
                }
                else {
                  processHTTPError(xhttp);
                }
              }
            }
          );
        }
      );
      break;

    default:
      //console.log("[processAction] action = "+a.id);
      break;
  }
}

function createMenu (id) {
  menubar = new Menubar();
  menubar.init (id);
  menubar.setActionCallback(processAction);
  menubar.fromJson (
  {
   "actions": [
     { "id": MenuItems.BP_HOME },
     { "id": MenuItems.BP_HELP },
     { "id": MenuItems.BP_DEBUG },

     { "id": MenuItems.FILE_OPEN, "ctrl": true, "key": "O" },
     { "id": MenuItems.FILE_SAVE_AS, "ctrl": true, "key": "S" },

     { "id": MenuItems.EDIT_SELECT_ALL, "ctrl": true, "key": "A","keyreleased": false, "keypressed": true },
     { "id": MenuItems.EDIT_DELETE, "key": 46, "keyreleased": true },
     { "id": MenuItems.EDIT_UNDO, "ctrl": true, "key": "Z" },
     { "id": MenuItems.EDIT_REDO, "ctrl": true, "key": "Y" },
     { "id": MenuItems.EDIT_COPY, "ctrl": true, "key": "C" },
     { "id": MenuItems.EDIT_PASTE, "ctrl": true, "key": "V" },

     { "id": MenuItems.VIEW_SHOW_GRID  },
     { "id": MenuItems.VIEW_SNAP_TO_GRID  },
     { "id": MenuItems.VIEW_CONSOLE  },
     { "id": MenuItems.VIEW_ZOOM_IN  },
     { "id": MenuItems.VIEW_ZOOM_OUT  },
     { "id": MenuItems.VIEW_ZOOM_1_1, "key":"1" },

     { "id": MenuItems.BLUEPRINT_RUN, "key": ActionKey.F9 },
     //{ "id": MenuItems.BLUEPRINT_COMMAND_LINE },
     //{ "id": MenuItems.BLUEPRINT_VALIDATE, "key": ActionKey.F10 },
     { "id": MenuItems.BLUEPRINT_COMPILE, "ctrl": true, "key": ActionKey.F12 },
     { "id": MenuItems.BLUEPRINT_REVERT, "key": ActionKey.F5 }
   ],
   "menubar": [
     {
       "menu": "<i class=\"icon i-project-diagram menu-element\"></i>",
       "items": [
         { "item": "Home", "id": MenuItems.BP_HOME, "icon": "<i class=\"icon i-home\"></i>" },
         { "separator": true },
         { "item": "Help", "id": MenuItems.BP_HELP, "icon": "<i class=\"icon i-info\"></i>" },
         { "item": "Debug", "id": MenuItems.BP_DEBUG }
       ]
     },
     {
       "menu": "File",
       "items": [
         { "item": "Open", "id": MenuItems.FILE_OPEN, "icon": "<i class=\"icon i-folder-open\"></i>" },
         { "item": "Save", "id": MenuItems.FILE_SAVE_AS, "icon": "<i class=\"icon i-save\"></i>" }
       ]
     },
     {
       "menu": "Edit",
       "items": [
         { "item": "Undo", "id": MenuItems.EDIT_UNDO, "icon": "<i class=\"icon i-undo\"></i>" },
         { "item": "Redo", "id": MenuItems.EDIT_REDO, "icon": "<i class=\"icon i-redo\"></i>" },
         { "separator": true },
         { "item": "Copy", "id": MenuItems.EDIT_COPY, "icon": "<i class=\"icon i-copy\"></i>" },
         { "item": "Paste", "id": MenuItems.EDIT_PASTE, "icon": "<i class=\"icon i-paste\"></i>" },
         { "separator": true },
         { "item": "Select all", "id": MenuItems.EDIT_SELECT_ALL }
       ]
     },
     {
       "menu": "View",
       "items": [
         { "item": "Show grid", "id": MenuItems.VIEW_SHOW_GRID, "checked":true },
         { "item": "Snap to grid", "id": MenuItems.VIEW_SNAP_TO_GRID, "checked":true },
         { "separator": true },
         { "item": "Collapse console", "id": MenuItems.VIEW_CONSOLE/*, "checked":true*/ },
         { "separator": true },
         { "item": "Zoom in", "id": MenuItems.VIEW_ZOOM_IN, "icon": "<i class=\"icon i-zoom-in\"></i>", "text":"Wheel up" },
         { "item": "Zoom out", "id": MenuItems.VIEW_ZOOM_OUT, "icon": "<i class=\"icon i-zoom-out\"></i>", "text":"Wheel down" },
         { "item": "Zoom 1:1", "id": MenuItems.VIEW_ZOOM_1_1, "icon": "<i class=\"icon i-search\"></i>" }
       ]
     },
     {
       "menu": "Blueprint",
       "items": [
         { "item": "Compile", "id": MenuItems.BLUEPRINT_COMPILE, "icon": '<i class="icon i-cogs"></i>' },
         { "item": "Run", "id": MenuItems.BLUEPRINT_RUN , "icon": "<i class=\"icon i-play\"></i>" },
         //{ "item": "Validate", "id": MenuItems.BLUEPRINT_VALIDATE, "icon": '<i class="icon i-check"></i>' },
         { "separator": true },
         { "item": "Revert", "id": MenuItems.BLUEPRINT_REVERT, "icon": "<i class=\"icon i-revert\"></i>" }
       ]
     }
   ],
   "toolbar": [
     { "item": "Open", "id": MenuItems.FILE_OPEN, "icon": "<i class=\"icon i-folder-open\"></i>" },
     { "item": "Save", "id": MenuItems.FILE_SAVE_AS, "icon": "<i class=\"icon i-save\"></i>" },
     { "separator": true },
     { "item": "Undo", "id": MenuItems.EDIT_UNDO, "icon": "<i class=\"icon i-undo\"></i>" },
     { "item": "Redo", "id": MenuItems.EDIT_REDO, "icon": "<i class=\"icon i-redo\"></i>" },
     { "separator": true },
     { "item": "Zoom in", "id": MenuItems.VIEW_ZOOM_IN, "icon": "<i class=\"icon i-zoom-in\"></i>" },
     { "item": "Zoom out", "id": MenuItems.VIEW_ZOOM_OUT, "icon": "<i class=\"icon i-zoom-out\"></i>" },
     { "item": "Zoom 1:1", "id": MenuItems.VIEW_ZOOM_1_1, "icon": "<i class=\"icon i-search\"></i>" },
     { "separator": true },
     { "item": "Compile", "id": MenuItems.BLUEPRINT_COMPILE, "icon": '<i class="icon i-cogs"></i>' },
     { "item": "Run", "id": MenuItems.BLUEPRINT_RUN, "icon": '<i class="icon i-play"></i>' }
     //{ "item": "Validate", "id": MenuItems.BLUEPRINT_VALIDATE, "icon": '<i class="icon i-check"></i>' },
   ]
  }
  );
}

function dragVariable(ev) {
  //ev.dataTransfer.setData("text", ev.target.id.replace ('row_', ''));
  //var id=ev.target.id.replace('row_', '');
  console.log(ev.target);
  var id = ev.target.getAttribute('_varid');
  ev.dataTransfer.setData("data", '{ "tag":"VARIABLE", "id":"'+id+'"}');
}

function beginEdit(ev) {
  actionsEnabled = false;
  console.log ("[beginEdit] actionsEnabled = "+actionsEnabled);
}

function endEdit(ev) {
  actionsEnabled = true;
  console.log ("[beginEdit] actionsEnabled = "+actionsEnabled);
}

function beginRename(ev) {

  var id = ev.target.getAttribute('_varid');
  //var oldName = ev.target.innerHTML;
  var oldName = ev.target.value;

  beginEdit();
}

function endRenameConnector(ev) {

  var id = ev.target.getAttribute('_varid');
  //var newName = ev.target.innerHTML;
  var newName = ev.target.value;
  var connector = blueprint.getConnector (id);

  if (connector) {
    connector.setLabel (newName);
    blueprint.redrawEdges();
  }
  else
    console.error ("[endRenameConnector] Error: connector not found: "+id);

  endEdit();
}

function updateVariableColor (id, typeId) {
  var elem = document.getElementById(id);
  /*var types = blueprint.getTypes();
  elem.style.color = types[typeId].color;*/
  elem.style.color = blueprint.getType(typeId).color;

  /* Update icon */
  elem.className = "";
  elem.classList.add ('icon');
  elem.classList.add ('i-circle'/*getTypeIcon (typeId)*/);
}

function connectorTypeChanged(ev) {
  var id = ev.target.getAttribute('_varid');
  //var v = blueprint.getVariable (id);
  var connector = blueprint.getConnector (id);

  console.log("Connector type changed to "+ev.target.value);

  connector.disconnectAll ();
  connector.setType (ev.target.value);
  connector.setDefaultValue ();
  updateVariableColor ('i_'+id, ev.target.value);
}

function connectorArrayChanged(ev) {
  var id = ev.target.getAttribute('_varid');
  //var v = blueprint.getVariable (id);
  var connector = blueprint.getConnector (id);

  console.log("Connector array changed to "+ev.target.checked);

  connector.disconnectAll ();
  connector.setDimensions (ev.target.checked ? 1 : 0);
  //connector.setDefaultValue ();
}

function deleteConnectorCallback(id) {
  var connector = blueprint.getConnector (id);
  var node = connector.owner;

  /* input parameters have output connectors */
  var elemId = connector.direction == BPDirection.INPUT ? '_out_'+id : '_in_'+id;
  var elem = document.getElementById(elemId);
  elem.parentElement.removeChild(elem);

  // Show button again, if output variable
  if (connector.direction == BPDirection.INPUT)
    document.getElementById("btn_add_output").style.display = "block";

  connector.disconnectAll ();
  node.deleteConnector (connector);
  blueprint.redrawEdges();
  setStatus (BPEditStatus.MODIFIED);
}

function appAddInOut(paramType, target)
{
  var targetId, targetName, targetType, tabId, rowPrefix;
  var table, cbBeginRename, cbEndRename, cbDelete, cbTypeChanged;
  var types = blueprint.getTypes();
  var draggable = false;

  var tr = document.createElement('tr');

  switch (paramType) {
    case AppParameterType.INPUT:
    case AppParameterType.OUTPUT:
      tabId = paramType == AppParameterType.INPUT ? 'tab_input' : 'tab_output';
      targetId = target.id;
      targetName = target.getLabel();
      targetType = target.dataType.name;
      rowPrefix = paramType == AppParameterType.INPUT ? '_in_' : '_out_';
      cbBeginRename = 'beginRename';
      cbEndRename = 'endRenameConnector';
      cbTypeChanged = 'connectorTypeChanged';
      cbDelete = 'deleteConnectorCallback';

      if (paramType == AppParameterType.OUTPUT)
        document.getElementById("btn_add_output").style.display = "none";

      break;

    default:
      break;
  }

  table = document.getElementById(tabId);
  tr.setAttribute('id', rowPrefix+targetId);

  // Type combo
  var color, type_id;
  var combo = '<select _varid='+targetId+' onchange="'+cbTypeChanged+'(event);" class="select-css">';
  for (var i=0; i<types.length; i++) {
    if (!types[i].exec) {
      //var selected = types[i].id == targetType ? ' selected="selected"' : '';
      var selected = '';

      if (types[i].name == targetType) {
        selected = ' selected="selected"';
        color = types[i].color;
        type_id = types[i].name;
      }

      combo += '<option value="'+types[i].name+'" '+selected+'>'+types[i].name+'</option>';

    }
  }
  combo += '</select>';
  
  // Array flag
  var checked = target.getDimensions() > 0 ? "checked" : "";
  var arrayFlag = '<input _varid='+targetId+' type="checkbox" id="array" name="array" value="0" onclick="connectorArrayChanged(event);" '+checked+'> Array';
  
  // Row
  var htmlRow = `
     <td class="tdVars" valign="top">
        <i id="i_`+targetId+`" class="icon i-circle" style="color:`+color+`; padding-right:4px"></i>
     </td>
     <td class="tdVars">
        <input id="input_`+targetId+`" class="inputConnector" _varid=`+targetId+` style="width:12em; margin-bottom:3px;" value="`+targetName+`" name="varName" onfocus="`+cbBeginRename+`(event)" onblur="`+cbEndRename+`(event);"  ondragstart="return false;" ondrop="return false;">

      <br>
      `+combo+`<br>
      `+arrayFlag+`
     </td>
     <td class="tdVars" valign="top">
       <i class="icon i-times w3-text-gray w3-hover-text-red" onclick="`+cbDelete+`('`+targetId+`');" style="cursor:pointer;" title="Delete"></i>
     </td>
   `;

    tr.innerHTML = htmlRow;
    table.appendChild(tr);

    var inElem = document.getElementById('input_'+targetId);
    setInputFilter(inElem, function(value) { return /^[a-zA-Z0-9_-]*$/.test(value); });

    setStatus (BPEditStatus.MODIFIED);
    cbModified ();
}

function appAddVariable(v)
{
  var cbDelete = v.isGlobal() ? 'deleteGlobalVariable' : 'deleteVariableCallback';
  var deleteButton = `<div class="w3-right" style="padding-left:0.5em;"><i class="icon i-times w3-text-gray w3-hover-text-red" onclick="`+cbDelete+`('`+v.id+`');" style="cursor:pointer;" title="Delete"></i></div>`;
  var editButton = `<div class="w3-right" style="padding-left:0.5em;"><i class="icon i-edit w3-text-gray" onclick="editVariable('`+v.id+`');" style="cursor:pointer;" title="Edit"></i></div>`;
  var array = v.getArray() == 0 ? '' : '<i class="icon ellipsis-h" style="color:darkgray"></i>';
	var color = blueprint.getType(v.getType()).color;

  // Entire row
  var varRow = document.createElement('div');
  varRow.classList.add ('variable');
  varRow.classList.add ('w3-round');
  varRow.setAttribute('_varid', v.id);
  varRow.ondragstart = dragVariable;
  varRow.draggable = true;
  varRow.innerHTML = `<i class="icon grip-lines-vertical" style="color:silver;padding-right:4px;"></i>
                       <i id="i_`+v.id+`" class="icon i-circle" style="color:`+color+`; padding-right:4px"></i> `+v.getName()+` `+array+`
                      `+deleteButton+` `+editButton;    
	
	v.element = varRow;
  document.getElementById(v.isGlobal() ? 'global_vars' : 'local_vars').appendChild(varRow);

  setStatus (BPEditStatus.MODIFIED);
  cbModified ();
}

/* addInputCallback() - Triggered by button */
function addInputCallback()
{
  var connector = blueprint.addInput ('Integer', 'Input', null);
  appAddInOut(AppParameterType.INPUT, connector);
}

/* addOutputCallback() - Triggered by button */
function addOutputCallback()
{
  var connector = blueprint.addOutput ('Integer', 'Output', 0);
  appAddInOut(AppParameterType.OUTPUT, connector);
}

/**
 * Add variable v to blueprint and application
 */
function addVariable(v)
{
  blueprint.addVariable (v);
  appAddVariable (v);
}

/**
 * Create a local variable (triggered by button)
 */
function addLocalVariable()
{
  undo.begin();
  var v = blueprint.addNewVariable ();
  appAddVariable (v);
  cbModified();
  undo.end();
}

/**
 * Create a global variable (triggered by button)
 */
function addGlobalVariable() {
  undo.begin();
  var v = blueprint.addNewVariable (true);
  appAddVariable (v);
  cbModified();
  undo.end();
}

function endRenameVariable(ev) {
  var id = ev.target.getAttribute('_varid');
  var newName = ev.target.value;

  var v = blueprint.getVariable (id);
  var oldName = v.getName();

  console.log ("Renaming "+oldName+" to "+newName);

  if (blueprint.setVariableName (v, newName)) {
    appRefreshVariables();
    cbModified();
  } else {
    ev.target.value = oldName;
    bpConsole.append ("Variable "+oldName+" can't be renamed "+newName, BPConsoleTextType.ERROR);
  }

  actionsEnabled = true;
}

/**
 * Delete a global variable (triggered by button)
 */
function deleteGlobalVariable(id) {
  var v = blueprint.getVariable (id);

  if (v.referenced) {
    dialogMessage ("Blueprint", "You cannot delete a referenced variable.", DialogButtons.OK, DialogIcon.WARNING, function (dialog) {
        dialog.destroy();
      }
    );
    return;
  }

  callServer ("DELETE", "/program/"+_jbp.programId+"/variable/"+v.getName(), null, function (xhttp) {
      if (xhttp.readyState == 4) {
        if (xhttp.status == 200) {
          deleteVariableCallback(id);
          bpConsole.append ("Deleted global variable "+v.getName());
        }
        else {
          if (xhttp.status == 0)
            bpConsole.append ("Can't connect to server", BPConsoleTextType.ERROR);
          else {
              var jerr = JSON.parse (xhttp.responseText);
              bpConsole.append ("Can't delete variable "+v.getName()+": "+jerr.message, BPConsoleTextType.ERROR);
          }
        }
      }
    }
  );
}

/**
 * Delete a variable with id 'id' (triggered by button)
 */
function deleteVariableCallback(id)
{
  //var vElem = document.getElementById('row_'+id);
  var v = blueprint.getVariable (id);

  if (v.referenced) {
    dialogMessage ("Blueprint", "You cannot delete a referenced variable.", DialogButtons.OK, DialogIcon.WARNING, function (dialog) {
        dialog.destroy();
      }
    );
    return;
  }

  v.element.parentElement.removeChild(v.element);
  blueprint.deleteVariable (v);
  cbModified();
}

/**
 * Change type of variable
 */
function variableTypeChanged(ev) {
  var id = ev.target.getAttribute('_varid');
  var v = blueprint.getVariable (id);

  if (v.referenced) {
    dialogMessage ("Blueprint", "You cannot change the type of a referenced variable.", DialogButtons.OK, DialogIcon.WARNING, function (dialog) {
        dialog.destroy();
      }
    );

    // Restore control value
    ev.target.value = v.type;
    return;
  }

  setVariableType(v, ev.target.value);
  cbModified();
}

function variableArrayChanged(ev) {
  var id = ev.target.getAttribute('_varid');
  var v = blueprint.getVariable (id);

  if (v.referenced) {
    dialogMessage ("Blueprint", "You cannot change the array type of a referenced variable.", DialogButtons.OK, DialogIcon.WARNING, function (dialog) {
        dialog.destroy();
      }
    );

    // Restore control value
    ev.target.checked = v.getArray() > 0 ? 1 : 0;
    return;
  }

  v.setArray(ev.target.checked ? 1 : 0);
  appRefreshVariables();
  cbModified();
}

function setVariableType(v, type) {
  var updVar = blueprint.setVariableType (v, type);
  console.log (updVar.getName()+" updated to "+updVar.getType());
  updVar.reset();
  updateVariableColor ('i_'+v.id, type);
}

function editVariable(id) {
  var type = null;
  var v = blueprint.getVariable (id);
  var types = blueprint.getTypes();

    beginEdit();

    var dialog = new Dialog ();
    dialog.setButtons(DialogButtons.CLOSE);
    dialog.callbackOK = function (dialog) {
        dialog.destroy();
        endEdit();
    }
    dialog.callbackCancel = function (dialog) { dialog.destroy(); endEdit(); };
		//dialog.setWidthMode(DialogWidthMode.MIN30);
    dialog.create('Edit variable');

    var content = dialog.getContentElement ();
    
    var combo = '<select id="ctrlTypes" _varid='+v.id+' _global='+v.global+' onchange="variableTypeChanged(event);" class="w3-select">';
    
    for (var i=0; i<types.length; i++) {
      if (!types[i].exec) {
        var selected = '';

        if (types[i].name == v.type)
          selected = ' selected="selected"';

        combo += '<option value="'+types[i].name+'" '+selected+'>'+types[i].name+'</option>';
      }
    }
    combo += '</select>';

    content.innerHTML = `
      <table>
        <tr><td>Name</td><td><input class="w3-input" _varid=`+v.id+` value="`+v.name+`" onfocus="beginRename(event)" onblur="endRenameVariable(event);"></td></tr>
        <tr><td>Type</td><td>`+combo+`</td></tr>
        <tr><td>Array</td><td><input id="ctrlArray" class="w3-check" type="checkbox" _varid=`+v.id+` name="array" value="0" onclick="variableArrayChanged(event);" `+(v.getArray() > 0 ? 'checked' : '')+`></td></tr>
      </table>
    `;

    if (v.referenced) {
      document.getElementById("ctrlTypes").disabled = true;
      document.getElementById("ctrlArray").disabled = true;
    }
    
    // TODO
    //var inElem = document.getElementById('input_'+v.id);
    //setInputFilter(inElem, function(value) { return /^[a-zA-Z0-9_-]*$/.test(value); });
}

function appClearVariables ()
{
  var table = document.getElementById("global_vars");
  table.innerHTML = "";

  var table = document.getElementById("local_vars");
  table.innerHTML = "";
}

function appClearAll ()
{
  var table = document.getElementById("tab_input");
  table.innerHTML = "";

  var table = document.getElementById("tab_output");
  table.innerHTML = "";

  appClearVariables ();
}

function debugOnConsole() {
  undo.dump();

  for (var i=0; i<blueprint.variables.length; i++)
    bpConsole.append (blueprint.variables[i].toString());

  for (var i=0; i<blueprint.nodes.length; i++)
    bpConsole.append (blueprint.nodes[i].toString());
}

function onUnload(event) {
  e.preventDefault();
  event.returnValue = '';
  if (getStatus() != BPEditStatus.SUBMITTED) {

  }
  else {

  }
}
