
var jsonResponse;  
var header = ['Name'];
var expanded = {};
var contextMenu = null;
var level = 0;
var menu = null;
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
/*
  var i;
  
  expanded[id] = !expanded[id];
  
  for (i = 0; i < x.length; i++) {
    var pkg_id = x[i].getAttribute('parent_id');
    if (pkg_id == id)
      x[i].style.display = expanded[id] ? 'table-row' : 'none';
  }
  
  document.getElementById('caret_'+id).innerHTML = expanded[id] ? '<i class="icon i-caret-down"></i>' : '<i class="icon i-caret-right"></i>';
*/
}

function createContextMenu(event) {
  event.preventDefault();
  var t = event.currentTarget;
  //var t = event.target;
  var id = t.getAttribute('id');
  var type = parseInt(t.getAttribute('type'));
  var name = t.getAttribute('name');
  
  console.log(type);
  
  switch (type) {
    case ItemType.FOLDER:
      jmenu =
        {
          'theme': 'default',
            'items': [
              {'icon': 'i-folder', 'name': 'Add folder',  action: () => createFolder(id) },
              {'icon': 'i-edit',   'name': 'Rename',  action: () => renameFolder(id, name) },
              {'icon': 'i-arrow-up',  'name': 'Unparent',  action: () => moveFolder(id, "_root") },
              {'name': 'separator' },
              {'icon': 'i-trash-alt',  'name': 'Delete',  action: () => deleteFolder(id, name) }
            ]
        }
      break;
      
    case ItemType.PROGRAM:
      jmenu =
        {
          'theme': 'default',
            'items': [
              {'icon': 'i-project-diagram', 'name': 'Add blueprint',  action: () => addBlueprint(id) },
              {'icon': 'i-archive', 'name': 'Create JAR',  action: () => createJAR(id) },
              {'icon': 'i-sliders-h', 'name': 'Edit properties',  action: () => window.location = '/program/'+id+'/edit-properties' },
              {'icon': 'i-edit',   'name': 'Rename',  action: () => renameProgram(id, name) },
              //{'icon': 'i-broom',   'name': 'Clean',  action: () => cleanProgram(id) },
              {'name': 'separator' },
              /*{'icon': 'i-arrow-up',  'name': 'Unparent',  action: () => moveFolder(id, "_root") },
              {'name': 'separator' },*/
              {'icon': 'i-trash-alt',  'name': 'Delete',  action: () => deleteProgram(id, name) }
            ]
        }
      break;
      
    case ItemType.BLUEPRINT:
      jmenu =
        {
          'theme': 'default',
            'items': [
              {'icon': 'i-edit',   'name': 'Edit',  action: () => window.open("/blueprint/"+id+"/edit") },
              {'name': 'separator' },
              {'icon': 'i-trash-alt',  'name': 'Delete',  action: () => deleteBlueprint(id, name) }
            ]
        }
      break;
      
    default:
      break;
  }
  
  if (menu) {
    menu.destroy();
    menu = null;
  }
  
  menu = new ContextMenu(jmenu);
  const time = menu.isOpen() ? 100 : 0;

  //menu.destroy();
  setTimeout(() => { menu.show(event.pageX, event.pageY) }, time);
  document.addEventListener('click', hideContextMenu, false);
}

function hideContextMenu(evt){
    menu.destroy();
    menu = null;
    document.removeEventListener('click', hideContextMenu);
}

function addObjects (containerElem, jtree) {
  var tr, row, col, caret, icon, jmenu, padding = 0;
  var status = '&nbsp;';
  
  for (key in jtree) {
    if (key == "_index")
      continue;
      
    jo = jtree[key];
    
    if (!expanded.hasOwnProperty(key))
      expanded.key = false;
    
    console.log(jo);

    row = document.createElement('div');
    row.classList.add('w3-row');
    row.classList.add('catalog-row');

    col = document.createElement('div');
    col.classList.add('w3-col');
    col.classList.add('s2');
    //col.classList.add('w3-hover-text-blue');
    /*
    col.setAttribute('id', key);
    col.setAttribute('type', jo.type);
    col.setAttribute('name', jo.name);
    
    col.ondragover = function(e) {
      e.preventDefault();
      this.classList.add('tr1-selected');
    }
      
    col.ondragleave = function(e) {
      e.preventDefault();
      this.classList.remove('tr1-selected');
    }

    col.ondrop = function(e) {
      //e.preventDefault();
      var folderId = e.dataTransfer.getData("id");
      var parentId = this.getAttribute('id');
      
      moveFolder (folderId, parentId);
    }
    
    col.ondragstart = dragFolder;
    col.draggable = true;
    */
      
    switch (jo.type) {
      case ItemType.FOLDER:
        caret = '<span id="caret_'+key+'" onclick="expandItem(\''+key+'\')">' + (expanded[key] ? '<i class="icon i-caret-down"></i>' : '<i class="icon i-caret-right"></i>') + '</span>';
        icon = "i-folder w3-text-orange";
        break;
        
      case ItemType.PROGRAM:
        caret = '<span id="caret_'+key+'" onclick="expandItem(\''+key+'\')">' + (expanded[key] ? '<i class="icon i-caret-down"></i>' : '<i class="icon i-caret-right"></i>') + '</span>';
        icon = "i-cog w3-text-blue-gray";

        switch (jo.status) {
          case 'READY':
            status = '<i class="icon i-check w3-text-green"></i> Ready';
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
        break;
        
      case ItemType.BLUEPRINT:
        caret = ' ';
        icon = "i-project-diagram w3-text-indigo";
        padding = 1;
        break;
        
      default:
        break;
    }

    col.innerHTML = caret+' <i class="icon '+icon+' "></i> '+jo.name;
    //col.innerHTML = '<div style="left:'+leftMargin+'"> '+caret+' <i class="icon '+icon+' "></i>'+jo.name+'</div>';
    col.style.cursor = 'pointer';
    col.style.paddingLeft = (level+padding)+'em';
    
    //col.oncontextmenu = createContextMenu;
    
    row.appendChild(col);
    
    // Status
    col = document.createElement('div');
    col.classList.add('w3-col');
    col.classList.add('s2');
    col.innerHTML = status;
    row.appendChild(col);
    
    // Modified
    col = document.createElement('div');
    col.classList.add('w3-col');
    col.classList.add('s2');
    col.innerHTML = secondsToString(jo.updateTime);
    row.appendChild(col);
    
    // Actions
    if (jo.type == ItemType.PROGRAM) {
      col = document.createElement('div');
      col.classList.add('w3-col');
      col.classList.add('s2');
      if (jo.jar)
        col.innerHTML = `<a target="Javascript:void(0);" onclick="downloadJAR ('`+key+`')" style="cursor:pointer;" title="Download JAR"><i class="icon i-download" style="color:grey;"></i></a>`;
      row.appendChild(col);
    }

    row.setAttribute('id', key);
    row.setAttribute('type', jo.type);
    row.setAttribute('name', jo.name);    
    row.oncontextmenu = createContextMenu;
    
    row.ondragover = function(e) {
      e.preventDefault();
      this.classList.add('catalog-selected');
    }
      
    row.ondragleave = function(e) {
      e.preventDefault();
      this.classList.remove('catalog-selected');
    }

    row.ondrop = function(e) {
      //e.preventDefault();
      var folderId = e.dataTransfer.getData("id");
      var parentId = this.getAttribute('id');
      
      moveFolder (folderId, parentId);
    }
    
    row.ondragstart = dragFolder;
    row.draggable = true;
     
    containerElem.appendChild(row);

    // Container element for children
    
    childrenContainer = document.createElement('div');
    childrenContainer.setAttribute('id', 'container_'+key);
    childrenContainer.style.display = expanded[key] ? 'block' : 'none';
    containerElem.appendChild(childrenContainer);

    level ++;
    addObjects (childrenContainer, jo.children);
    level --;
    
  }
}

function refreshTable () {
  document.getElementById("catalog").innerHTML = "";
  addObjects (document.getElementById("catalog"), jsonResponse);
}

function refreshCatalog () {
  callServer ("GET", "/catalog", null, function (xhttp) {
      if (xhttp.readyState == 4) {
        if (xhttp.status == 200) {
          jsonResponse = JSON.parse(xhttp.responseText);
          refreshTable ();
        }
        else {
          document.getElementById('catalog').innerHTML = xhttp.statusText;
        }
      }
      else if (this.readyState == 3) { // Processing request 
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

function addBlueprint (programId) {
  editFolderDialog ('Add blueprint', 'New blueprint', function (data) {
      var resource = "/program/"+programId+"/blueprint/"+data.name+"?tree=1";
      dialogWorking = dialogMessage ('Working', 'Adding blueprint...', DialogButtons.NONE, DialogIcon.RUNNING, null);
      callServer ("PUT", resource, null, myCallback);
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

