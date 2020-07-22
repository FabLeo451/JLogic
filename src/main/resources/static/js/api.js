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

        var header = ['API', '', 'Program', 'Blueprint', 'Enabled', 'Modified', null];
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

            //for (var i = 0; i < fields.length; i++) {
              // API name
              td = document.createElement('td');
              td.classList.add('td1');
              //td.classList.add('w3-padding');
              td.innerHTML = `<a target="Javascript:void(0);" onclick="editAPI (`+index+`)" style="cursor:pointer;"><strong>`+value["name"]+`</strong></a>`;
              tr.appendChild(td);

              // GET
              td = document.createElement('td');
              td.classList.add('td1');
              td.innerHTML = '<a target="_blank" href="/api/'+value.name+'">GET <i class="icon i-external-link-alt w3-text-gray" title="HTTP GET"></a>';
              tr.appendChild(td);

              // Program
              td = document.createElement('td');
              td.classList.add('td1');
              /*var pr = searchCatalog (jcatalog, value["programId"]);
              var prName = pr ? pr["name"] : '<div class="w3-text-red">Not found</div>';
              td.innerHTML = prName;*/
              td.innerHTML = value.blueprint ? value.blueprint.programName : '';
              tr.appendChild(td);

              // Blueprint
              td = document.createElement('td');
              td.classList.add('td1');
              var bp = value.blueprint; //searchCatalog (jcatalog, value["blueprintId"]); //_blueprints[value["blueprintId"]];
              var bpName = bp ?
                           bp.name+' <a href="/blueprint/'+bp.id+'/edit" target="_blank" style="cursor:pointer;"><i class="icon i-external-link-alt w3-text-gray w3-hover-text-green" title="Open blueprint"></a>' :
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

              td = document.createElement('td');
              td.classList.add('td1');
              //td.classList.add('w3-padding');
              td.appendChild(document.createTextNode(secondsToString(dateFromISO8601(value.updateTime) / 1000)));
              tr.appendChild(td);

              td = document.createElement ('td');
              td.classList.add ('td1');
              td.setAttribute ('id', buttonsId);
              //console.log (value);
              //td.innerHTML = '<button class="btnApp" onclick="deleteAPI ('+value.id+')"><i class="icon i-trash-alt"></i> Delete</button>';
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
  callServer ("GET", "/api", null, refreshAPICallback);
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

function editAPIDialog (title, api, cbFun) {
  var dialog = new Dialog ();

  /* Callback functions */

  dialog.callbackOK = function (dlg) {
    var content = dlg.getContentElement ();

    var apiName = document.getElementById("apiName").value.trim();
    var progamId = document.getElementById("program").value;
    var blueprintId = document.getElementById("blueprint").value;
    var resultElem = document.getElementById("result");

    if (!apiName) {
      resultElem.innerHTML = '<div class="w3-text-red">Missing API name</div>';
      return;
    }
    var data = {"name":apiName, /*"programId":progamId,*/ "blueprint":{"id":blueprintId}, "enabled":true};

    if (api)
      data.id = api.id;

    dlg.destroy();

    console.log(data);
    cbFun(data);
  }

  dialog.callbackCancel = function (dialog) { dialog.destroy(); };

  /* Build dialog */

  dialog.create(title);

  var content = dialog.getContentElement ();
  var programOptions = "<option disabled selected value>Select program</option>";

  for (var key in jindex) {
    if (jindex[key].type == 1)
      programOptions += '<option value="'+key+'">'+jindex[key].name+'</option>';
  }

  content.innerHTML = `
    <div class="w3-panel w3-padding-small">
      <label for="apiName"><b>Name</b></label>
      <input id="apiName" class="w3-input w3-border" type="text" placeholder="Name" value="`+api.getName()+`">
      <br>
      <label for="program"><b>Program</b></label> <select id="program" onchange="programChanged(event);" class="w3-select">`+programOptions+`</select>
      <br><br>
      <label for="blueprint"><b>Blueprint</b></label> <select id="blueprint" class="w3-select"></select>
    </div>
    <div id="result" class="w3-panel">&nbsp;</div>
  `;

  if (api.programId) {
    document.getElementById("program").value = api.programId;

    callServer ("GET", "/program/"+api.programId+"/index", null, function (xhttp) {
        if (xhttp.readyState == 4) {
          if (xhttp.status == 200) {
            var jprogindex = JSON.parse(xhttp.responseText);

            var blueprintOptions = "";//"<option disabled selected value>Select program</option>";

            for (var i in jprogindex) {
              if (jprogindex[i].type == 'EVENTS')
                continue;

              blueprintOptions += '<option value="'+jprogindex[i].id+'">'+jprogindex[i].name+'</option>';
            }

            document.getElementById("blueprint").innerHTML = blueprintOptions;
            document.getElementById("blueprint").value = api.blueprintId;
          }
          else {
            showSnacknar(BPResult.ERROR, xhttp.responseText);
          }
        }
      }
    );
  }
}

function createAPI () {
  var api = new API();
  api.setName('New API');

  editAPIDialog ('Create API', api, function (data) {
      var d = dialogMessage ('Working', 'Creating API...', DialogButtons.NONE, DialogIcon.RUNNING, null);
      callServer ("POST", '/api', JSON.stringify(data), function (xhttp) {
          if (xhttp.readyState == 4) {
            d.destroy();

            console.log(xhttp.status+' - '+xhttp.responseText);

            if (xhttp.status == 200) {
              g_apiList = JSON.parse(xhttp.responseText);
              refreshTable ();
            }
            else {
              //showSnacknar(BPResult.ERROR, xhttp.responseText);
              dialogError (JSON.parse(xhttp.responseText).message);
            }
          }
        }
      );
    }
  );
}

function editAPI (index) {
  var japi = g_apiList[index];
  console.log(JSON.stringify(g_apiList));
  var api = new API();
  api.fromJson(japi);
  api.setId(japi.id);

  console.log(api.toJSON());

  editAPIDialog ('Edit API', api, function (data) {
      var d = dialogMessage ('Working', 'Updating API...', DialogButtons.NONE, DialogIcon.RUNNING, null);
      callServer ("PUT", '/api/'+api.id, JSON.stringify(data), function (xhttp) {
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

function deleteAPI (id, name) {
  dialogMessage ("Confirm", "Delete API "+name+"?", DialogButtons.YES_NO, DialogIcon.QUESTION, function (dialog) {
      dialog.destroy();
      var d = dialogMessage ('Working', 'Deleting folder...', DialogButtons.NONE, DialogIcon.RUNNING, null);

      callServer ("DELETE", "/api/"+id, null, function (xhttp) {
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
      callServer ("PUT", "/api/"+id+"/"+(enabled ? 'enable' : 'disable'), null, function (xhttp) {
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
