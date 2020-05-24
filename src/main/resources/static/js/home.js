'use strict';

var pluginList;

function refreshTable () {
  var table = document.getElementById("plugin-table");
  table.innerHTML = "";

  /* Order keys */
  const ordered = {};
  Object.keys(pluginList).sort().forEach(function(key) {
    ordered[key] = pluginList[key];
  });

  pluginList = ordered;

  var i, header = ['Name', 'Version', 'Status', 'Message'];
  
  var tr = document.createElement('tr');

  for (var i = 0; i < header.length; i++) {
    var th = document.createElement('th');
    
    if (i == 0)
      th.classList.add('td1-short');
      
    th.innerHTML = header[i];
    tr.appendChild(th);
  }
  
  table.appendChild(tr);

  var td, tr, plugin;

  for (i in pluginList) {
    plugin = pluginList[i];
    
    if (plugin.flags == 0 || plugin.type == 0)
      continue;

    tr = document.createElement('tr');
    
    /* Name */
    td = document.createElement('td');
    td.classList.add('td1-short');
    td.innerHTML = plugin.name;
    tr.appendChild(td);
    
    /* Version */
    td = document.createElement('td');
    td.innerHTML = plugin.version;
    tr.appendChild(td);
    
    /* Status */
    td = document.createElement('td');
    
    switch (plugin.result) {
      case 0:
        td.innerHTML = '<i class="icon i-check w3-text-green"></i> OK';
        break;
        
      case 3:
        td.innerHTML = '<i class="icon i-exclamation-triangle w3-text-orange"></i> Warning';
        break;
        
      default:
        td.innerHTML = '<i class="icon i-exclamation w3-text-red"></i> Error';
        break;
    }
      
    tr.appendChild(td);
    
    /* Message */
    td = document.createElement('td');
    td.innerHTML = plugin.result == 0 ? 'Working' : plugin.message;
    tr.appendChild(td);

    table.appendChild(tr);
  }
  
  //document.getElementById('plugin-table').appendChild(table);
}

function refreshPluginsCallback (xhttp) {
  if (xhttp.readyState == 4) { // Request finished and response is ready
    if (xhttp.status == 200) {
      pluginList = JSON.parse(xhttp.responseText);
      refreshTable ();
    }
    else {
      //document.getElementById('plugin-table').innerHTML = 'Error '+xhttp.status+' '+xhttp.statusText;
      dialogError ("Unable to get data from server.");
    }
  }
  else  { // Processing request 
    //document.getElementById('plugin-table').innerHTML = "Getting data...";
  }
}

function refreshPlugins () {
  //document.getElementById("plugin-table").innerHTML =  '<div class="loader"></div>Refreshing...';
  callServer ("GET", "/plugin", null, refreshPluginsCallback);
};

