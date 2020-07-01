
function includeHTML() {
  var z, i, elmnt, file, xhttp;
  
  /* loop through a collection of all HTML elements: */
  z = document.getElementsByTagName("*");
  
  for (i = 0; i < z.length; i++) {
    elmnt = z[i];
    
    /*search for elements with a certain atrribute:*/
    file = elmnt.getAttribute("w3-include-html");
    
    if (file) {
      console.log ('Including '+file);
      
      /*make an HTTP request using the attribute value as the file name:*/
      xhttp = new XMLHttpRequest();
      
      xhttp.onreadystatechange = function() {
        if (this.readyState == 4) {
          if (this.status == 200) {elmnt.innerHTML = this.responseText;}
          if (this.status == 404) {elmnt.innerHTML = "Page not found.";}
          
          /*remove the attribute, and call this function once more:*/
          elmnt.removeAttribute("w3-include-html");

          includeHTML();
        }
      }
      
      //console.log ('Including '+file);
      
      xhttp.open("GET", file, true);
      xhttp.send();
      /*exit the function:*/
      return;
    }
  }
};

function callServer (method, resource, data, callbackFunction) {
  var xhttp = new XMLHttpRequest();


  xhttp.onreadystatechange = function() {
    //if (this.readyState == 4) {
    //  if (this.status == 200) {
        if (callbackFunction != null)
          callbackFunction(this);
    //  }
    //  else if (this.status == 404)
    //    showSnacknar(BPResult.ERROR, 'Resource not found', 5000);
    //  else if (this.status == 0)
     //   showSnacknar(BPResult.ERROR, "Can't connect server", 5000);
    //  else
    //    showSnacknar(BPResult.ERROR, "Error "+this.status, 5000);
    //}
  };

  console.log (method+ " "+resource);

  //xhttp.open(method, " "+__SERVER['url']+"/"+resource, true);
  xhttp.open(method, resource, true);
  xhttp.setRequestHeader ("Content-Type", "application/json");
  xhttp.setRequestHeader ('Client', detectBrowser()/*+'/'+navigator.appVersion*/+' ('+detectPlatform()+')');

  if (data) {
    //console.log ("data = "+data);
    //xhttp.setRequestHeader("Content-Length", data.length);
    xhttp.send(data);
  }
  else
    xhttp.send();
}

