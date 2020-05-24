
var jsonResponse;  
var header = [null, 'Name', 'User', 'Modified'];
var expanded = {};
var contextMenu = null;

const ItemFlags = {
 NONE:    0,
 LAST:    1
};

function addDataToTable (table, idParent) {
  tr = document.createElement('tr');
  tr.classList.add('tr1');
  tr.setAttribute('parent_id', idParent);

  tr.innerHTML = '<td class="td1"></td><td class="td1"><span style="color:gray; margin-left:0.5em;">&boxur;</span> <i class="icon i-database w3-text-green"></i> <a href="/package/'+idParent+'/data"><b>Data</b></a></td><td class="td1"></td><td class="td1"></td><td class="td1"></td>';
  
  tr.oncontextmenu = function (event) {
    event.preventDefault();
    
    if (contextMenu)
      contextMenu.remove();
    
    contextMenu = new ContextMenu();
    contextMenu.init();
    
    var json =
      { 
        "items": [
           { "id": 9, "item": "Delete" }
        ] 
      }
    ;

    contextMenu.createFromJson(json);

    contextMenu.callback = function (id, data) {
      switch (id) {
        case 9:
          deletePackageDatafile (idParent);
          break;
          
        default:
          break;
      }
    }
    
    contextMenu.show(event.x, event.y);
  }
    
  tr.style.display = expanded[idParent] ? 'table-row' : 'none';
  
  table.appendChild(tr);
}

function addBlueprintToTable (table, key, row, idParent, flags) {

  //var margin = (idParent == '_root') ? '' : 'style="margin-left:1em;"';
  var decoration = (flags & ItemFlags.LAST) ? '&boxur;' : '&boxvr;';
  var childSing = (idParent == '_root') ? '' : '<span style="color:gray; margin-left:0.5em;">'+decoration+'</span> ';

  tr = document.createElement('tr');
  tr.classList.add('tr1');
    
  tr.ondragover = function(e) {
    e.preventDefault();
    this.classList.add('tr1-selected');
  }
    
  tr.ondragleave = function(e) {
    e.preventDefault();
    this.classList.remove('tr1-selected');
  }

  tr.setAttribute('id', 'bp_'+key);
  tr.setAttribute('parent_id', idParent);
  
  tr.ondragstart = dragBlueprint;
  tr.draggable = true;
  
  // Caret
  var td = document.createElement('td');
  td.classList.add('td1');
  td.classList.add('td1-1');
  tr.appendChild(td);
  
  // Name
  td = document.createElement('td');
  td.classList.add('td1');
  //td.innerHTML = '<i class="icon i-project-diagram w3-text-blue-gray" '+margin+'></i> <a href="/blueprint/'+key+'/edit" target="_blank" bp_id="'+key+'" ondragstart="dragBlueprint(event);"><b>'+row.name+'</b></a>';
  td.innerHTML = childSing+'<i class="icon i-project-diagram w3-text-blue"></i> <a href="/blueprint/'+key+'/edit" target="_blank" bp_id="'+key+'" ondragstart="dragBlueprint(event);"><b>'+row.name+'</b></a>';
  tr.appendChild(td);
  
  // Operations
  /*
  td = document.createElement('td');
  td.classList.add('td1');
  td.innerHTML = '<a href="/blueprint/'+key+'/data"><i class="icon i-database w3-text-blue-gray w3-hover-text-blue" onclick="editData ('+key+');" style="cursor:pointer;" title="Edit data"></i></a>';
  tr.appendChild(td);
  */

  /* User */
  td = document.createElement('td');
  td.classList.add('td1');
  td.innerHTML = row.hasOwnProperty('username') ? row.username : '';
  tr.appendChild(td);
 
  // Timestamp
  td = document.createElement('td');
  td.classList.add('td1');
  td.innerHTML = secondsToString(row.timestamp);
  tr.appendChild(td);
  
  /*
  td = document.createElement('td');
  td.classList.add('td1');
  td.align = "right";
  td.innerHTML = `<i class="icon i-times w3-text-gray w3-hover-text-red" onclick="deleteBlueprint ('`+key+`', '`+row.name+`');" style="cursor:pointer;"></i>`;
  tr.appendChild(td);
  */

  tr.ondrop = function(e) {
    //e.preventDefault();
    var bp_id = e.dataTransfer.getData("id");
    var parent_id = this.getAttribute('parent_id');
    
    moveBlueprint (bp_id, parent_id);
  }
  
  tr.oncontextmenu = function (event) {
    event.preventDefault();
    
    if (contextMenu) {
      contextMenu.remove();
    }
    
    contextMenu = new ContextMenu();
    contextMenu.init();
    
    var json =
      { 
        "items": [
           { "id": 9, "item": "Delete" }
        ] 
      }
    ;

    contextMenu.createFromJson(json);

    contextMenu.callback = function (id, data) {
      switch (id) {
        case 0:
          window.location = '/blueprint/'+key+'/data';
          break;
          
        case 9:
          deleteBlueprint (key, row.name);
          break;
          
        default:
          break;
      }
    }
    
    contextMenu.show(event.x, event.y);
  }
  
  if (idParent != '_root')
    tr.style.display = expanded[idParent] ? 'table-row' : 'none';
  //console.log (row.id+' '+idParent+' '+expanded[idParent]);

  table.appendChild(tr);
}

function addPackageToTable (table, tree, id, jpkg) {
  //console.log(row.name+' '+Object.keys(root[key]).length);
  
  if (!expanded.hasOwnProperty(id))
    expanded[id] = false;
    
  var caret = expanded[id] ? '<i class="icon i-caret-down"></i>' : '<i class="icon i-caret-right"></i>';

  tr = document.createElement('tr');
  tr.classList.add('tr1');
    
  tr.ondragover = function(e) {
    e.preventDefault();
    this.classList.add('tr1-selected');
  }
    
  tr.ondragleave = function(e) {
    e.preventDefault();
    this.classList.remove('tr1-selected');
  }
  
  tr.setAttribute('id', 'pkg_'+id);
  
  // Caret
  var td = document.createElement('td');
  td.classList.add('td1');
  td.classList.add('td1-1');
  td.innerHTML = '<span id="caret_'+id+'" onclick="expandPackage(\''+id+'\')" style="cursor:pointer;">'+caret+'</span>';
  tr.appendChild(td);
  
  // Name
  td = document.createElement('td');
  td.classList.add('td1');
  td.innerHTML = '<span> <i class="icon i-box-open w3-text-blue-gray"></i> <b>'+jpkg.name+'</b></span>';
  tr.appendChild(td);
  
  // Username
  td = document.createElement('td');
  td.classList.add('td1');
  //td.innerHTML = '<i class="icon i-edit w3-text-blue-gray w3-hover-text-blue" onclick="submitPackage ('+id+');" style="cursor:pointer;" title="Rename package '+jpkg.name+'"></i>';
  tr.appendChild(td);
  
  td = document.createElement('td');
  td.classList.add('td1');
  tr.appendChild(td);
  
  /*
  td = document.createElement('td');
  td.classList.add('td1');
  td.align = "right";
  td.innerHTML = '<i class="icon i-times w3-text-gray w3-hover-text-red" onclick="deletePackage ('+id+');" style="cursor:pointer;" title="Delete package '+jpkg.name+'"></i>';
  */
  
  var pkgId = id;
  
  tr.oncontextmenu = function (event) {
    //if (event.button == 2) {
      event.preventDefault();
      //event.stopPropagation();
      
      if (contextMenu) {
        contextMenu.remove();
      }
      
      contextMenu = new ContextMenu();
      contextMenu.init();
      
      var json = `
        { 
          "items": [
             { "id": 0, "item": "Add blueprint" },`+
             (jpkg.hasOwnProperty('datafile') ? `` : `{ "id": 1, "item": "Add datafile" }, `)+
             `{ "id": 2, "item": "Rename" }, 
             { "id": 3, "item": "Export" }, 
             { "id": 9, "item": "Delete" }
          ] 
        }`
      ;
      
      //console.log (json);

      //contextMenu.enableSearch (false);
      contextMenu.createFromJson(JSON.parse(json));
      //contextMenu.setCallback(null);
      contextMenu.callback = function (id, data) {
        switch (id) {
          case 0:
            submitBlueprint (null, pkgId);
            break;
            
          case 1:
            addPackageDatafile (pkgId);
            break;
            
          case 2:
            submitPackage (pkgId);
            break;
            
          case 9:
            deletePackage (pkgId);
            break;
            
          default:
            break;
        }
      }
      
      contextMenu.show(event.x, event.y);
    //}
  }
  
  tr.appendChild(td);
  
  table.appendChild(tr);

  tr.ondrop = function(e) {
    //e.preventDefault();
    var bp_id = e.dataTransfer.getData("id");
    var pkg_id = this.getAttribute('id').replace ('pkg_', '');
    
    moveBlueprint (bp_id, pkg_id);
  }
  
  total = Object.keys(tree[id]).length;
  
  //console.log('Package '+jpkg.name+' has '+total+' blueprints');
  
  if (jpkg.hasOwnProperty('datafile'))
    addDataToTable (table, id);
  
  // Content
  var i = 1;
  
  for (key in tree[id]) {
    //console.log('Adding blueprint '+key);
      
    flags = (i == total) ? ItemFlags.LAST : ItemFlags.NONE;
    
    addBlueprintToTable (table, key, jsonResponse[key], id, flags);
    
    i ++;
  }
}

function refreshTable () {

  document.getElementById("blueprints").innerHTML = "";
  var table = document.createElement('table');
  table.classList.add('table1');
  
  // Headers
  var tr = document.createElement('tr');
  tr.classList.add ('tr1');

  for (var i = 0; i < header.length; i++) {
      var th = document.createElement('th');
      th.classList.add('th1');
      //th.classList.add('w3-padding');
      th.innerHTML = header[i];
      tr.appendChild(th);
  }

  tr.ondragover = function(e) {
    e.preventDefault();
  }

  tr.ondrop = function(e) {
    var bp_id = e.dataTransfer.getData("id");
    moveBlueprint (bp_id, '_root');
  }
  
  table.appendChild(tr);
  
  var td, tr, key, row;
  var tree = jsonResponse["_tree"];
  //console.log (tree);

  if (tree) {
    //addToTable (table, tree, '_root');
    for (key in tree) {
      if (!jsonResponse.hasOwnProperty(key))
        continue;
        
      row = jsonResponse[key];
      
      //console.log(JSON.stringify(row));

      //if (typeof tree[key] === 'object') {
      if (!tree[key].hasOwnProperty("type")) {
        // Package
        addPackageToTable (table, tree, key, row);
      }
      else {
        // Blueprint
        addBlueprintToTable (table, key, row, '_root', ItemFlags.NONE);
      }
    }
  }
  
  console.log (expanded);
  
  document.getElementById('blueprints').appendChild(table);
}

function refreshBlueprintsCallback (xhttp) {

  //console.log ("refreshBlueprintsCallback() readyState = "+xhttp.readyState);

    if (xhttp.readyState == 4) { // Request finished and response is ready
    
      //console.log ("refreshBlueprintsCallback() status = "+xhttp.status);
      
      if (xhttp.status == 200) {

        //var data = xhttp.responseText;
        jsonResponse = JSON.parse(xhttp.responseText);

        refreshTable ();
      }
      else {
        document.getElementById('blueprints').innerHTML = xhttp.statusText;
      }
    }
    else if (this.readyState == 3) { // Processing request 
      document.getElementById('blueprints').innerHTML = "Getting blueprints...";
    }
    else {
      document.getElementById('blueprints').innerHTML = xhttp.statusText;
    }
    
    //document.getElementById("refresh").disabled = false;
  
  //document.getElementById("refresh").disabled = true;
}

function refreshBlueprints () {
  document.getElementById("blueprints").innerHTML =  '<div class="loader"></div>Refreshing...';
  //callServer ("GET", "blueprint", null, refreshBlueprintsCallback);
  callServer ("GET", "/catalog", null, refreshBlueprintsCallback);
};

function deleteBlueprintCallback (xhttp) {
  var message;
  console.log ('[deleteBlueprintCallback] readyState = '+ xhttp.readyState);

  if (xhttp.readyState == 4) { // Request finished and response is ready
    
    if (xhttp.status == 200) {
      showSnacknar(BPResult.SUCCESS, "Blueprint successfully deleted", 2000);
      //refreshBlueprints ();
      jsonResponse = JSON.parse(xhttp.responseText);
      refreshTable ();
    }
    else {
      message = xhttp.status == 0 ? "Can't delete blueprint" : xhttp.status+': '+xhttp.statusText;
      showSnacknar(BPResult.ERROR, message, 2000);
    }
  }
  else if (this.readyState == 3) { // Processing request 
    //blueprint.getElement().innerHTML = "Deleting blueprint...";
  }
  else {
    
  }
}

function deleteBlueprint (id, name) {
  //dialogOKCancel ("You are about to delete a blueprint.<br>Continue?", function (dialog) {
  dialogMessage ("Confirm", "Delete blueprint <b>"+name+"</b>?", DialogButtons.YES_NO, DialogIcon.QUESTION, function (dialog) {
      //console.log ('Deleting '+id);
      dialog.destroy();
      callServer ("DELETE", "/blueprint/"+id, null, deleteBlueprintCallback);
    }
  );
};

function submitBlueprint (bpId, parentId) {
  var dialog = new Dialog ();
  
  dialog.callbackOK = function (dlg) {
    var content = dlg.getContentElement ();

    var bpName = document.getElementById("bpName").value.trim();
    var resultElem = document.getElementById("result");
    
    if (!bpName) {
      resultElem.innerHTML = '<div class="w3-text-red">Missing blueprint name</div>';
      return;
    }
 
    var selfDialog = dlg;
    var resource = bpId ? "/blueprint/"+bpId : "/blueprint/_new?name="+bpName+ (parentId ? '&parent='+parentId : '');
    
    resource = resource + '&tree=1';

    resultElem.innerHTML = '<i class="icon i-spinner w3-spin"></i> Submitting blueprint...';
    
    //console.log ('Renaming '+pkgId);

    callServer ("PUT", resource, '{}', function (xhttp) {
      if (xhttp.readyState == 4) {
        if (xhttp.status == 200) {
          selfDialog.destroy();
          jsonResponse = JSON.parse(xhttp.responseText);
          
          if (jsonResponse.hasOwnProperty('_id'))
            window.open("/blueprint/"+jsonResponse["_id"]+"/edit");
          
          refreshTable ();
        }
        else
          resultElem.innerHTML = "<i class=\"icon i-exclamation w3-text-red\"></i> Can't submit blueprint:<br>"+xhttp.responseText;
      }
      else {
        //apiResponse.innerHTML = 'Saving...';
      }
    }
    
    );
  }
  
  dialog.callbackCancel = function (dialog) { dialog.destroy(); };
  
  /* Build dialog */
  var bp = null, bpName = 'New blueprint';
/*  
  if (pkgId)
    pkg = jsonResponse.hasOwnProperty(pkgId.toString()) ? jsonResponse[pkgId.toString()] : null;
    
  if (pkg) {
    pkgName = jsonResponse[pkgId.toString()].name;
  }
 
  //console.log ('Renaming '+pkgId+ ' '+pkgName);
      
  var title = pkg ? 'Rename package' : 'Create package';
  dialog.create(title);
*/

  dialog.create('Create blueprint');
  
  var content = dialog.getContentElement ();

  content.innerHTML = `
    <div class="w3-panel w3-padding-small">
        <label><b>Name</b></label>
        <input id="bpName" class="w3-input w3-border" type="text" placeholder="Name" value="`+bpName+`">
    </div>
    <div id="result" class="w3-panel">&nbsp;</div>
  `;

}

function newBlueprint () {
  submitBlueprint (null, null);
}

function moveBlueprint (id, parent_id) {
  callServer ("PUT", "/blueprint/"+id+"/move/"+parent_id, null, function (xhttp) {
    if (xhttp.readyState == 4) {
      if (xhttp.status == 200) {
        //refreshBlueprints ();
        jsonResponse = JSON.parse(xhttp.responseText);
        refreshTable ();
      }
      else
        showSnacknar(BPResult.ERROR, xhttp.status+': '+xhttp.statusText, 2000);
    }
    else {
    }
  }
  );
}

function isPackageEmpty (id) {
  var p = jsonResponse["_tree"][id];
  var n = 0;

  for (key in p)
    n ++;
    
  return (n == 0 && !jsonResponse[id].hasOwnProperty('datafile'));
}

function deletePackage (id) {
  if (isPackageEmpty (id)) {
    callServer ("DELETE", "/package/"+id, null, function (xhttp) {
      if (xhttp.readyState == 4) {
        if (xhttp.status == 200) {
          delete expanded.id;
          //refreshBlueprints ();
          jsonResponse = JSON.parse(xhttp.responseText);
          refreshTable ();
        }
        else {
          message = xhttp.status == 0 ? "Can't delete package" : xhttp.status+': '+xhttp.statusText;
          showSnacknar(BPResult.ERROR, message, 2000);
        }
      }
      else {
        //showSnacknar(BPResult.ERROR, "Can't delete package");
      }
    }
    );
  }
  else {
    showSnacknar(BPResult.WARNING, "Package is not empty", 2000);
  }
};

function addPackageDatafile (id) {
  callServer ("PUT", "/package/"+id+"/datafile?tree=1", null, function (xhttp) {
    if (xhttp.readyState == 4) {
      if (xhttp.status == 200) {
        jsonResponse = JSON.parse(xhttp.responseText);
        refreshTable ();
      }
      else {
        message = xhttp.status == 0 ? "Can't create datafile" : xhttp.status+': '+xhttp.statusText;
        showSnacknar(BPResult.ERROR, message, 2000);
      }
    }
    else {
      //showSnacknar(BPResult.ERROR, "Can't delete package");
    }
  }
  );
};

function deletePackageDatafile (id) {
  dialogMessage ("Confirm", "Delete datafile?", DialogButtons.YES_NO, DialogIcon.QUESTION, function (dialog) {
      dialog.destroy();
      
      callServer ("DELETE", "/package/"+id+"/datafile?tree=1", null, function (xhttp) {
        if (xhttp.readyState == 4) {
          if (xhttp.status == 200) {
            jsonResponse = JSON.parse(xhttp.responseText);
            refreshTable ();
          }
          else {
            message = xhttp.status == 0 ? "Can't delete package datafile" : xhttp.status+': '+xhttp.statusText;
            showSnacknar(BPResult.ERROR, message, 2000);
          }
        }
        else {
          //showSnacknar(BPResult.ERROR, "Can't delete package");
        }
      }
      );
    }
  );
};

function dragBlueprint(ev) {
  ev.dataTransfer.setData("id", ev.target.id.replace ('bp_', '') || ev.target.getAttribute('bp_id'));
  //console.log(ev.target.id.replace ('bp_', ''));
}

function expandPackage(id) {
  var x = document.getElementsByTagName("tr");
  var i;
  
  expanded[id] = !expanded[id];
  
  for (i = 0; i < x.length; i++) {
    var pkg_id = x[i].getAttribute('parent_id');
    //console.log (pkg_id +' '+ id);
    if (pkg_id == id)
      x[i].style.display = expanded[id] ? 'table-row' : 'none';
  }
  
  document.getElementById('caret_'+id).innerHTML = expanded[id] ? '<i class="icon i-caret-down"></i>' : '<i class="icon i-caret-right"></i>';
}

function submitPackage (pkgId) {
  var dialog = new Dialog ();
  
  dialog.callbackOK = function (dlg) {
    var content = dlg.getContentElement ();
    
    //var pkgId = document.getElementById("pkgId") ? document.getElementById("pkgId").value : null;
    var pkgName = document.getElementById("pkgName").value.trim();
    var resultElem = document.getElementById("result");
    
    if (!pkgName) {
      resultElem.innerHTML = '<div class="w3-text-red">Missing package name</div>';
      return;
    }
 
    var selfDialog = dlg;
    var resource = pkgId ? "/package/"+pkgId+"/rename/"+pkgName+"?tree=1" : "/package/_new";
    var data = pkgId ? '' : '{ "name":"'+pkgName+'" }';

    resultElem.innerHTML = '<i class="icon i-spinner w3-spin"></i> Submitting package...';
    
    console.log ('Renaming '+pkgId);

    callServer ("PUT", resource, data, function (xhttp) {
      if (xhttp.readyState == 4) {
        if (xhttp.status == 200) {
          selfDialog.destroy();
          jsonResponse = JSON.parse(xhttp.responseText);
          refreshTable ();
        }
        else
          resultElem.innerHTML = "<i class=\"icon i-exclamation w3-text-red\"></i> Can't submit package";
      }
      else {
        //apiResponse.innerHTML = 'Saving...';
      }
    }
    
    );
  }
  
  dialog.callbackCancel = function (dialog) { dialog.destroy(); };
  
  /* Build dialog */
  
  var pkg = null, pkgName = 'New package';
  
  if (pkgId)
    pkg = jsonResponse.hasOwnProperty(pkgId.toString()) ? jsonResponse[pkgId.toString()] : null;
    
  if (pkg) {
    pkgName = jsonResponse[pkgId.toString()].name;
  }
  
  //console.log ('Renaming '+pkgId+ ' '+pkgName);
      
  var title = pkg ? 'Rename package' : 'Create package';
  dialog.create(title);

  var content = dialog.getContentElement ();

  content.innerHTML = `
    <div class="w3-panel w3-padding-small">
        <!--input id="pkgId" class="w3-input" type="hidden" value="`+pkgId+`"-->
        <label><b>Name</b></label>
        <input id="pkgName" class="w3-input w3-border" type="text" placeholder="Name" value="`+pkgName+`">
    </div>
    <div id="result" class="w3-panel">&nbsp;</div>
  `;

}
