'use strict';

var title="API";
var g_apiList;
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

function refreshTable() {
    $.get("/apipanel?id=apis").done(function(fragment) { // get from controller
        $("#apis").replaceWith(fragment); // update snippet of page
    });
};

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
  var enabled = document.getElementById("enabled").value;

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
             "enabled": enabled
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
