

function refreshSessionsCallback (xhttp) {

  console.log ("refreshSessionsCallback() readyState = "+xhttp.readyState);

    if (xhttp.readyState == 4) { // Request finished and response is ready

      console.log ("refreshSessionsCallback() status = "+xhttp.status);

      if (xhttp.status == 200) {

        document.getElementById("sessions").innerHTML = "";
        var table = document.createElement('table');
        table.classList.add('table1');

        var data = xhttp.responseText;
        var jsonResponse = JSON.parse(data);

        var header = ['Client&nbsp;IP', 'User', 'Login&nbsp;time', 'Status', /*'Resource', 'API',*/ 'Blueprint', 'Agent'];
        //var fields = ['sid', 'pid', 'ip', 'resource', 'status', 'start_time', 'agent'];

        var tr = document.createElement('tr');

        for (var i = 0; i < header.length; i++) {
            var th = document.createElement('th');
            th.classList.add('th1');
            //th.classList.add('w3-padding');
            th.innerHTML = header[i];
            tr.appendChild(th);
        }

        table.appendChild(tr);

        var td, tr, key, row;
        var method = [ 'UNKNOW', 'HEAD', 'GET', 'PUT', 'POST', 'DELETE', 'OPTIONS' ];
        var status = [ 'IDLE', 'ACTIVE', 'EXECUTING', 'COMPLETED' ];

        //console.log (data);

        for (key in jsonResponse) {
          row = jsonResponse[key];
          tr = document.createElement('tr');

          // Skip completed
          if (row.status == 3)
            continue;
          /*
          for (var i = 0; i < fields.length; i++) {
            var td = document.createElement('td');
            td.classList.add('td1');

            if (!fields[i].localeCompare ('start_time')) {
              td.innerHTML = secondsToString(row[fields[i]]);
            }
            else if (!fields[i].localeCompare ('http_method')) {
              td.innerHTML = method[row[fields[i]]];
            }
            else if (!fields[i].localeCompare ('status')) {
              td.innerHTML = status[row[fields[i]]];
            }
            else
              td.appendChild(document.createTextNode(row[fields[i]]));

            tr.appendChild(td);
          }*/

          /* IP */
          var td = document.createElement('td');
          td.classList.add('td1');
          td.appendChild(document.createTextNode(row.remoteAddress));
          tr.appendChild(td);

          /* User */
          var td = document.createElement('td');
          td.classList.add('td1');
          td.appendChild(document.createTextNode(row.user));
          tr.appendChild(td);

          /* Start time */
          var td = document.createElement('td');
          td.classList.add('td1');
          td.innerHTML = secondsToString(dateFromISO8601(row.creationTime)/1000);
          tr.appendChild(td);

          /* Status */
          var td = document.createElement('td');
          td.classList.add('td1');
          td.innerHTML = status[row.status];
          tr.appendChild(td);

          /* Resource */
          /*
          var td = document.createElement('td');
          td.classList.add('td1');
          td.appendChild(document.createTextNode(row.requestURI));
          tr.appendChild(td);*/

          /* API */
          /*
          var td = document.createElement('td');
          td.classList.add('td1');
          td.appendChild(document.createTextNode(row.hasOwnProperty('api_name') ? row['api_name'] : ''));
          tr.appendChild(td);*/

          /* Blueprint */
          var td = document.createElement('td');
          td.classList.add('td1');
          td.appendChild(document.createTextNode(row.hasOwnProperty('blueprint_name') ? row['blueprint_name'] : ''));
          tr.appendChild(td);

          /* Agent */
          var td = document.createElement('td');
          td.classList.add('td1');
          td.appendChild(document.createTextNode(row.agent));
          tr.appendChild(td);

          table.appendChild(tr);
        }


        document.getElementById('sessions').appendChild(table);
      }
      else {

        document.getElementById('sessions').innerHTML = 'Error '+xhttp.status+' '+xhttp.statusText;
      }
    }
    else if (this.readyState == 3) { // Processing request
      document.getElementById('sessions').innerHTML = "Getting blueprints...";
    }
    else {
      document.getElementById('sessions').innerHTML = xhttp.statusText;
    }

    //document.getElementById("refresh").disabled = false;

  //document.getElementById("refresh").disabled = true;
}

function refreshSessions () {
  document.getElementById("sessions").innerHTML =  '<div class="loader"></div>Refreshing...';
  //callServer ("GET", "blueprint", null, refreshSessionsCallback);
  callServer ("GET", "/sessions", null, refreshSessionsCallback);
};
