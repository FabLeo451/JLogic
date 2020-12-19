'use strict';

var jsonResponse;
var header = ['Name'];
var expanded = {};
var contextMenu = null;
var level = 0;
//var menu = null;
var contextMenu = null;
var dialogWorking = null;

const ItemFlags = {
 NONE:    0,
 LAST:    1
};

const ItemType = {
 FOLDER:    0,
 PROGRAM:   1,
 BLUEPRINT: 2
};

const ProgramStatus = {
  READY: 0,
  COMPILED: 1,
  ERRORS: 2
};

function dragFolder(ev) {
  ev.dataTransfer.setData("id", ev.target.id);
  //console.log(ev.target.id.replace ('bp_', ''));
}

function expandItem(id) {
  console.log ('Expanding container_'+id+'...');
  var x = document.getElementById('container_'+id);
  expanded[id] = !expanded[id];
  document.getElementById('caret_'+id).innerHTML = expanded[id] ? '<i class="icon i-caret-down"></i>' : '<i class="icon i-caret-right"></i>';
  document.getElementById('container_'+id).style.display = expanded[id] ? 'block' : 'none';
}

function createProgramMenu(event) {
  event.preventDefault();
  var t = event.currentTarget;
  var id = t.getAttribute('id');
  var name = t.getAttribute('name');

  console.log(id+" "+name);

  var jmenu =
    {
        'items': [
          //{'icon': '<i class="icon i-project-diagram"></i>', 'item':'Add blueprint',  action: () => addBlueprint(id) },
          {'icon': '<i class="icon i-clone"></i>', 'item': 'Clone',  action: () => cloneProgram(id) },
          //{'icon': '<i class="icon i-archive"></i>', 'item': 'Create JAR',  action: () => createJAR(id) },
          //{'icon': '<i class="icon i-project-diagram"></i>', 'item': 'Import blueprint file',  action: () => importBlueprint(id) },
          {'icon': '<i class="icon i-sliders-h"></i>', 'item': 'Edit properties',  action: () => window.location = '/program/'+id+'/edit-properties' },
          //{'icon': '<i class="icon i-edit"></i>',   'item': 'Rename',  action: () => renameProgram_deprecated(id, name) },
          {'icon': '<i class="icon i-download"></i>',   'item': 'Export',  action: () => exportProgram(id) },
          {'item': 'separator' },
          {'icon': '<i class="icon i-trash-alt"></i>',  'item': 'Delete',  action: () => deleteProgram(id, name) }
        ]
    };

  if (contextMenu) {
    hideContextMenu(event);
  }

  contextMenu = new ContextMenu();

  contextMenu.enableSearch (false);
  contextMenu.createFromJson(jmenu);
  contextMenu.setWidthAuto();
  //contextMenu.setCallback(bpGetSet);
  //contextMenu.show(event.x, event.y);
  contextMenu.show(event.pageX, event.pageY);
  setTimeout(function() { document.addEventListener('click', hideContextMenu, false) }, 100);
}

function hideContextMenu(evt) {
    if (contextMenu) {
      contextMenu.remove();
      contextMenu = null;
    }

    document.removeEventListener('click', hideContextMenu);
}

/**
 * Refresh an element in Edit program page
 */
function refreshProgramData(id) {
    console.log("Refreshing "+id+"...");
    
    var programId = document.getElementById("programId").value;

    $.get("/program/"+programId+"/edit?element="+id).done(function(fragment) { // get from controller
        $("#"+id).replaceWith(fragment); // update snippet of page
    });
}

function refreshPrograms() {
    //console.log("Refreshing Catalog...");

    $.get("/view-programs?element=programs").done(function(fragment) { // get from controller
        $("#programs").replaceWith(fragment); // update snippet of page
    });
}

function refreshBlueprints() {
    console.log("Refreshing blueprints...");

    var programId = document.getElementById("programId").value;
    
    $.get("/program/"+programId+"/edit?element=blueprints").done(function(fragment) {
        $("#blueprints").replaceWith(fragment);
    });
}

function processResponse(xhttp) {

  if (xhttp.readyState == 4) {
    if (dialogWorking)
      dialogWorking.destroy();

    var jresponse = null, status, message, output;

    //console.log(xhttp.responseText);

    try {
      jresponse = JSON.parse(xhttp.responseText);

      if (jresponse.hasOwnProperty('status') && jresponse.hasOwnProperty('message')) {
        status = jresponse.status;
        message = jresponse.message;
        output = jresponse.output;
      }
    }
    catch (err) {
      //console.error(err.message);
    }

    if (xhttp.status == 200) {

        if (jresponse) {
            if (jresponse.hasOwnProperty('message')) {
              showSnacknar(BPResult.SUCCESS, message, 2000);
            }
            else {
              showSnacknar(BPResult.SUCCESS, "Success", 2000);

              // Update catalog tree
              //jsonResponse = jresponse;
              //refreshBlueprints();
            }
        }
      
        return true;
    }
    else {
      if (jresponse) {
        var jresponse = JSON.parse(xhttp.responseText);

        if (jresponse.hasOwnProperty('message')) {
          dialogError (message+'\n'+output);
        }
        else {
        }
      }
      else
        showSnacknar(BPResult.ERROR, 'Failed: '+xhttp.status, 2000);
    }
    
    return false;
  }
  else {
    //showSnacknar(BPResult.ERROR, "Can't delete package");
  }
}

function catalogCallback(xhttp) {
    if (xhttp.readyState == 4) {
        if (processResponse(xhttp))
            refreshPrograms();
    }
}

function blueprintCallback(xhttp) {
    if (xhttp.readyState == 4) {
        if (processResponse(xhttp))
            refreshBlueprints();
    }
}

function editFolderDialog (title, folderName, cbFun) {
  var dialog = new Dialog ();

  /* Callback functions */

  dialog.callbackOK = function (dlg) {
    var content = dlg.getContentElement ();

    var folderName = document.getElementById("folderName").value.trim();
    var resultElem = document.getElementById("result");

    if (!folderName) {
      resultElem.innerHTML = '<div class="w3-text-red">Missing folder name</div>';
      return;
    }

    var data = {};
    data.name = folderName;

    dlg.destroy();
    cbFun(data);
  }

  dialog.callbackCancel = function (dialog) { dialog.destroy(); };

  /* Build dialog */

  dialog.create(title);

  var content = dialog.getContentElement ();

  content.innerHTML = `
    <div class="w3-panel w3-padding-small">
        <label><b>Name</b></label>
        <input id="folderName" class="w3-input w3-border" type="text" placeholder="Name" value="`+folderName+`">
    </div>
    <div id="result" class="w3-panel">&nbsp;</div>
  `;
}

function renameFolder (folderId, folderName) {
  editFolderDialog ('Rename folder', folderName, function (data) {
      dialogWorking = dialogMessage ('Working', 'Renaming folder...', DialogButtons.NONE, DialogIcon.RUNNING, null);
      callServer ("PUT", '/folder/'+folderId+'/rename/'+data.name+'?tree=1', null, blueprintCallback);
    }
  );
}

function createFolder (parentId) {
  editFolderDialog ('Create folder', 'New folder', function (data) {
      var resource = parentId == null ? "/folder" : "/folder/parent/"+parentId;
      dialogWorking = dialogMessage ('Working', 'Creating folder...', DialogButtons.NONE, DialogIcon.RUNNING, null);
      callServer ("PUT", resource, JSON.stringify(data), blueprintCallback);
    }
  );
}

function moveFolder (id, parentId) {
  dialogWorking = dialogMessage ('Working', 'Moving folder...', DialogButtons.NONE, DialogIcon.RUNNING, null);
  callServer ("PUT", "/folder/"+id+"/move/"+parentId+"?tree=1", null, blueprintCallback);
}

function deleteFolder (id, name) {
  dialogMessage ("Confirm", "Delete folder "+name+"?", DialogButtons.YES_NO, DialogIcon.QUESTION, function (dialog) {
      dialog.destroy();
      dialogWorking = dialogMessage ('Working', 'Deleting folder...', DialogButtons.NONE, DialogIcon.RUNNING, null);
      callServer ("DELETE", "/folder/"+id, null, blueprintCallback);
    }
  );
}

function createProgram (parentId) {
  editFolderDialog ('Create program', 'New program', function (data) {
      var resource = parentId == null ? "/program" : "/program/parent/"+parentId;
      dialogWorking = dialogMessage ('Working', 'Creating program...', DialogButtons.NONE, DialogIcon.RUNNING, null);
      callServer ("PUT", resource, JSON.stringify(data), blueprintCallback);
    }
  );
}

function cloneProgram (programId) {
  dialogWorking = dialogMessage ('Working', 'Cloning program...', DialogButtons.NONE, DialogIcon.RUNNING, null);
  callServer ("POST", '/program/'+programId+'/clone?tree=1', null, catalogCallback);
}

function createJAR (programId) {
  dialogWorking = dialogMessage ('Working', 'Creating JAR with all dependencies.<br>This will take a while...', DialogButtons.NONE, DialogIcon.RUNNING, null);
  callServer ("POST", '/program/'+programId+'/jar', null, function(xhttp) {
        if (xhttp.readyState == 4) {
            if (processResponse(xhttp))
                refreshProgramData("programStatus");
        }
    }
  );
}

function downloadJAR (programId) {
  console.log("Downloading JAR...");
  document.getElementById('my_iframe').src = '/program/'+programId+'/jar';
}
/*
function renameProgram_deprecated (programId, programName) {
  editFolderDialog ('Rename program', programName, function (data) {
      dialogWorking = dialogMessage ('Working', 'Renaming program...', DialogButtons.NONE, DialogIcon.RUNNING, null);
      callServer ("PUT", '/program/'+programId+'/rename/'+data.name+'?tree=1', null, blueprintCallback);
    }
  );
}
*/
function renameProgram() {
    var name = document.getElementById("programName").value;
    var programId = document.getElementById("programId").value;
    
    if (name == "")
        return;
        
    dialogWorking = dialogMessage ('Working', 'Renaming program...', DialogButtons.NONE, DialogIcon.RUNNING, null);
    callServer ("PUT", '/program/'+programId+'/rename/'+name, null, function (xhttp) {
          if (xhttp.readyState == 4) {
            dialogWorking.destroy();
            
            if (xhttp.status == 200) {
              resetNameCtrl();
            }
            else {
              dialogError (xhttp.statusText);
            }
          }
        }
    );
}

function nameChanged() {
    document.getElementById("renameCtrl").style.visibility = "visible";
    document.getElementById("programName").style.backgroundColor = "yellow";
}

function resetNameCtrl() {
    document.getElementById("renameCtrl").style.visibility = "hidden";
    document.getElementById("programName").style.backgroundColor = "white";
}

function revertName(name) {
    document.getElementById("programName").value = name;
    resetNameCtrl();
}

function deleteProgram (id, name) {
  dialogMessage ("Confirm", "Delete program "+name+"?", DialogButtons.YES_NO, DialogIcon.QUESTION, function (dialog) {
      dialog.destroy();
      dialogWorking = dialogMessage ('Working', 'Deleting program...', DialogButtons.NONE, DialogIcon.RUNNING, null);
      callServer ("DELETE", "/program/"+id, null, catalogCallback);
    }
  );
}

function addBlueprint_Callback(xhttp) {

  if (xhttp.readyState == 4) {
    var jresponse = null;

    if (dialogWorking)
      dialogWorking.destroy();

    //console.log(xhttp.responseText);

    jresponse = JSON.parse(xhttp.responseText);

    if (xhttp.status == 200) {
      window.open("/blueprint/"+jresponse.id+"/edit");
      refreshBlueprints();
    }
    else {
        var jresponse = JSON.parse(xhttp.responseText);

        if (jresponse.hasOwnProperty('message')) {
          dialogError (message+'\n'+output);
        }
        else {
          dialogError (xhttp.responseText);
        }
    }
  }
}

function addBlueprint (programId) {
  editFolderDialog ('Add blueprint', 'New blueprint', function (data) {
      var resource = "/program/"+programId+"/blueprint/"+data.name+"?tree=1";
      dialogWorking = dialogMessage ('Working', 'Adding blueprint...', DialogButtons.NONE, DialogIcon.RUNNING, null);
      callServer ("PUT", resource, null, addBlueprint_Callback);
    }
  );
}

function deleteBlueprint(id, name) {
  dialogMessage ("Confirm", "Delete blueprint "+name+"?", DialogButtons.YES_NO, DialogIcon.QUESTION, function (dialog) {
      dialog.destroy();
      dialogWorking = dialogMessage ('Working', 'Deleting blueprint...', DialogButtons.NONE, DialogIcon.RUNNING, null);
      callServer ("DELETE", "/blueprint/"+id+"?tree=1", null, blueprintCallback);
    }
  );
}

function importBlueprint (programId) {
  var input = document.getElementById('file-dialog');
  
  input.onchange = function(e) {
    var file = e.target.files[0];
    //console.log("file = "+e.target.value);  
    
    if (!file)
      return;
    
    var reader = new FileReader();

    reader.onload = function(e) {
      var contents = e.target.result;
      
      dialogWorking = dialogMessage ('Working', 'Importing ...', DialogButtons.NONE, DialogIcon.RUNNING, null);
      callServer ("PUT", '/program/'+programId+'/import/blueprint?tree=1', contents, blueprintCallback);
  
      /* Reset so 'changed' event triggers again */
      document.getElementById('file-dialog').value="";
    };

    reader.readAsText(file);
  }
  
  input.click();
}

/**
 * Clone blueprint
 */
function cloneBlueprint (id, name) {
  dialogWorking = dialogMessage ('Working', 'Cloning blueprint '+name+'...', DialogButtons.NONE, DialogIcon.RUNNING, null);
  callServer ("PUT", "/blueprint/"+id+"/clone", null, blueprintCallback);
}

/**
 * Export program
 */
function exportProgram (programId) {
  console.log("Exporting...");
  document.getElementById('my_iframe').src = '/program/'+programId+'/export';
}

/**
 * Import program
 */
function importProgram () {
  var input = document.getElementById('file-dialog');
  
  input.onchange = function(e) {
    var file = e.target.files[0];
    //console.log("file = "+e.target.value);  
    
    if (!file)
      return;
    
    var reader = new FileReader();
    //var fileByteArray = [];
    

    reader.onload = function(e) {
      var contents = e.target.result;
      
      dialogWorking = dialogMessage ('Working', 'Importing program...', DialogButtons.NONE, DialogIcon.RUNNING, null);
      //callServer ("POST", '/program/import?tree=1', contents, blueprintCallback);
      var xhttp = new XMLHttpRequest();
      
      xhttp.onreadystatechange = function() { catalogCallback(this); }
      xhttp.open("POST", '/program/import?tree=1', true);
      xhttp.setRequestHeader ("Content-Type", "application/octet-stream");
      //xhttp.setRequestHeader ('Client', detectBrowser()/*+'/'+navigator.appVersion*/+' ('+detectPlatform()+')');   
      
      xhttp.send(contents);
        
      /* Reset so 'changed' event triggers again */
      document.getElementById('file-dialog').value="";
    };

    //reader.readAsText(file);
    reader.readAsArrayBuffer(file);
  }
  
  input.click();
}

