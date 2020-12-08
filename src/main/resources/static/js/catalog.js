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

/*
const MenuItems = {
 ADD_FOLDER:    0,
 DELETE_FOLDER: 1
};

function menuCallback(id, data) {
  switch (id) {
    case MenuItems.ADD_FOLDER:
      createFolder(data);
      break;

    case MenuItems.DELETE_FOLDER:
      deleteFolder(data);
      break;

    default:
      break;
  }
}
*/

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
          {'icon': '<i class="icon i-archive"></i>', 'item': 'Create JAR',  action: () => createJAR(id) },
          {'icon': '<i class="icon i-project-diagram"></i>', 'item': 'Import blueprint file',  action: () => importBlueprint(id) },
          {'icon': '<i class="icon i-sliders-h"></i>', 'item': 'Edit properties',  action: () => window.location = '/program/'+id+'/edit-properties' },
          {'icon': '<i class="icon i-edit"></i>',   'item': 'Rename',  action: () => renameProgram(id, name) },
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

function getBlueprintMenuHTML(jo) {
  var marginLeft = '0.5em';
  var html = '<span id="bpmenu_'+jo.id+'" style="margin-left:2em;visibility:hidden;">';
  
  if (jo.type == 'GENERIC') {
    html += `<i class="icon i-clone w3-text-gray" style="margin-left:`+marginLeft+`; cursor:pointer;" onclick="cloneBlueprint('`+jo.id+`', '`+jo.name+`')" title="Clone blueprint"></i>`;
    html += `<i class="icon i-trash-alt w3-text-gray" style="margin-left:`+marginLeft+`; cursor:pointer;" onclick="deleteBlueprint('`+jo.id+`', '`+jo.name+`')" title="Delete blueprint"></i>`;
  }
  
  html += '</span>';
  
  return(html);
}

function addBlueprints (programId, containerElem, jblueprints) {
  var jo;

  var listElem =  document.createElement('div');
  listElem.classList.add('w3-panel');
  containerElem.appendChild(listElem);

  for (var i=0; i<jblueprints.length; i++) {
    jo = jblueprints[i];

    var bpElem = document.createElement('div');
    bpElem.classList.add('w3-container');
    bpElem.setAttribute('id', jo.id);

    //var delButton = jo.type == 'GENERIC' ? `<i id="del_`+jo.id+`" class="icon i-trash-alt w3-text-gray" style="margin-left:2em; cursor:pointer; visibility:hidden" onclick="deleteBlueprint('`+jo.id+`', '`+jo.name+`')" title="Delete blueprint"></i>` : '<div id="del_'+jo.id+'"></div>';

    bpElem.innerHTML = `<i class="icon i-project-diagram w3-text-blue-gray" style="width:1.5em;"></i> <a href="/blueprint/`+jo.id+`/edit" target="_blank" title="Open in editor">`+jo.name+`</a> `+getBlueprintMenuHTML(jo);

    bpElem.onmouseenter = function(ev) {
      var t = ev.currentTarget;
      var id = t.getAttribute('id');
      document.getElementById('bpmenu_'+id).style.visibility="visible";
    }

    bpElem.onmouseleave = function(ev) {
      var t = ev.currentTarget;
      var id = t.getAttribute('id');
      document.getElementById('bpmenu_'+id).style.visibility="hidden";
    }


    listElem.appendChild(bpElem);
  }

  var addElem = document.createElement('div');
  addElem.classList.add('w3-container');
  addElem.innerHTML = `<button class="btnApp" onclick="addBlueprint('`+programId+`');" style="margin-left:1.5em;"><i class="icon i-add"></i> New blueprint</button>`;
  listElem.appendChild(addElem);
}

function createProgramHeader(jo) {
  var status;

    var headerElem= document.createElement('div');
    headerElem.classList.add('w3-row');
    headerElem.classList.add('w3-padding-small');

    var col = document.createElement('div');
    col.classList.add('w3-col');
    col.classList.add('s3');

        var caret = '<span id="caret_'+jo.id+'" onclick="expandItem(\''+jo.id+'\')">' + (expanded[jo.id] ? '<i class="icon i-caret-down"></i>' : '<i class="icon i-caret-right"></i>') + '</span>';
        //icon = "i-cog w3-text-blue-gray";

        switch (jo.status) {
          case 'READY':
            status = '<i class="icon i-clock w3-text-green"></i> Ready';
            break;

          case 'COMPILED':
            status = '<i class="icon i-check w3-text-green"></i> Compiled';
            break;

          case 'ERRORS':
            status = '<i class="icon i-exclamation-triangle w3-text-red"></i> Errors';
            break;

          default:
            status = '<i class="icon i-question w3-text-orange"></i> Unknown';
            break;
        }

    //col.innerHTML = caret+' <i class="icon '+icon+' "></i> '+jo.name;
    col.innerHTML = caret+' <b onclick="expandItem(\''+jo.id+'\')">'+jo.name+'</b>';
    col.style.cursor = 'pointer';
    //col.style.paddingLeft = (level+padding)+'em';

    //col.oncontextmenu = createContextMenu;

    headerElem.appendChild(col);

    // Status
    col = document.createElement('div');
    col.classList.add('w3-col');
    col.classList.add('s3');
    col.innerHTML = status;
    headerElem.appendChild(col);

    // Modified
    col = document.createElement('div');
    col.classList.add('w3-col');
    col.classList.add('s3');
    col.innerHTML = secondsToString(Date.parse(jo.updateTime) / 1000);
    headerElem.appendChild(col);

    // Actions
      col = document.createElement('div');
      col.classList.add('w3-col');
      col.classList.add('s2');
      if (jo.jar)
        col.innerHTML = `<a target="Javascript:void(0);" onclick="downloadJAR ('`+jo.id+`')" style="cursor:pointer;" title="Download JAR"><i class="icon i-download" style="color:gray;"></i></a>`;
      else
        col.innerHTML = '<i class="icon i-download" style="color:lightgray;"></i>';
      headerElem.appendChild(col);

    // Program menu button
    col = document.createElement('div');
    col.classList.add('w3-col');
    col.classList.add('s1');
    col.innerHTML = `<div id="`+jo.id+`" name="`+jo.name+`" class="w3-right" onclick="createProgramMenu(event);" style="cursor:pointer;"><i class="icon ellipsis-h" style="color:grey;"></i></div>`;
    headerElem.appendChild(col);

    return(headerElem);
}

function addObjects (containerElem, jtree) {
  var tr, progElem, row, col, caret, icon, padding = 0;
  var status = '&nbsp;';

  for (var i=0; i<jtree.length; i++) {

    var jo = jtree[i];
    var key = jo.id;

    if (!expanded.hasOwnProperty(key))
      expanded.key = false;

    //console.log(jo);

    progElem = document.createElement('div');
    progElem.classList.add('program');
    progElem.classList.add('w3-container');
    progElem.classList.add('w3-round');
    progElem.classList.add('w3-border');
    progElem.classList.add('w3-margin-top');
/*
    progElem.setAttribute('id', key);
    //progElem.setAttribute('tag', jo.tag);
    progElem.setAttribute('name', jo.name);
    //progElem.oncontextmenu = createProgramMenu;
    progElem.onclick = createProgramMenu;
*/
    progElem.appendChild(createProgramHeader(jo));

    /*
    progElem.ondragover = function(e) {
      e.preventDefault();
      this.classList.add('catalog-selected');
    }

    progElem.ondragleave = function(e) {
      e.preventDefault();
      this.classList.remove('catalog-selected');
    }

    progElem.ondrop = function(e) {
      //e.preventDefault();
      var folderId = e.dataTransfer.getData("id");
      var parentId = this.getAttribute('id');

      moveFolder (folderId, parentId);
    }

    progElem.ondragstart = dragFolder;
    progElem.draggable = true;
    */

    containerElem.appendChild(progElem);

    // Container element for children

    var childrenContainer = document.createElement('div');
    childrenContainer.classList.add('w3-container');
    childrenContainer.classList.add('w3-stretch');
    childrenContainer.classList.add('w3-border-top');
    childrenContainer.setAttribute('id', 'container_'+key);
    childrenContainer.style.display = expanded[key] ? 'block' : 'none';
    progElem.appendChild(childrenContainer);

    addBlueprints(key, childrenContainer, jo.blueprints);
  }
}

function refreshTable () {
  console.log("Refreshing table...");
  document.getElementById("catalog").innerHTML = "";
  addObjects (document.getElementById("catalog"), jsonResponse);
}

function refreshCatalog () {
  //callServer ("GET", "/catalog", null, function (xhttp) {
  callServer ("GET", "/programs", null, function (xhttp) {
      if (xhttp.readyState == 4) {
        if (xhttp.status == 200) {
          jsonResponse = JSON.parse(xhttp.responseText);
          //console.log(jsonResponse);
          refreshTable ();
        }
        else {
          document.getElementById('catalog').innerHTML = xhttp.statusText;
        }
      }
      else if (xhttp.readyState == 3) { // Processing request
        document.getElementById('catalog').innerHTML = "Getting blueprints...";
      }
      else {
        document.getElementById('catalog').innerHTML = xhttp.statusText;
      }
    }
  );
}

function myCallback (xhttp) {

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
      console.error(err.message);
    }

    if (xhttp.status == 200) {

      if (jresponse) {
        if (jresponse.hasOwnProperty('message')) {
          showSnacknar(BPResult.SUCCESS, message, 2000);
        }
        else {
          showSnacknar(BPResult.SUCCESS, "Success", 2000);

          // Update catalog tree
          jsonResponse = jresponse;
          refreshTable ();
        }
      }
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
  }
  else {
    //showSnacknar(BPResult.ERROR, "Can't delete package");
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
      callServer ("PUT", '/folder/'+folderId+'/rename/'+data.name+'?tree=1', null, myCallback);
    }
  );
}

function createFolder (parentId) {
  editFolderDialog ('Create folder', 'New folder', function (data) {
      var resource = parentId == null ? "/folder" : "/folder/parent/"+parentId;
      dialogWorking = dialogMessage ('Working', 'Creating folder...', DialogButtons.NONE, DialogIcon.RUNNING, null);
      callServer ("PUT", resource, JSON.stringify(data), myCallback);
    }
  );
}

function moveFolder (id, parentId) {
  dialogWorking = dialogMessage ('Working', 'Moving folder...', DialogButtons.NONE, DialogIcon.RUNNING, null);
  callServer ("PUT", "/folder/"+id+"/move/"+parentId+"?tree=1", null, myCallback);
}

function deleteFolder (id, name) {
  dialogMessage ("Confirm", "Delete folder "+name+"?", DialogButtons.YES_NO, DialogIcon.QUESTION, function (dialog) {
      dialog.destroy();
      dialogWorking = dialogMessage ('Working', 'Deleting folder...', DialogButtons.NONE, DialogIcon.RUNNING, null);
      callServer ("DELETE", "/folder/"+id, null, myCallback);
    }
  );
}

function createProgram (parentId) {
  editFolderDialog ('Create program', 'New program', function (data) {
      var resource = parentId == null ? "/program" : "/program/parent/"+parentId;
      dialogWorking = dialogMessage ('Working', 'Creating program...', DialogButtons.NONE, DialogIcon.RUNNING, null);
      callServer ("PUT", resource, JSON.stringify(data), myCallback);
    }
  );
}

function cloneProgram (programId) {
  dialogWorking = dialogMessage ('Working', 'Cloning program...', DialogButtons.NONE, DialogIcon.RUNNING, null);
  callServer ("POST", '/program/'+programId+'/clone?tree=1', null, myCallback);
}

function createJAR (programId) {
  dialogWorking = dialogMessage ('Working', 'Creating JAR with all dependencies.<br>This will take a while...', DialogButtons.NONE, DialogIcon.RUNNING, null);
  callServer ("POST", '/program/'+programId+'/jar', null, myCallback);
}

function downloadJAR (programId) {
  console.log("Downloading JAR...");
  document.getElementById('my_iframe').src = '/program/'+programId+'/jar';
}

function renameProgram (programId, programName) {
  editFolderDialog ('Rename program', programName, function (data) {
      dialogWorking = dialogMessage ('Working', 'Renaming program...', DialogButtons.NONE, DialogIcon.RUNNING, null);
      callServer ("PUT", '/program/'+programId+'/rename/'+data.name+'?tree=1', null, myCallback);
    }
  );
}

function deleteProgram (id, name) {
  dialogMessage ("Confirm", "Delete program "+name+"?", DialogButtons.YES_NO, DialogIcon.QUESTION, function (dialog) {
      dialog.destroy();
      dialogWorking = dialogMessage ('Working', 'Deleting program...', DialogButtons.NONE, DialogIcon.RUNNING, null);
      callServer ("DELETE", "/program/"+id, null, myCallback);
    }
  );
}

/*
function cleanProgram (programId) {
  dialogWorking = dialogMessage ('Working', 'Cleaning...', DialogButtons.NONE, DialogIcon.RUNNING, null);
  callServer ("PUT", '/program/'+programId+'/clean', null, myCallback);
}*/

function addBlueprintCallback(xhttp) {

  if (xhttp.readyState == 4) {
    var jresponse = null;

    if (dialogWorking)
      dialogWorking.destroy();

    //console.log(xhttp.responseText);

    jresponse = JSON.parse(xhttp.responseText);

    if (xhttp.status == 200) {
      window.open("/blueprint/"+jresponse.id+"/edit");
      refreshCatalog();
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
      callServer ("PUT", resource, null, addBlueprintCallback);
    }
  );
}

function deleteBlueprint (id, name) {
  dialogMessage ("Confirm", "Delete blueprint "+name+"?", DialogButtons.YES_NO, DialogIcon.QUESTION, function (dialog) {
      dialog.destroy();
      dialogWorking = dialogMessage ('Working', 'Deleting blueprint...', DialogButtons.NONE, DialogIcon.RUNNING, null);
      callServer ("DELETE", "/blueprint/"+id+"?tree=1", null, myCallback);
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
      callServer ("PUT", '/program/'+programId+'/import/blueprint?tree=1', contents, myCallback);
  
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
  callServer ("PUT", "/blueprint/"+id+"/clone?tree=1", null, myCallback);
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
      //callServer ("POST", '/program/import?tree=1', contents, myCallback);
      var xhttp = new XMLHttpRequest();
      
      xhttp.onreadystatechange = function() { myCallback(this); }
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
/*
  var dialog = new Dialog ();

  // Callback functions

  dialog.callbackCancel = function (dialog) { dialog.destroy(); };
  dialog.callbackOK = function (dlg) {
    var content = dlg.getContentElement ();
    console.log("Uploading...");
  }

  // Build dialog 

  dialog.create("Import program");

  var content = dialog.getContentElement ();

  content.innerHTML = `
    <form method="POST" action="/program/import" enctype="multipart/form-data">
        <input class="w3-input w3-border" type="file" name="file" /><br/><br/>
        <input class="btnApp" type="submit" value="Import" />
    </form>
  `;
*/
}

