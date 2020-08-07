'use strict';

var jsonResponse, nItems;

function callbackSaveData(dialog) {
  var jdata, value;
  var key = document.getElementById("dataKey").value;
  var type = Number(document.getElementById("dataType").value);
  var dataResponseDiv = document.getElementById("dataResponse");

  /*console.log ('key = "'+key+'"');
  //console.log ('type = "'+type+'"');*/

  if (key === null || key === "" || key === 'undefined') {
    dialog.showError ('A key must be specified');
    return;
  }
 /*
  if (dialog.isTimestamp()) {
    if (!document.getElementById("date").value) {
      dialog.showError ('Missing date');
      return;
    }
    if (!document.getElementById("time").value) {
      dialog.showError ('Missing time');
      return;
    }
  }*/

  key = key.trim();

  if (dialog.checkData()) {
  /*
    var value_str, ts;

    switch (type) {
      case 'Integer':
      case 'Double':
      case 'Boolean':
        value_str = dialog.getData();
        break;

      case 'String':
        value_str = "'"+dialog.getData().replace (/"/g,'\\"')+"'";
        break;

      default:
        value_str = JSON.stringify (dialog.getData());
        break;
    }

    ts = dialog.isTimestamp() ? ', "is_timestamp": true' : '';

    jdata = '{ "'+key+'": { "type": '+type+', "value": '+value_str+ts+'} }';
    */

    //var value_str = "'"+dialog.getData().replace (/"/g,'\\"')+"'";
    var jtmp = { "___value___":dialog.getData() };
    jdata = JSON.stringify(jtmp).replace("___value___", key);

    console.log (jdata);

    var selfDialog = dialog;

    callServer ("POST", __request, jdata, function (xhttp) {
        if (xhttp.readyState == 4) {
          if (xhttp.status == 200) {
            selfDialog.destroy();
            refreshData ();
          }
          else
            dataResponseDiv.innerHTML = '<div class="w3-text-red">Error: '+xhttp.statusText+'</div>';
        }
        else
          dataResponseDiv.innerHTML = 'Saving...';
      }
    );
  }
  else
    dialog.showError('Invalid data');
}

function editData (type, key) {
  var flags, value = jsonResponse[key];//.value;

  flags = DataFlags.EDIT_ONLY_VALUE;

  if (jsonResponse[key].is_timestamp === true)
    flags |= DataFlags.TIMESTAMP;

  var dialog = new DialogData ();

  dialog.callbackOK = callbackSaveData;

  dialog.callbackCancel = function (dialog) { dialog.destroy(); };
  dialog.create(type, key, value, flags);
}

function selectAll (ev) {
  var selected = ev.target.checked;

  for (var i=0; i<nItems; i++)
    document.getElementById("check_"+i).checked = selected;
}

function readFile(e) {
  var file = e.target.files[0];

  if (!file) {
    return;
  }
  var reader = new FileReader();
  reader.onload = function(e) {
    var contents = e.target.result;

    callServer ("POST", __request, contents, function (xhttp) {
        if (xhttp.readyState == 4) {
          if (xhttp.status == 200) {
            refreshData ();
          }
          else
            //dataResponseDiv.innerHTML = '<div class="w3-text-red">Error: '+xhttp.statusText+'</div>';
            console.error("Cant'import data: "+xhttp.status);
        }
        /*else
          dataResponseDiv.innerHTML = 'Saving...';*/
      }
    );

    /* Reset so 'changed' event triggers again */
    document.getElementById('file-dialog').value="";
  };

  console.log ("Reading file...");

  reader.readAsText(file);
}

function importData () {
  var input = document.getElementById('file-dialog');
  input.click();
}

function exportJson (jdata) {
  var text = JSON.stringify(jdata),
      blob = new Blob([text], { type: 'text/plain' }),
      anchor = document.createElement('a');

  console.log ("Exporting...");

  anchor.download = "data.json";
  anchor.href = (window.webkitURL || window.URL).createObjectURL(blob);
  anchor.dataset.downloadurl = ['text/plain', anchor.download, anchor.href].join(':');
  anchor.click();
}

function exportData () {
  var proceed=false, nSelected=0, jdata = {};

  if (nItems == 0)
    return;

  for (var i=0; i<nItems; i++) {
    if (document.getElementById("check_"+i).checked) {
      var key = document.getElementById("check_"+i).name;
      var jitem = jsonResponse[key];

      jdata[key] = jitem;

      nSelected ++;
    }
  }

  if (nSelected == 0) {
    //dialogOKCancel ("Export all data?", function (dialog) {
    dialogMessage ("Confirm", "Export all data?", DialogButtons.YES_NO, DialogIcon.QUESTION, function (dialog) {
        dialog.destroy();
        jdata = jsonResponse;

        // This function is asyncronous, call function here.
        exportJson (jdata);
      }
    );
  }
  else
    proceed = true;

  if (proceed)
    exportJson (jdata);
}

function refreshDataCallback (xhttp) {

  //console.log ("refreshDataCallback() readyState = "+xhttp.readyState);

    if (xhttp.readyState == 4) { // Request finished and response is ready

      //console.log ("refreshDataCallback() status = "+xhttp.status);

      if (xhttp.status == 200) {

        //document.getElementById("data").innerHTML = "";
        var table = document.getElementById("myTable");
        table.innerHTML = "";
        /*
        var table = document.createElement('table');
        //table.classList.add('table1');
        table.classList.add('w3-table');
        table.classList.add('w3-striped');
        table.classList.add('w3-bordered');
        //table.setAttribute('id', 'myTable');
        */

        var data = xhttp.responseText;

        console.log (data);

        jsonResponse = JSON.parse(data);

        /* Order keys */
        const ordered = {};
        Object.keys(jsonResponse).sort().forEach(function(key) {
          ordered[key] = jsonResponse[key];
        });

        jsonResponse = ordered;

        var header = ['<input type="checkbox" onchange="selectAll(event)"> Key', 'Value'];
        //var fields = ['pid', 'ip'];

        var tr = document.createElement('tr');

        for (var i = 0; i < header.length; i++) {
          var th = document.createElement('th');
          th.classList.add('th1');
          th.classList.add ('td1-border');
          th.classList.add('w3-light-grey');
          th.innerHTML = header[i];
          tr.appendChild(th);
        }

        table.appendChild(tr);

        var td, tr, key, value, type, type_str, attr_str='', i=0, /*rowId,*/ valueId, buttonsId;

        //console.log (data);

          for (key in jsonResponse) {
            /*type = jsonResponse[key].type;
            value = jsonResponse[key].value;*/


            if (key.toLowerCase().indexOf("password") > -1)
              value = '<span style="color:grey;">hidden</span>';
            else
              value = jsonResponse[key];

            //rowId = 'tr'+i;
            /*
            if (jsonResponse[key].is_timestamp === true) {
              value = secondsToString(value);
              //attr_str = '<i class="icon i-clock w3-text-orange" title="Timestamp"></i>';
            }*/

            tr = document.createElement('tr');
            tr.classList.add ('tr1');

            //for (var i = 0; i < fields.length; i++) {
              /*
              switch (type) {
                case 'Integer':
                  type_str = jsonResponse[key].is_timestamp ? 'Timestamp' : 'Integer';
                  break;

                case 'Double':
                  type_str = 'Float';
                  break;

                case 'Boolean':
                  type_str = 'Boolean';
                  break;

                case 'String':
                  type_str = 'String';
                  break;

                default:
                  type_str = 'Json';
                  break;
              } */

              // Key
              td = document.createElement('td');
              td.classList.add ('td1');
              td.classList.add ('td1-border');
              td.style.width = "25%";
              td.innerHTML = '<input id="check_'+i+'" type="checkbox" name="'+key+'"> '+key;
              tr.appendChild(td);

              // Type
              /*
              td = document.createElement('td');
              td.innerHTML = '<i class="icon '+ (jsonResponse[key].is_timestamp ? "i-clock" : getTypeIcon(type)) +'" style="color:gray"></i> '+type_str;
              td.classList.add ('td1');
              td.classList.add ('td1-border');
              tr.appendChild(td);*/

              // Attributes
              /*
              td = document.createElement('td');
              td.innerHTML = attr_str;
              td.classList.add ('td1');
              td.classList.add ('td1-border');
              tr.appendChild(td);
              */

              // Data
              td = document.createElement('td');
              td.classList.add('td1');
              td.classList.add ('td1-border');
              td.classList.add('data');
              td.innerHTML = `<div class="w3-hover-text-green" onclick='editData ("String", "`+key+`")' style="cursor:pointer;" title="Edit value">`+(typeof value === 'object' ? JSON.stringify(value, null, 2) : value)+`</div>`;

              tr.appendChild(td);

              // Delete
              /*
              td = document.createElement ('td');
              td.classList.add ('td1');
              td.innerHTML = '<i class="icon i-trash-alt w3-text-gray w3-hover-text-red" onclick="deleteData (\''+key+'\')" style="cursor:pointer;" title="Delete '+key+'"></i>';
              tr.appendChild (td);
              */

              //console.log (typeof value);

            table.appendChild(tr);

            i ++;
          }

        nItems = i;

        //document.getElementById('data').appendChild(table);
      }
      else {

        //document.getElementById('data').innerHTML = 'Error '+xhttp.status+' '+xhttp.statusText;
      }
    }
    else if (xhttp.readyState == 3) { // Processing request
      //document.getElementById('data').innerHTML = "Getting blueprints...";
    }
    else {
      //document.getElementById('data').innerHTML = xhttp.statusText;
    }

    //document.getElementById("refresh").disabled = false;

  //document.getElementById("refresh").disabled = true;
}

function refreshData () {
  //document.getElementById("data").innerHTML =  '<div class="loader"></div>Refreshing...';
  callServer ("GET", __request, null, refreshDataCallback);
};

function deleteDataCallback (xhttp) {
  if (xhttp.readyState == 4) { // Request finished and response is ready

    //console.log (xhttp.responseText);

    if (xhttp.status == 200) {
      showSnacknar("Data successfully deleted", 2000);
      refreshData ();
    }
    else {
      //blueprint.getElement().innerHTML = xhttp.statusText;
    }
  }
  else if (xhttp.readyState == 3) { // Processing request
    //blueprint.getElement().innerHTML = "Deleting blueprint...";
  }
  else {
    showSnacknar(xhttp.statusText);
  }
}

function deleteData (jlist) {
  //document.getElementById("data").innerHTML =  '<div class="loader"></div>Deleting '+id+'...';
  callServer ("DELETE", __request, JSON.stringify(jlist), deleteDataCallback);
};

function deleteSelected () {
  var jlist = [], nSelected = 0;

  for (var i=0; i<nItems; i++) {
    if (document.getElementById("check_"+i).checked) {
      var key = document.getElementById("check_"+i).name;

      jlist.push(key);

      nSelected ++;
    }
  }

  if (nSelected)
    deleteData (jlist);
};
