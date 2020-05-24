

function refreshStatsCallback (xhttp) {

  if (xhttp.readyState == 4) { // Request finished and response is ready
    
    if (xhttp.status == 200) {
    
      var data = xhttp.responseText;
      var jsonResponse = JSON.parse(data);
      
      var item, text;
      var offsetX=90, width=705, heigth=250, offsetY=heigth;
      var i, x, y, lastX, lastY, label;
      var dx = Math.round(width / jsonResponse.items.length);
      //var dy = Math.round(heigth / jsonResponse.items.length);

      var graph = document.getElementById("_graph");
      
      var l = graph.getElementsByClassName("line");

      while (l.length) {
        graph.removeChild(l[0]);
        l = graph.getElementsByClassName("line");
      }
        
      l = document.getElementById("_x_labels").getElementsByClassName("x_label");

      while (l.length) {
        document.getElementById("_x_labels").removeChild(l[0]);
        l = document.getElementById("_x_labels").getElementsByClassName("x_label");
      }
        
      l = document.getElementById("_y_labels").getElementsByClassName("y_label");

      while (l.length) {
        document.getElementById("_y_labels").removeChild(l[0]);
        l = document.getElementById("_y_labels").getElementsByClassName("y_label");
      }
        
    
      /* Access */
      document.getElementById("_from").innerHTML = secondsToString (jsonResponse.from);
      document.getElementById("_to").innerHTML = secondsToString (jsonResponse.to);
      document.getElementById("_total").innerHTML = jsonResponse.total;
      
      var average = jsonResponse.total / jsonResponse.items.length;
      document.getElementById("_average").innerHTML = average.toFixed(1) + " / " +jsonResponse.unit;
      
      /* Memory */
      document.getElementById("_totalram").innerHTML = jsonResponse.totalram + ' KB';
      document.getElementById("_freeram").innerHTML = jsonResponse.freeram + ' KB';
      document.getElementById("_usedmem").innerHTML = (((jsonResponse.totalram - jsonResponse.freeram) / jsonResponse.totalram) * 100).toFixed(1) + '%';
      
      /*document.getElementById("_ly4").innerHTML = jsonResponse.max;
      document.getElementById("_ly3").innerHTML = Math.floor(jsonResponse.max * 0.75);
      document.getElementById("_ly2").innerHTML = Math.floor(jsonResponse.max * 0.25);*/
      
      var nLabels = 5;
      
      for (i=0; i<nLabels; i++) {
        text = document.createElementNS('http://www.w3.org/2000/svg','text');
        text.classList.add('y_label');
        text.setAttribute('x', '80');
        text.setAttribute('y', 10 + offsetY - ((i/(nLabels-1)) * heigth));
        label = Math.trunc((i/(nLabels-1)) * jsonResponse.max);
        text.innerHTML = label;
        document.getElementById("_y_labels").append(text);     
      }
      
      for (i=0; i<nLabels; i++) {
        text = document.createElementNS('http://www.w3.org/2000/svg','text');
        text.classList.add('x_label');
        text.setAttribute('x',10 + offsetX + (width * (i/nLabels)));
        text.setAttribute('y',heigth + 20);
        label = secondsToString (jsonResponse.from + ((jsonResponse.to - jsonResponse.from) * (i/nLabels)));
        label = label.substr(label.indexOf(' ')+1);
        label = label.substr(0, label.lastIndexOf(':'));
        text.innerHTML = label;
        document.getElementById("_x_labels").append(text);     
      }

      
      for (i=0; i<jsonResponse.items.length; i++) {
        item = jsonResponse.items[i];
        
        x = offsetX + i * dx;
        y = offsetY - ((item.count / jsonResponse.max) * heigth);
        
        //console.log (item.timestamp +' '+item.count +' ('+x+','+y+')');
       
        if (i > 0) {
          newLine = document.createElementNS('http://www.w3.org/2000/svg','line');
          newLine.classList.add('line');
          newLine.setAttribute('x1',lastX);
          newLine.setAttribute('y1',lastY);
          newLine.setAttribute('x2',x);
          newLine.setAttribute('y2',y);
          newLine.setAttribute("stroke", "#0074d9");
          newLine.setAttribute("stroke-width", "2");
          graph.append(newLine);
        }
    
        lastX = x;
        lastY = y;
      }
      
      //console.log ('Lines drawn: '+(jsonResponse.items.length - 1));
      
      l = graph.getElementsByClassName("line");
      //console.log ('Lines: '+l.length);
    }
    else {
      //document.getElementById('graph').innerHTML = 'Error '+xhttp.status+' '+xhttp.statusText;
      showSnacknar(BPResult.ERROR, "Can't get data from server", 3000);
    }
  }
  else if (this.readyState == 3) { // Processing request 
    //document.getElementById('graph').innerHTML = "Getting blueprints...";
  }
  else {
    //document.getElementById('graph').innerHTML = xhttp.statusText;
  }
}

function refreshStats() {
  //document.getElementById("sessions").innerHTML =  '<div class="loader"></div>Refreshing...';
  callServer ("GET", "/stats?json=1", null, refreshStatsCallback);
}

