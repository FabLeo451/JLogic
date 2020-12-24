'use strict';

var jsonResponse, submitPassword = false;

function refreshTable () {
  var table = document.getElementById("users-table");
  table.innerHTML = "";

  /* Order keys */
  /*
  const ordered = {};
  Object.keys(jsonResponse).sort().forEach(function(key) {
    ordered[key] = jsonResponse[key];
  });

  jsonResponse = ordered;
  */

  var header = ['User', 'Role', 'Name', 'Lock', 'Created', ''];

  var tr = document.createElement('tr');

  for (var i = 0; i < header.length; i++) {
    var th = document.createElement('th');
    th.classList.add('th1');
    th.classList.add('w3-light-grey');
    th.innerHTML = header[i];
    tr.appendChild(th);
  }

  table.appendChild(tr);

  var td, tr, key, value, type, type_str, attr_str='', i=0, /*rowId,*/ valueId, buttonsId;

  //console.log (data);

  for (key in jsonResponse) {
    var user = jsonResponse[key];

    tr = document.createElement('tr');
    tr.classList.add ('tr1');

    // User
    td = document.createElement('td');
    td.classList.add ('td1');
    td.innerHTML = '<a href="/user/'+user.username+'/edit">'+user.username+'</a>';
    tr.appendChild(td);

    // Role
    var roles = ["admin", "editor", "viewer", "user"];
    td = document.createElement('td');
    td.classList.add ('td1');
    td.innerHTML = roles[user.roleSet];
    tr.appendChild(td);

    // Name
    td = document.createElement('td');
    td.classList.add ('td1');
    td.innerHTML = (user.firstName ? user.firstName : "") + " " + (user.lastName ? user.lastName : "");
    tr.appendChild(td);

    // Lock
    var locked = user.hasOwnProperty('accountNonLocked') ? !user.accountNonLocked : false;
    td = document.createElement('td');
    td.classList.add ('td1');
    
    if (!user.hasOwnProperty('reserved') || !user.reserved)
      td.innerHTML = locked ? `<i class="icon i-lock" style="color:goldenrod;cursor:pointer;" onclick="lockUser('`+user.id+`', '`+user.username+`', false);" title="Unlock"></i>` : `<i class="icon i-lock-open" style="color:gray;cursor:pointer;" onclick="lockUser('`+user.id+`', '`+user.username+`', true);" title="Lock"></i>`;
      
    tr.appendChild(td);

    // Created
    td = document.createElement('td');
    td.classList.add('td1');
    td.innerHTML = secondsToString(Date.parse(user.creationTime) / 1000); //secondsToString(user.creationTime);
    tr.appendChild(td);

    // Trash
    td = document.createElement('td');
    td.classList.add('td1');

    if (!user.hasOwnProperty('reserved') || !user.reserved)
      td.innerHTML = `<a target="Javascript:void(0);" onclick="deleteUser ('`+user.id+`', '`+user.username+`')" style="cursor:pointer;"><i class="icon i-trash-alt w3-text-gray w3-hover-text-red"></i></a>`;
    tr.appendChild(td);

    table.appendChild(tr);
    
    i ++;
  }
}

function refreshUsersCallback (xhttp) {

  if (xhttp.readyState == 4) {
    if (xhttp.status == 200) {
      jsonResponse = JSON.parse(xhttp.responseText);
      refreshTable ();
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
}

function validateData () {
  var juser = {};
  
  juser.id = document.getElementById("userId").value;
  juser.id = juser.id > 0 ? juser.id : null;
  juser.username = document.getElementById('username').value;
  juser.firstName = document.getElementById('firstName').value;
  juser.lastName = document.getElementById('lastName').value;
  juser.roleSet = document.getElementById('role') ? document.getElementById('role').value : -1;
  
  /*var roleElem = document.getElementById('role');
  var role = roleElem.options[roleElem.selectedIndex].text;*/
  
  var responseElem = document.getElementById("response");
  
  //console.log (username+' '+password+' '+password_confirmed+' '+role);
  
  if (!juser.username || juser.username == "") {
    responseElem.innerHTML = "<i class=\"icon i-exclamation-triangle w3-text-red\"></i> Missing user name.";
    return null;
  }
  
  var pwdElem = document.getElementById('password');
  
  if (pwdElem) {
    juser.password = document.getElementById('password').value;
    var password_confirmed = document.getElementById('password_confirmed').value;

    if (!juser.password || juser.password == "") {
      responseElem.innerHTML = "<i class=\"icon i-exclamation-triangle w3-text-red\"></i> Missing password.";
      return null;
    }
    
    if (juser.password != password_confirmed) {
      responseElem.innerHTML = "<i class=\"icon i-exclamation-triangle w3-text-red\"></i> Password confirmation mismatch.";
      return null;
    }
  }

  return (juser);
};

function createUser() {
  var juser = validateData();
  var responseElem = document.getElementById("response");
  
  if (juser) {
    callServer ("POST", '/user', JSON.stringify(juser), function (xhttp) {
      if (xhttp.readyState == 4) {
        if (xhttp.status == 200) {
          responseElem.innerHTML = '<i class="icon i-check w3-text-green"></i> Successfully created. Redirecting...';
          window.location = '/view-users';
        }
        else
          responseElem.innerHTML = "<i class=\"icon i-exclamation-triangle w3-text-red\"></i> "+xhttp.responseText;
      }
      else {
        responseElem.innerHTML = '<i class="icon i-spinner w3-spin"></i> Creating user...';
      }
    }
    );
  }
}

function updateUser() {
  var juser = validateData();
  var responseElem = document.getElementById("response");
  
  if (juser) {
    callServer ("PUT", juser.id ? '/user/'+juser.id : '/me', JSON.stringify(juser), function (xhttp) {
      if (xhttp.readyState == 4) {
        if (xhttp.status == 200) {
          responseElem.innerHTML = '<i class="icon i-check w3-text-green"></i> Successfully updated.';
          //window.location = '/view-users';
        }
        else
          responseElem.innerHTML = "<i class=\"icon i-exclamation-triangle w3-text-red\"></i> "+xhttp.responseText;
      }
      else {
        responseElem.innerHTML = '<i class="icon i-spinner w3-spin"></i> Updating user...';
      }
    }
    );
  }
}

function refreshUsers () {
  callServer ("GET", "/users", null, refreshUsersCallback);
};

function deleteUser (id, name) {
  dialogMessage ("Confirm", "Delete user <b>"+name+"</b>?", DialogButtons.YES_NO, DialogIcon.QUESTION, function (dialog) {
      dialog.destroy();
      callServer ("DELETE", "/user/"+id, null, function (xhttp) {
        if (xhttp.readyState == 4) {
          if (xhttp.status == 200) {
            showSnacknar(BPResult.SUCCESS, "User successfully deleted", 2000);
            jsonResponse = JSON.parse(xhttp.responseText);
            refreshTable ();
          }
          else
            dialogError ("Unable to delete user.\n"+xhttp.responseText);
        }
        else {
        }
      }
      );
    }
  );
};

function lockUser (id, name, lock) {
  dialogMessage ("Confirm", (lock ? 'Lock' : 'Unlock')+" user <b>"+name+"</b>?", DialogButtons.YES_NO, DialogIcon.QUESTION, function (dialog) {
      dialog.destroy();
      callServer ("POST", "/user/"+id+"/"+(lock ? 'lock' : 'unlock'), null, function (xhttp) {
        if (xhttp.readyState == 4) {
          if (xhttp.status == 200) {
            showSnacknar(BPResult.SUCCESS, "User successfully "+(lock ? 'locked' : 'unlocked'), 2000);
            jsonResponse = JSON.parse(xhttp.responseText);
            refreshTable ();
          }
          else
            dialogError ("Unable to "+(lock ? 'lock' : 'unlock')+" user.\n"+xhttp.responseText);
        }
        else {
        }
      }
      );
    }
  );
};


function updatePassword() {
  var juser = {};
  var responseElem = document.getElementById("response");
  
  
  var pwdElem = document.getElementById('password');
  
  if (pwdElem) {
    juser.password = document.getElementById('password').value;
    var password_confirmed = document.getElementById('password_confirmed').value;

    if (!juser.password || juser.password == "") {
      responseElem.innerHTML = "<i class=\"icon i-exclamation-triangle w3-text-red\"></i> Missing password.";
      return;
    }
    
    if (juser.password != password_confirmed) {
      responseElem.innerHTML = "<i class=\"icon i-exclamation-triangle w3-text-red\"></i> Password confirmation mismatch.";
      return;
    }
  }
  else
    return;
  
  if (juser) {
    callServer ("PUT", '/password', JSON.stringify(juser), function (xhttp) {
      if (xhttp.readyState == 4) {
        if (xhttp.status == 200) {
          responseElem.innerHTML = '<i class="icon i-check w3-text-green"></i> Successfully updated.';
          //window.location = '/view-users';
        }
        else
          responseElem.innerHTML = "<i class=\"icon i-exclamation-triangle w3-text-red\"></i> "+xhttp.responseText;
      }
      else {
        responseElem.innerHTML = '<i class="icon i-spinner w3-spin"></i> Updating password...';
      }
    }
    );
  }
}

