
function GetFullOffset(element){
var rect = element.getBoundingClientRect();
  var offset = {
    //top: element.offsetTop,
    //left: element.offsetLeft,
    top: rect.top,
    left: rect.left,
  };
 /*
  if(element.offsetParent){
    var po = GetFullOffset(element.offsetParent);
    offset.top += po.top;
    offset.left += po.left;
    return offset;
  }
  else*/
    return offset;
}

function getAttachPoint(element){
  var offset = GetFullOffset(element);

  var rect = element.getBoundingClientRect();
  return {
    //x: offset.left + element.offsetWidth / 2,
    //y: offset.top + element.offsetHeight / 2
    x: offset.left + rect.width / 2,
    y: offset.top + rect.height / 2
  };
};

function uuidv4() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

function includeHTML() {
  var z, i, elmnt, file, xhttp;
  /*loop through a collection of all HTML elements:*/
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
     /*
          var i;
          var x = document.getElementsByClassName("title");
          for (i = 0; i < x.length; i++)
            x[i].innerHTML = __SERVER.package.application_name;

          //document.getElementById('version').innerHTML = __SERVER['version'];
          x = document.getElementsByClassName("version");
          for (i = 0; i < x.length; i++)
            x[i].innerHTML = __SERVER.package.version;

          x = document.getElementsByClassName("nodename");
          for (i = 0; i < x.length; i++)
            x[i].innerHTML = __SERVER.system.nodename;

          x = document.getElementsByClassName("username");
          for (i = 0; i < x.length; i++)
            x[i].innerHTML = __SERVER.auth.username;

          if (__SERVER.auth.enabled) {
            var x = document.getElementById("logout_button");

            if (x) {
              x.innerHTML = `<a target="Javascript:void(0);" onclick="window.location = '/logout';" style="cursor:pointer;"><i class="icon i-sign-out w3-text-gray w3-hover-text-orange"></i></a>`;
            }
          }

          //<a href="/bp" class="w3-bar-item w3-button w3-padding"><i class="icon i-project-diagram" style="color:lightslategray; min-width: 1.5em;"></i> Blueprints</a>

          x = document.getElementById("menu");

          for (i=0;i<__SERVER.menu.length; i++) {
            var item = __SERVER.menu[i];
            var a = document.createElement('a');
            a.setAttribute('href', item.href);
            a.classList.add("w3-bar-item");
            a.classList.add("w3-button");
            a.classList.add("w3-padding");
            a.innerHTML = '<i class="icon '+item.icon+'" style="color:lightslategray; min-width: 1.5em;"></i> '+item.label;

            x.appendChild(a);
          }
    */
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

function findGetParameter(parameterName) {
    var result = null,
        tmp = [];
    location.search
        .substr(1)
        .split("&")
        .forEach(function (item) {
          tmp = item.split("=");
          if (tmp[0] === parameterName) result = decodeURIComponent(tmp[1]);
        });
    return result;
}

function detectBrowser()
{
  var ua = navigator.userAgent.match(/(opera|chrome|safari|firefox|msie)\/?\s*(\.?\d+(\.\d+)*)/i), browser;

  if (navigator.userAgent.match(/Trident.*rv[ :]*11\./i)) {
    browser = "MSIE";
  }
  else if (navigator.userAgent.match(/Edge/i)) {
    browser = "Edge";
  }
  else {
    browser = ua[1];
  }

  // Detect version
  var start, end, version = null;

  if (browser === 'Safari')
    start = navigator.userAgent.indexOf('Version') + 8;
  else
    start = navigator.userAgent.indexOf(browser) + browser.length + 1;

  if (start > -1) {
    version = navigator.userAgent.substring(start);
    end = version.indexOf(' ');

    if (end > -1)
      version = version.substring(0, end);
    else
      version = version.substring(0);
  }

  if (version)
    browser += '/'+version;

  //console.log ("[detectBrowser] "+browser);
  return (browser);
}

function detectPlatform()
{
  if (navigator.userAgent.match(/(win64|x64)/i)) {
    return ('Win 64');
  }
  else
    return (navigator.platform);
}

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

const BPResult = {
  SUCCESS: 0,
  WARNING: 1,
  ERROR: 2
}

function showSnacknar(resultId, text, time) {
  // Get the snackbar DIV
  var icon, x = document.getElementById("snackbar");

  switch (resultId) {
    case BPResult.SUCCESS:
      icon = '<i class="icon i-check w3-text-green"></i>';
      break;
    case BPResult.WARNING:
      icon = '<i class="icon i-exclamation-triangle w3-text-orange"></i>';
      break;
    case BPResult.ERROR:
      icon = '<i class="icon i-exclamation w3-text-red"></i>';
      break;
    default:
      break;
  }

  x.innerHTML = '<div class="snackbarRow"><div class="snackbarColumn" style="margin-right:1em;">'+icon+'</div><div class="snackbarColumn">'+text+'</div></div>';
  //console.log ('[snacknar] '+x.innerHTML);

  // Add the "show" class to DIV
  x.className = "show";

  // After 3 seconds, remove the show class from DIV
  setTimeout(function(){ x.className = x.className.replace("show", ""); }, time);
}

function modalShow(text) {
  var modal = document.getElementById('myModal');
  var modalContent = document.getElementsByClassName('modal-content')[0];
  modalContent.innerHTML = text;
  modal.style.display = "block";
}

function modalHide() {
  var modal = document.getElementById('myModal');
  modal.style.display = "none";
}

function setElementZoom(el, zoom, ox, oy) {
  //console.log ('[setZoom] zoom = '+zoom);

  //transformOrigin = [0.5,0.5];
  el = el || instance.getContainer();
  var p = ["webkit", "moz", "ms", "o"],
  s = "scale(" + zoom + ")",
  //oString = (transformOrigin[0] * 100) + "% " + (transformOrigin[1] * 100) + "%";
  oString = ox + "px " + oy+"px";

  for (var i = 0; i < p.length; i++) {
      el.style[p[i] + "Transform"] = s;
      el.style[p[i] + "TransformOrigin"] = oString;
  }

  el.style["transform"] = s;
  el.style["transformOrigin"] = oString;
}

// Restricts input for the given textbox to the given inputFilter.
function setInputFilter(textbox, inputFilter) {
  ["input", "keydown", "keyup", "mousedown", "mouseup", "select", "contextmenu", "drop"].forEach(function(event) {
    textbox.addEventListener(event, function() {
      if (inputFilter(this.value)) {
        //console.log (this.value + ' is OK');
        this.oldValue = this.value;
        //this.oldSelectionStart = this.selectionStart;
        //this.oldSelectionEnd = this.selectionEnd;
      } else if (this.hasOwnProperty("oldValue")) {
        //console.log (this.value + 'is not OK, come back to '+this.oldValue);
        this.value = this.oldValue;
        //this.setSelectionRange(this.oldSelectionStart, this.oldSelectionEnd);
      }

      //console.log ('value = '+ (this.value === "" ? 'empty' : this.value));

      /* Check null */
      /*if (this.value === "")
        this.classList.add('inputNull');
      else
        this.classList.remove('inputNull');*/
    });
  });
}

function copyStringToClipboard (str) {
   // Create new element
   var el = document.createElement('textarea');
   // Set value (string to be copied)
   el.value = str;
   // Set non-editable to avoid focus and move outside of view
   el.setAttribute('readonly', '');
   el.style = {position: 'absolute', left: '-9999px'};
   document.body.appendChild(el);
   // Select text inside element
   el.select();
   // Copy text to clipboard
   document.execCommand('copy');
   // Remove temporary element
   document.body.removeChild(el);
}

function dateFromISO8601(isostr) {
    var parts = isostr.match(/\d+/g);
    return new Date(parts[0], parts[1] - 1, parts[2], parts[3], parts[4], parts[5]);
}

function secondsToString (seconds) {
  var s;
  var t = new Date(seconds * 1000);

  s = t.getFullYear()+'-'+(t.getMonth()+1).toString().padStart(2,'0')+'-'+t.getDate().toString().padStart(2,'0')+' '+
      t.getHours().toString().padStart(2,'0') +':'+ t.getMinutes().toString().padStart(2,'0') +':'+ t.getSeconds().toString().padStart(2,'0');

  return (s);
}

function secondsToDay (seconds) {
  var s;
  var t = new Date(seconds * 1000);

  s = t.getFullYear()+'-'+(t.getMonth()+1).toString().padStart(2,'0')+'-'+t.getDate().toString().padStart(2,'0');

  return (s);
}

function secondsToTime (seconds) {
  var s;
  var t = new Date(seconds * 1000);

  s = t.getHours().toString().padStart(2,'0') +':'+ t.getMinutes().toString().padStart(2,'0') +':'+ t.getSeconds().toString().padStart(2,'0');

  return (s);
}

function colourNameToHex(colour)
{
    var colours = {"aliceblue":"#f0f8ff","antiquewhite":"#faebd7","aqua":"#00ffff","aquamarine":"#7fffd4","azure":"#f0ffff",
    "beige":"#f5f5dc","bisque":"#ffe4c4","black":"#000000","blanchedalmond":"#ffebcd","blue":"#0000ff","blueviolet":"#8a2be2","brown":"#a52a2a","burlywood":"#deb887",
    "cadetblue":"#5f9ea0","chartreuse":"#7fff00","chocolate":"#d2691e","coral":"#ff7f50","cornflowerblue":"#6495ed","cornsilk":"#fff8dc","crimson":"#dc143c","cyan":"#00ffff",
    "darkblue":"#00008b","darkcyan":"#008b8b","darkgoldenrod":"#b8860b","darkgray":"#a9a9a9","darkgreen":"#006400","darkkhaki":"#bdb76b","darkmagenta":"#8b008b","darkolivegreen":"#556b2f",
    "darkorange":"#ff8c00","darkorchid":"#9932cc","darkred":"#8b0000","darksalmon":"#e9967a","darkseagreen":"#8fbc8f","darkslateblue":"#483d8b","darkslategray":"#2f4f4f","darkturquoise":"#00ced1",
    "darkviolet":"#9400d3","deeppink":"#ff1493","deepskyblue":"#00bfff","dimgray":"#696969","dodgerblue":"#1e90ff",
    "firebrick":"#b22222","floralwhite":"#fffaf0","forestgreen":"#228b22","fuchsia":"#ff00ff",
    "gainsboro":"#dcdcdc","ghostwhite":"#f8f8ff","gold":"#ffd700","goldenrod":"#daa520","gray":"#808080","green":"#008000","greenyellow":"#adff2f",
    "honeydew":"#f0fff0","hotpink":"#ff69b4",
    "indianred ":"#cd5c5c","indigo":"#4b0082","ivory":"#fffff0","khaki":"#f0e68c",
    "lavender":"#e6e6fa","lavenderblush":"#fff0f5","lawngreen":"#7cfc00","lemonchiffon":"#fffacd","lightblue":"#add8e6","lightcoral":"#f08080","lightcyan":"#e0ffff","lightgoldenrodyellow":"#fafad2",
    "lightgrey":"#d3d3d3","lightgreen":"#90ee90","lightpink":"#ffb6c1","lightsalmon":"#ffa07a","lightseagreen":"#20b2aa","lightskyblue":"#87cefa","lightslategray":"#778899","lightsteelblue":"#b0c4de",
    "lightyellow":"#ffffe0","lime":"#00ff00","limegreen":"#32cd32","linen":"#faf0e6",
    "magenta":"#ff00ff","maroon":"#800000","mediumaquamarine":"#66cdaa","mediumblue":"#0000cd","mediumorchid":"#ba55d3","mediumpurple":"#9370d8","mediumseagreen":"#3cb371","mediumslateblue":"#7b68ee",
    "mediumspringgreen":"#00fa9a","mediumturquoise":"#48d1cc","mediumvioletred":"#c71585","midnightblue":"#191970","mintcream":"#f5fffa","mistyrose":"#ffe4e1","moccasin":"#ffe4b5",
    "navajowhite":"#ffdead","navy":"#000080",
    "oldlace":"#fdf5e6","olive":"#808000","olivedrab":"#6b8e23","orange":"#ffa500","orangered":"#ff4500","orchid":"#da70d6",
    "palegoldenrod":"#eee8aa","palegreen":"#98fb98","paleturquoise":"#afeeee","palevioletred":"#d87093","papayawhip":"#ffefd5","peachpuff":"#ffdab9","peru":"#cd853f","pink":"#ffc0cb","plum":"#dda0dd","powderblue":"#b0e0e6","purple":"#800080",
    "rebeccapurple":"#663399","red":"#ff0000","rosybrown":"#bc8f8f","royalblue":"#4169e1",
    "saddlebrown":"#8b4513","salmon":"#fa8072","sandybrown":"#f4a460","seagreen":"#2e8b57","seashell":"#fff5ee","sienna":"#a0522d","silver":"#c0c0c0","skyblue":"#87ceeb","slateblue":"#6a5acd","slategray":"#708090","snow":"#fffafa","springgreen":"#00ff7f","steelblue":"#4682b4",
    "tan":"#d2b48c","teal":"#008080","thistle":"#d8bfd8","tomato":"#ff6347","turquoise":"#40e0d0",
    "violet":"#ee82ee",
    "wheat":"#f5deb3","white":"#ffffff","whitesmoke":"#f5f5f5",
    "yellow":"#ffff00","yellowgreen":"#9acd32"};

    if (typeof colours[colour.toLowerCase()] != 'undefined')
        return colours[colour.toLowerCase()];

    return false;
}

function stringForJson (s) {
  return (s.replace (/\\/g,'\\\\').replace (/"/g,'\\"').replace (/\n/g,'\\n'));
}

function lightenColor(color, percent) {
  var num = parseInt(color.replace("#",""),16);

  amt = Math.round(2.55 * percent),
  R = (num >> 16) + amt,
  B = (num >> 8 & 0x00FF) + amt,
  G = (num & 0x0000FF) + amt;

  return "#" + (0x1000000 + (R<255?R<1?0:R:255)*0x10000 + (B<255?B<1?0:B:255)*0x100 + (G<255?G<1?0:G:255)).toString(16).slice(1);
};
