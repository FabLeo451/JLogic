'use strict';

const MenuItems = {
 BP:             9,
 BP_HOME:      901,
 BP_HELP:      902,
 
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

function appLoadBlueprint (j) {
  var connectors;
  
  console.log ("[application] [appLoadBlueprint] Start");
  
  appClearVariables ();
  blueprint.clear();
  
  console.log ("Adding variables...");

  for (var i=0; i<j["variables"].length; i++) {
    var v = new Variable(j["variables"][i].name, 
                         j["variables"][i].type, 
                         j["variables"][i].dimensions/*isArray*/);
    v.id = j["variables"][i].id;
    /*v.name = j["variables"][i].name;
    v.type = j["variables"][i].type;*/
    v.referenced = j["variables"][i].referenced;
    
    /* Check value */
    if (j["variables"][i].hasOwnProperty('value')) {
      v.set(j["variables"][i].value);
    }
    
    //console.log("[readFile] Creating "+v.toString());
    
    /* Add to sidebar */
    console.log ("Adding "+v.name+" to sidebar...");
    appAddVariable (AppParameterType.VARIABLE, v);
    
    /* Add to blueprint */
    console.log ("Adding "+v.name+" to blueprint...");
    blueprint.addVariable (v);
  }
  
  /* Build blueprint */
  blueprint.fromJson (j);
  
  console.log ("Adding input");
  
  /* Input */
  connectors = blueprint.entryPointNode.connectors;
  
  for (var i=0; i<connectors.length; i++) {
    if (connectors[i].pinType.id != BPTypeID.EXEC)
      appAddVariable (AppParameterType.INPUT, connectors[i]);
  }
  
  console.log ("[application] [appLoadBlueprint] Adding output");
  
  /* Output */
  connectors = blueprint.returnNode.connectors;
  
  for (var i=0; i<connectors.length; i++) {
    if (connectors[i].pinType.id != BPTypeID.EXEC)
      appAddVariable (AppParameterType.OUTPUT, connectors[i]);
  }
  
  var bpName = document.getElementById('bpName');
  bpName.value = blueprint.getName();
  document.title = blueprint.getName();
  
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
    var j = JSON.parse(contents);
    appLoadBlueprint (j);

    setStatus (BPEditStatus.MODIFIED);
  
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

function cbModified () {
  //console.log ("[application] [cbModified]");
  
  if (!blueprint)
    return;
    
  setStatus (BPEditStatus.MODIFIED);
  
  try {
    undo.setCurrent (blueprint.toJson());
  }
  catch (err) {
    //console.log (blueprint.toString());
  }
  
  //undo.dump();
}

function onNameChanged() {
  //document.getElementById('bp-title').innerHTML = document.getElementById('bpName').value;
  blueprint.onModified ();
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
  /*
  bpConsole.textElem.onmouseleave = function () {
    console.log ("Console lost focus");
    endEdit ();
  }
  */

  bpConsole.clear();

      
  undo = new UndoRedo ();
  undo.begin();

  console.log ('Initializing blueprint...');
  
  //bpStart (null);
  blueprint = new Blueprint ();
  blueprint.init ();
  blueprint.setAsset (_asset);

  // Get blueprint id
  var args = window.location.pathname.split("/");
  blueprint_id = args[2];
  
  var dialog = dialogMessage ('Working', 'Getting blueprint...', DialogButtons.NONE, DialogIcon.RUNNING, null);

  callServer ("GET", "/blueprint/"+blueprint_id, null, function (xhttp) {
    if (xhttp.readyState == 4) {

      if (xhttp.status == 200) {
        console.log ('Blueprint OK');

        _jbp = JSON.parse(xhttp.responseText);
       
        // Get program index
        
        console.log ('Getting program ...');

        callServer ("GET", "/program/"+ _jbp.programId, null, function (xhttp) {
            if (xhttp.readyState == 4) {
              if (xhttp.status == 200) {
                console.log ('Program OK');
                
                program = JSON.parse(xhttp.responseText);
                
                // Program info taken. Go on setting stuff
                
                appLoadBlueprint (_jbp);
                setStatus (BPEditStatus.SUBMITTED);
                showSnacknar(BPResult.SUCCESS, 'Blueprint '+_jbp["name"]+' loaded', 2000);
                
                console.log ('[application] [appStart] Setting callbacks');
                blueprint.setCallbackBeginModify(cbBeginModify);
                blueprint.setCallbackModified(cbModified);
                setStatus (BPEditStatus.SUBMITTED);
                
                document.getElementById('programName').innerHTML = program.name;
                document.getElementById('bp-title').innerHTML = _jbp.name;
                
                // Check if it's a main blueprint
                
                if (_jbp.type == BlueprintType.MAIN) {
                  document.getElementById("bpName").readOnly = true;
                  document.getElementById('input-panel').style.display = 'none';
                  document.getElementById('output-panel').style.display = 'none';
                }
      
                // Set program status
                
                setProgramStatus(program.status);

                undo.end();
                undo.setCurrent (blueprint.toJson());
                undo.dump();
                                   
                dialog.destroy();
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
/*    
  appLoadBlueprint (_jbp);
  setStatus (BPEditStatus.SUBMITTED);
  showSnacknar(BPResult.SUCCESS, 'Blueprint '+_jbp["name"]+' loaded', 2000);
  
  console.log ('[application] [appStart] Setting callbacks');
  blueprint.setCallbackBeginModify(cbBeginModify);
  blueprint.setCallbackModified(cbModified);
  setStatus (BPEditStatus.SUBMITTED);

  undo.end();
  undo.setCurrent (blueprint.toJson());
  undo.dump();
*/ 


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
      dialogRunning = dialogMessage ("Blueprint", "Executing...", DialogButtons.STOP, DialogIcon.RUNNING, function (dialog) {
          //dialog.destroy();
          document.getElementById("_dlg_msg").innerHTML = "Stopping...";
          
          callServer ("POST", "/blueprint/"+blueprint_id+"/stop?client_id="+__SERVER['client_id'], '', function (xhttp) {
            if (xhttp.readyState == 4) {
              if (xhttp.status == 200) {
                //dialogRunning.destroy();
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
      bpConsole.append ("Connected");
      break;
      
    case 3:
      bpConsole.append ("Getting response...");
      break;
      
    case 4:
      //modalHide ();
      dialogRunning.destroy();
      
      /*var n = xhttp.responseText.search("{");
      var response = xhttp.responseText.substring (n);*/
      var response = xhttp.responseText;
      
      if (!bpConsole.getVisible())
        toggleConsoleVisibility();

      if (xhttp.status == 200) {
        bpConsole.append ('<i class=\"icon i-check w3-text-green\"></i> Successfully executed', BPConsoleTextType.SUCCESS);
        bpConsole.append (response);
      }
      /*else if (xhttp.status == 404)
        bpConsole.append ("Blueprint not found", BPConsoleTextType.ERROR);
      else if (xhttp.status == 401)
        bpConsole.append ("Not authorized", BPConsoleTextType.ERROR);
      else if (xhttp.status == 500)
        bpConsole.append ("Internal server error", BPConsoleTextType.ERROR);*/
      else if (xhttp.status == 0)
        bpConsole.append ("Can't connect server", BPConsoleTextType.ERROR);
      else {
        //console.log ("xhttp.status = "+xhttp.status);
        var jo;
        
        try {
          jo = JSON.parse(response);
          bpConsole.append ('Error: '+jo.message, BPConsoleTextType.ERROR);
        }
        catch (err) {
          console.error(err.message);
          bpConsole.append ('Error: '+response);
        }
        
        bpConsole.append ('Execution failed', BPConsoleTextType.ERROR);
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
  
  callServer ("POST", "/program/"+_jbp.programId+"/run/"+blueprint.getName(), input, runCallback);
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
        if (xhttp.status == 200) {
          var jo = JSON.parse(xhttp.responseText);

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
            try {
              var jerr = JSON.parse (xhttp.responseText);
              bpConsole.append (jerr.message, BPConsoleTextType.ERROR);
              bpConsole.append (jerr.output, BPConsoleTextType.ERROR);
            }
            catch (err) {
              bpConsole.append ("Error "+xhttp.status+": "+xhttp.responseText, BPConsoleTextType.ERROR);
            }
            
          }
           
          setProgramStatus (ProgramStatus.ERRORS);
        }
        compileEnd ();
      }
    }
  );
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
          
          compileProgram ();
        }
        else {
          if (xhttp.status == 404)
            bpConsole.append ("Resource not found", BPConsoleTextType.ERROR);
          else if (xhttp.status == 0)
            bpConsole.append ("Can't connect to server", BPConsoleTextType.ERROR);
          else
            bpConsole.append ("Error "+xhttp.status+": "+xhttp.responseText, BPConsoleTextType.ERROR);
            
          //setStatus (g_last_status);
          
          compileEnd ();
        }
      }
    }
  );
}

function compileStart () {
  console.log("Compile");
  dialogWorking = dialogMessage ('Working', 'Compiling...', DialogButtons.NONE, DialogIcon.RUNNING, null);
  
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
/*  
  if (visible)
    g_bp_height = blueprint.getHeight();
  
  blueprint.setHeight(visible ? "100%" : g_bp_height+"px");
    
  visible = bpConsole.toggleVisibility ();
*/
  paned.collapse(1);
  //icon.innerHTML = visible ? '<i class="icon i-check"></i>' : "";
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
      
      dialog.setButtons(DialogButtons.OK);
      
      dialog.callbackOK = function (dialog) {
        dialog.destroy();
      }

      dialog.create('Informations');
      
      var content = dialog.getContentElement ();
      
      content.innerHTML = `
      <center>
        <img src="/img/logo-64.png"><br><br>
        <b>JLogic blueprint editor</b><br><br>
        Copyright (c) 2020 Fabio Leone
      <center>
      `;

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
        }
        
        undo.end ();
      }
      break;
      
    case MenuItems.EDIT_COPY:
      if (blueprint.getStatus() == BPStatus.READY)
        blueprint.copySelection ();
      break;
      
    case MenuItems.EDIT_PASTE:
      if (blueprint.getStatus() == BPStatus.READY)
        blueprint.paste ();
      break;
      
    case MenuItems.EDIT_DELETE:
      if (blueprint.getStatus() == BPStatus.READY) {
        //cbBeginModify ();
        undo.begin();
        blueprint.deleteSelection ();
        undo.end();
        cbModified ();
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
      /*
      var visible = bpConsole.toggleVisibility ();
      var item = menubar.getMenuItem (MenuItems.VIEW_CONSOLE);
      var icon = item.childNodes[0];
      icon.innerHTML = visible ? '<i class="icon i-check"></i>' : "";
      console.log (blueprint.getHeight());
      blueprint.setHeight("100%");
      */
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
    /*  
    case MenuItems.BLUEPRINT_VALIDATE:
      callServer ("POST", "/blueprint/validate", blueprint.toString(), function (xhttp) {
          if (xhttp.readyState == 4) {
            if (xhttp.status == 200) {
              showSnacknar(BPResult.SUCCESS, "Blueprint is valid", 3000);
              bpConsole.append ("<i class=\"icon i-check w3-text-green\"></i> Blueprint is valid", BPConsoleTextType.SUCCESS);
            }
            else {
              if (xhttp.status == 404)
                bpConsole.append ("Resource not found", BPConsoleTextType.ERROR);
              else if (xhttp.status == 0)
                bpConsole.append ("Can't connect to server", BPConsoleTextType.ERROR);
              else
                bpConsole.append ("Blueprint is not valid.\n"+xhttp.responseText, BPConsoleTextType.ERROR);
            }
          }
        }
      );
      break;*/
      
    case MenuItems.BLUEPRINT_RUN:
      beginEdit ();
      var dialog = new Dialog ();
      dialog.setCallbackOK (run);
      dialog.callbackCancel = function (dialog) { dialog.destroy(); endEdit(); };
      dialog.create('Run <b>'+blueprint.getName()+'</b>');
      
      var content = dialog.getContentElement ();
      console.log(blueprint.getInputArrayAsString());
      var jinput = JSON.parse(blueprint.getInputArrayAsString());
      var k = 0, input = '{\n';

      
      for (var i=0; i<jinput.length; i++) {
        //console.log('type = '+jinput[i].type);
        if (jinput[i].type === BPTypeID.EXEC)
          continue;
         
        if (k > 0)
          input += ',\n';
          
        input += '  "'+jinput[i].label+'": '+JSON.stringify(jinput[i].value);
        k ++;
      }
      
      input += '\n}';
      
      content.innerHTML = '<textarea class="w3-border" id="inputTextArea" rows="15" cols="30">'+input+'</textarea><!--<input id="_trace" class="w3-check" type="checkbox"> Trace -->';
      break;
    /* 
    case MenuItems.BLUEPRINT_COMMAND_LINE:
      callServer ("POST", "/blueprint/"+blueprint_id+"/exec?command_line=1", null,  function (xhttp) {
          if (xhttp.readyState == 4) {
            if (xhttp.status == 200) {
              //showSnacknar(BPResult.SUCCESS, "Blueprint is valid", 3000);
              bpConsole.append (xhttp.responseText);
            }
            else {
              if (xhttp.status == 404)
                bpConsole.append ("Resource not found", BPConsoleTextType.ERROR);
              else if (xhttp.status == 0)
                bpConsole.append ("Can't connect to server", BPConsoleTextType.ERROR);
              else
                bpConsole.append ("Can't get command line: "+xhttp.responseText+"\n", BPConsoleTextType.ERROR);
            }
          }
        }
      );
      break;*/
      
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
                  if (xhttp.status == 404)
                    bpConsole.append ("Resource not found", BPConsoleTextType.ERROR);
                  else if (xhttp.status == 0)
                    bpConsole.append ("Can't connect to server", BPConsoleTextType.ERROR);
                  else
                    bpConsole.append ("Can't get blueprint: "+xhttp.responseText+"\n", BPConsoleTextType.ERROR);
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
         { "item": "Help", "id": MenuItems.BP_HELP, "icon": "<i class=\"icon i-info\"></i>" }
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
         { "item": "Run", "id": MenuItems.BLUEPRINT_RUN , "icon": "<i class=\"icon i-play\"></i>" },
         //{ "item": "Command line", "id": MenuItems.BLUEPRINT_COMMAND_LINE , "icon": "<i class=\"icon i-terminal\"></i>" },
         { "separator": true },
         //{ "item": "Validate", "id": MenuItems.BLUEPRINT_VALIDATE, "icon": '<i class="icon i-check"></i>' },
         { "item": "Compile", "id": MenuItems.BLUEPRINT_COMPILE, "icon": '<i class="icon i-cogs"></i>' },
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
     { "item": "Run", "id": MenuItems.BLUEPRINT_RUN, "icon": '<i class="icon i-play"></i>' },
     { "separator": true },
     //{ "item": "Validate", "id": MenuItems.BLUEPRINT_VALIDATE, "icon": '<i class="icon i-check"></i>' },
     { "item": "Compile", "id": MenuItems.BLUEPRINT_COMPILE, "icon": '<i class="icon i-cogs"></i>' }
   ]
  }
  );
}

function dragVariable(ev) {
  //ev.dataTransfer.setData("text", ev.target.id.replace ('i_', ''));
  ev.dataTransfer.setData("text", ev.target.id.replace ('row_', ''));
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
  elem.classList.add (getTypeIcon (typeId));
}

function connectorTypeChanged(ev) {
  var id = ev.target.getAttribute('_varid');
  //var v = blueprint.getVariable (id);
  var connector = blueprint.getConnector (id);
  
  connector.disconnectAll ();
  connector.setType (ev.target.value);
  connector.setDefaultValue ();
  updateVariableColor ('i_'+id, ev.target.value);
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

function endRenameVariable(ev) {
  
  var id = ev.target.getAttribute('_varid');
  //var newName = ev.target.innerHTML;
  var newName = ev.target.value;
  
  //console.log ("[endRenameVariable] id = "+id+" new name = "+newName);
  
  var v = blueprint.getVariable (id);
  
  if (v) {
    v = blueprint.setVariableName (v, newName);
    //blueprint.redrawEdges();
  }
  else
    console.error ("[endRenameVariable] Error: variable not found: "+id);
    
  ev.target.innerHTML = v.name;
  actionsEnabled = true;
  //console.log ("[endRenameVariable] Changing var "+v.name);
}

function variableTypeChanged(ev) {
  var id = ev.target.getAttribute('_varid');
  var v = blueprint.getVariable (id);

  if (v.referenced) {
    //alert("You cannot change the type of a referenced variable.");
    dialogMessage ("Blueprint", "You cannot change the type of a referenced variable.", DialogButtons.OK, DialogIcon.WARNING, function (dialog) {
        dialog.destroy();
      }
    );
    
    ev.target.value = v.type;
    return;
  }
  
  v = blueprint.setVariableType (v, ev.target.value);
  v.reset();
  updateVariableColor ('i_'+id, ev.target.value);
}

function setInitialValue(id) {
  var type = null;
  var v = blueprint.getVariable (id);
  
  //console.log ('var type = "'+v.getType()+'" '+typeof v.getType());
  //console.log ('BPTypeID.FLOAT = "'+BPTypeID.FLOAT+'" '+typeof BPTypeID.FLOAT);
  
  switch (v.getType()) {
    case BPTypeID.BOOLEAN:
      type = DataType.BOOLEAN;
      break;
      
    case BPTypeID.INTEGER:
      type = DataType.INTEGER;
      break;
      
    case BPTypeID.FLOAT:
      type = DataType.NUMBER;
      break;
      
    case BPTypeID.STRING:
      type = DataType.STRING;
      break;
      
    case BPTypeID.JSON:
      type = DataType.JSON;
      break;
      
    default:
      //console.error ('Unknown variable type: '+v.getType());
      break;
  }

  
  if (type) {
    beginEdit();
    
    /*dialogEditData (type, v.getName(), v.get(), 0, function (dialog) {
        //console.log ('Seting value '+document.getElementById("dataValue").value);
        v.set(document.getElementById("dataValue").value);
        dialog.destroy();
      }
    );*/
    var dialog = new DialogData ();
    
    dialog.callbackOK = function (dialog) {
      if (dialog.checkData()) {
        //console.log ('Seting value '+dialog.getData());
        v.set(dialog.getData());
        //console.log (v.toString());
        dialog.destroy();
        endEdit();
        //console.log ('New value '+v.get());
        
        setStatus (BPEditStatus.MODIFIED);
        cbModified ();
      }
      else
        dialog.showError('Invalid data');
    }
    
    dialog.callbackCancel = function (dialog) { dialog.destroy(); endEdit(); };
    dialog.create(type, v.getName(), v.get(), 0);
    
  }
}

function appAddVariable(paramType, target) 
{
  var targetId, targetName, targetType, tabId, /*prefix,*/ rowPrefix;
  var table, cbBeginRename, cbEndRename, cbDelete, cbTypeChanged, initialize='';
  var types = blueprint.getTypes();
  var draggable = false;

  var tr = document.createElement('tr');
  
  switch (paramType) {
    case AppParameterType.INPUT:
    case AppParameterType.OUTPUT:
      tabId = paramType == AppParameterType.INPUT ? 'tab_input' : 'tab_output';
      targetId = target.id;
      targetName = target.getLabel();
      targetType = target.dataType.id;
      rowPrefix = paramType == AppParameterType.INPUT ? '_in_' : '_out_';
      cbBeginRename = 'beginRename';
      cbEndRename = 'endRenameConnector';
      cbTypeChanged = 'connectorTypeChanged';
      cbDelete = 'deleteConnectorCallback';
      
      if (paramType == AppParameterType.OUTPUT)
        document.getElementById("btn_add_output").style.display = "none";
        
      break;
      
    default:
      tabId = 'tab_variables';
      targetId = target.id;
      targetName = target.name;
      targetType = target.type;
      //tr.ondragstart = dragVariable;
      //tr.draggable = true;
      //target.element = tr;
      draggable = true;
      //prefix = 'var_';
      rowPrefix = 'row_';
      cbBeginRename = 'beginRename';
      cbEndRename = 'endRenameVariable';
      cbDelete = 'deleteVariableCallback';
      cbTypeChanged = 'variableTypeChanged';
      initialize = '<br><button class="btnApp" onclick="setInitialValue('+targetId+');" style="background-color:seagreen;">Initialize</button>';
      break;
  }
  
  table = document.getElementById(tabId);
  tr.setAttribute('id', rowPrefix+targetId);
    
  var color, type_id;
  var combo = '<select _varid='+targetId+' onchange="'+cbTypeChanged+'(event);" class="select-css">';
  for (var i=0; i<types.length; i++) {
    if (!types[i].exec) {
      //var selected = types[i].id == targetType ? ' selected="selected"' : '';
      var selected = '';
      
      if (types[i].id == targetType) {
        selected = ' selected="selected"';
        color = types[i].color;
        type_id = types[i].id;
      }
      
      combo += '<option value="'+types[i].id+'" '+selected+'>'+types[i].name+'</option>';
      
    }
  }
  combo += '</select>';
  
    var htmlRow = `
       <td class="tdVars" valign="top">
          <i id="i_`+targetId+`" class="icon `+getTypeIcon(type_id)+`" style="color:`+color+`; padding-right:4px"></i>
       </td>
       <td class="tdVars">
          <input id="input_`+targetId+`" class="inputConnector" _varid=`+targetId+` style="width:12em; margin-bottom:3px;" value="`+targetName+`" name="varName" onfocus="`+cbBeginRename+`(event)" onblur="`+cbEndRename+`(event);"  ondragstart="return false;" ondrop="return false;">
        
        <br>
        `+combo+`
        `+initialize+`
       </td>
       <td class="tdVars" valign="top">
         <i class="icon i-times w3-text-gray w3-hover-text-red" onclick="`+cbDelete+`('`+targetId+`');" style="cursor:pointer;" title="Delete"></i>
       </td>
     `;
     
    tr.innerHTML = htmlRow;
    table.appendChild(tr);
    
    var inElem = document.getElementById('input_'+targetId);
    setInputFilter(inElem, function(value) { return /^[a-zA-Z0-9_-]*$/.test(value); });
   
    /* Variables can be dragged to into blueprint */
    if (draggable) {
      tr.classList.add ('trVars');
      tr.title = 'Drag into blueprint';
      //var v = document.getElementById('i_'+targetId);
      tr.ondragstart = dragVariable;
      tr.draggable = true;
      target.element = tr;
    }
    
    setStatus (BPEditStatus.MODIFIED);
    cbModified ();
}

/* addInputCallback() - Triggered by button */
function addInputCallback() 
{
  var connector = blueprint.addInput (BPTypeID.INTEGER, 'Input', null);
  appAddVariable(AppParameterType.INPUT, connector);
}

/* addOutputCallback() - Triggered by button */
function addOutputCallback() 
{
  var connector = blueprint.addOutput (BPTypeID.INTEGER, 'Output', 0);
  appAddVariable(AppParameterType.OUTPUT, connector);
}

/* addVariableCallback() - Triggered by button */
function addVariableCallback() 
{
  var v = blueprint.addNewVariable ();
  appAddVariable (AppParameterType.VARIABLE, v);
}

/* deleteVariableCallback() - Triggered by button */
function deleteVariableCallback(id) 
{
  var vElem = document.getElementById('row_'+id);
  var v = blueprint.getVariable (id);

  if (v.referenced) {
    alert("You cannot delete a referenced variable.");
    return;
  }
  
  blueprint.deleteVariable (v);
  vElem.parentElement.removeChild(vElem);
  setStatus (BPEditStatus.MODIFIED);
}

function appClearVariables ()
{
  var table = document.getElementById("tab_input");
  table.innerHTML = "";
  
  var table = document.getElementById("tab_output");
  table.innerHTML = "";
  
  var table = document.getElementById("tab_variables");
  table.innerHTML = "";
}


