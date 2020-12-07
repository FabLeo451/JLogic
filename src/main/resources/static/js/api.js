'use strict';

var title="API";
var g_apiList;
var jcatalog = null, jindex = null;
var dialogWorking;

function searchCatalog(jroot, id) {
  var key;

  if (jroot == null)
    return (null);

  if (jroot.hasOwnProperty(id))
    return (jroot[id]);

  for (key in jroot) {
    var jchildren = jroot[key].children;
    var jitem = searchCatalog(jchildren, id);

    if (jitem)
      return (jitem);
  }

  return (null);
}

function refreshTable () {
  console.log ('Refreshing table...');
  var index;

        document.getElementById("api-table").innerHTML = "";
        var table = document.createElement('table');
        table.classList.add('table1');
        table.setAttribute('id', 'myTable');

        /* Order keys */
        const ordered = {};
        Object.keys(g_apiList).sort().forEach(function(key) {
          ordered[key] = g_apiList[key];
        });

        g_apiList = ordered;

        var header = ['API', 'Method', 'Path', 'Program', 'Blueprint', 'Enabled', 'Modified', null, null];
        //var fields = ['name', 'blueprint_id', 'enabled', 'modified'];

        var tr = document.createElement('tr');

        for (var i = 0; i < header.length; i++) {
            var th = document.createElement('th');
            th.classList.add('th1');
            //th.classList.add('w3-padding');
            th.innerHTML = header[i];
            tr.appendChild(th);
        }

        table.appendChild(tr);

        var td, tr, key, value, i=0, rowId, valueId, buttonsId;

        //console.log (data);

          for (index in g_apiList) {
            //if (key == "_index")
            //  continue;

            value = g_apiList[index];
            rowId = 'tr'+i;
            valueId = 'value_'+i;
            buttonsId = 'btns_'+i;
            tr = document.createElement('tr');
            tr.classList.add ('tr1');
            tr.setAttribute('id', rowId);

              // API name
              td = document.createElement('td');
              td.classList.add('td1');
              td.innerHTML = `<a href="/mapping/`+value.id+`/edit" style="cursor:pointer;"><strong>`+value["name"]+`</strong></a>`;
              tr.appendChild(td);

              // Method
              td = document.createElement('td');
              td.classList.add('td1');
              td.innerHTML = value.method;
              tr.appendChild(td);

              // Path
              td = document.createElement('td');
              td.classList.add('td1');
              td.innerHTML = value.path;
              tr.appendChild(td);

              // Program
              td = document.createElement('td');
              td.classList.add('td1');
              td.innerHTML = value.blueprint ? value.blueprint.programName : '';
              tr.appendChild(td);

              // Blueprint
              td = document.createElement('td');
              td.classList.add('td1');
              var bp = value.blueprint; //searchCatalog (jcatalog, value["blueprintId"]); //_blueprints[value["blueprintId"]];
              var bpName = bp ?
                           '<a href="/blueprint/'+bp.id+'/edit" target="_blank" style="cursor:pointer;">'+bp.name+' <i class="icon i-external-link-alt w3-text-gray" title="Open blueprint"></a>' :
                           '<div class="w3-text-red">Not found</div>';
              td.innerHTML = bpName;
              tr.appendChild(td);

              // Enabled
              td = document.createElement('td');
              td.classList.add('td1');

              if (value.enabled) {
                td.innerHTML = `<i class="icon i-check w3-text-green" style="cursor:pointer;" onclick="enableAPI('`+value.id+`', '`+value.name+`', false);" title="Disable"></i>`;
              }
              else {
                td.innerHTML = `<i class="icon i-ban w3-text-red" style="cursor:pointer;" onclick="enableAPI('`+value.id+`', '`+value.name+`', true);" title="Enable"></i>`;
              }

              tr.appendChild(td);

              // Modified
              td = document.createElement('td');
              td.classList.add('td1');
              td.appendChild(document.createTextNode(secondsToString(Date.parse(value.updateTime) / 1000)));
              tr.appendChild(td);
              
              // View log
              td = document.createElement('td');
              td.classList.add('td1');
              td.innerHTML = `<a href="/mapping/`+value.id+`/view-log" style="cursor:pointer;">Log</a>`;
              tr.appendChild(td);

              // Delete
              td = document.createElement ('td');
              td.classList.add ('td1');
              td.setAttribute ('id', buttonsId);
              td.innerHTML = `<a target="Javascript:void(0);" onclick="deleteAPI ('`+value.id+`', '`+value.name+`')" style="cursor:pointer;"><i class="icon i-trash-alt w3-text-gray w3-hover-text-red"></i></a>`;
              tr.appendChild (td);

              //console.log (typeof value);

            table.appendChild(tr);

            i ++;
          //}
        }

        document.getElementById('api-table').appendChild(table);
}

function refreshAPICallback (xhttp) {
  if (xhttp.readyState == 4) { // Request finished and response is ready
    if (xhttp.status == 200) {
      g_apiList = JSON.parse(xhttp.responseText);
      refreshTable ();
    }
    else {
      //document.getElementById('api-table').innerHTML = 'Error '+xhttp.status+' '+xhttp.statusText;
      dialogError ("Unable to get data from server.");
    }
  }
  else  { // Processing request
    //document.getElementById('api-table').innerHTML = "Getting data...";
  }
}

function refreshAPIs () {
  document.getElementById("api-table").innerHTML =  '<div class="loader"></div>Refreshing...';
  callServer ("GET", "/mapping", null, refreshAPICallback);
};

function loadCatalog () {
  var d = dialogMessage (title, 'Getting catalog...', DialogButtons.NONE, DialogIcon.RUNNING, null);

  callServer ("GET", "/catalog", null, function (xhttp) {
      if (xhttp.readyState == 4) {
        d.destroy();

        if (xhttp.status == 200) {
          jcatalog = JSON.parse(xhttp.responseText);
          jindex = jcatalog['_index'];
          console.log("Catalog OK");

          refreshAPIs();
        }
        else {
          showSnacknar(BPResult.ERROR, xhttp.statusText);
        }
      }
    }
  );

}

class API {
  constructor() {
    this.id = null;
    this.name = "MyAPI";
    this.enabled = true;
    this.programId = null;
    this.blueprintId = null;
  }

  fromJson(jo) {
    this.id = jo.hasOwnProperty('id') ? jo.id : null;
    this.name = jo.name;
    this.enabled = jo.enabled;
    this.programId = jo.blueprint.programId;
    this.blueprintId = jo.blueprint.id;
  }

  toJSON() {
    var jo = {"id":this.id, "name":this.name, "enabled":this.enabled, "programId":this.programId, "blueprintId":this.blueprintId};
    return (jo);
  }

  setId(id) { this.id = id; }
  setName(name) { this.name = name; }
  getName() { return(this.name); }
};

function programChanged(event) {
  var id = document.getElementById("program").value;

  var d = dialogMessage (title, 'Getting blueprints...', DialogButtons.NONE, DialogIcon.RUNNING, null);

  callServer ("GET", "/program/"+id+"/index", null, function (xhttp) {
      if (xhttp.readyState == 4) {
        d.destroy();

        if (xhttp.status == 200) {
          var jprogindex = JSON.parse(xhttp.responseText);

          var programOptions = "";//"<option disabled selected value>Select program</option>";

          for (var i in jprogindex) {
              if (jprogindex[i].type == 'EVENTS')
                continue;

              programOptions += '<option value="'+jprogindex[i].id+'">'+jprogindex[i].name+'</option>';
          }

          document.getElementById("blueprint").innerHTML = programOptions;
        }
        else {
          showSnacknar(BPResult.ERROR, xhttp.statusText);
        }
      }
    }
  );
}

function validateAPI() {
  var apiName = document.getElementById("apiName").value.trim();
  var progamId = document.getElementById("program").value;
  var path = document.getElementById("path").value;

  if (apiName == "" || apiName == null) {
    dialogError ("Missing API name.");
    return null;
  }

  if (path == "" || path == null) {
    dialogError ("Missing path.");
    return null;
  }

  if (progamId == "") {
    dialogInfo ("Select program.");
    return null;
  }
  
  var api = {"name": apiName, 
             "blueprint": {"id": document.getElementById("blueprint").value}, 
             "method": document.getElementById("method").value, 
             "path": path, 
             "enabled": true
            };
            
  return api;
}

function submitAPI(method, id) {
  var message, url;
  var api = validateAPI();
  
  if (!api)
    return;
    
  api.id = id;
    
  switch (method) {
    case "POST":
      message = 'Creating API...';
      url = '/mapping';
      break;
    case "PUT":
      message = 'Updating API...';
      url = '/mapping/'+id;
      break;
  }
    
  var d = dialogMessage ('Working', message, DialogButtons.NONE, DialogIcon.RUNNING, null);

  callServer(method, url, JSON.stringify(api), function (xhttp) {
      if (xhttp.readyState == 4) {
        d.destroy();

        console.log(xhttp.status+' - '+xhttp.responseText);

        if (xhttp.status == 200) {
          console.log("API successfully submitted");
          window.location = '/apipanel';
        }
        else {
          //showSnacknar(BPResult.ERROR, xhttp.responseText);
          dialogError (JSON.parse(xhttp.responseText).message);
        }
      }
    }
  );
}

function createAPI() {
  submitAPI('POST', null);
}

function updateAPI() {
  submitAPI('PUT', document.getElementById("apiId").value);
}

function deleteAPI (id, name) {
  dialogMessage ("Confirm", "Delete API "+name+"?", DialogButtons.YES_NO, DialogIcon.QUESTION, function (dialog) {
      dialog.destroy();
      var d = dialogMessage ('Working', 'Deleting folder...', DialogButtons.NONE, DialogIcon.RUNNING, null);

      callServer ("DELETE", "/mapping/"+id, null, function (xhttp) {
          if (xhttp.readyState == 4) {
            d.destroy();

            if (xhttp.status == 200) {
              g_apiList = JSON.parse(xhttp.responseText);
              refreshTable ();
            }
            else {
              dialogError (JSON.parse(xhttp.responseText).message);
            }
          }
        }
      );
    }
  );
}

function enableAPI (id, name, enabled) {
  dialogMessage ("Confirm", (enabled ? 'Enable' : 'Disable')+" API <b>"+name+"</b>?", DialogButtons.YES_NO, DialogIcon.QUESTION, function (dialog) {
      dialog.destroy();
      callServer ("PUT", "/mapping/"+id+"/"+(enabled ? 'enable' : 'disable'), null, function (xhttp) {
        if (xhttp.readyState == 4) {
          if (xhttp.status == 200) {
            showSnacknar(BPResult.SUCCESS, "API successfully "+(enabled ? 'enabled' : 'disabled'), 2000);
            g_apiList = JSON.parse(xhttp.responseText);
            refreshTable ();
          }
          else
            dialogError (JSON.parse(xhttp.responseText).message);
        }
        else {
        }
      }
      );
    }
  );
};
