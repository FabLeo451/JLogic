
class Edge {

  constructor() {
    this.start = { x:0, y:0 };
    this.end = { x:0, y:0 };
    this.color = "green";
    this.connector1 = null;
    this.connector2 = null;
    this.zoom = 1;
    this.callbackMouseEnter = null;
    this.callbackMouseLeave = null;
  }
  
  createPath (a, b){
    var diff = {
      x: b.x - a.x,
      y: b.y - a.y
    };
    
    /* Initial setting */
    var c1 = { x: a.x + diff.x / 3 * 2, y: a.y };
    var c2 = { x: a.x + diff.x / 3, y: b.y };
    
    //c1.x = (Math.abs(diff.x) > Math.abs(diff.y)) ? (a.x + b.x) / 2 : b.x;
    c1.y = a.y;
    //c2.x = (Math.abs(diff.x) > Math.abs(diff.y)) ? c1.x : a.x;
    c2.y = b.y;
    /*
    if (a.x > b.x) {
      c1.x = a.x + Math.abs(diff.x);
      c2.x = b.x - Math.abs(diff.x);
    }*/
    var bezierWeight = 0.675;
    //var bezierWeight = diff.x > 20 ? 0.675 : 2;
    //console.log (diff.x+' '+bezierWeight);
    var dx = Math.abs(b.x - a.x) * bezierWeight;
    
    c1.x = a.x + dx;
    c2.x = b.x - dx;
    
    if (Math.abs(diff.x) < 100 && diff.x < diff.y) {
      c1.x += 100 * (1 - Math.abs(diff.x) / 100);
      c2.x -= 100 * (1 - Math.abs(diff.x) / 100);
    }
  
    var pathStr = 'M' + a.x + ',' + a.y + ' ';
    pathStr += 'C';
    pathStr += c1.x + ',' + c1.y + ' ';
    pathStr += c2.x + ',' + c2.y + ' ';
    pathStr += b.x + ',' + b.y;
    
    return pathStr;
  }
  
  getPath () { return (this.path); }
  setLocked (b) { this.locked = b; }
  getLocked () { return (this.locked); }

  setCallbackMouseEnter (f) {
    this.callbackMouseEnter = f;
  }
  
  setCallbackMouseLeave (f) {
    this.callbackMouseLeave = f;
  }
      
  setColor (color) {
    this.color = color;
    this.path.setAttributeNS(null, 'stroke', this.color);
  }
  
  darken () {
    var hex = colourNameToHex(this.color);
    
    if (hex) {
      this.path.style.stroke = lightenColor(hex, -20);
      //console.log ('color = '+this.style.stroke);
    }
  }
  
  lighten () {
    var hex = colourNameToHex(this.color);
    
    if (hex) {
      this.path.style.stroke = lightenColor(hex, 30);
      //console.log ('color = '+this.style.stroke);
    }
  }
  
  toNormalColor () {
    this.path.style.stroke = this.color;
  }
  
  create (svg, a, b) {
    this.start = a;
    this.end = b;
    
    this.path = document.createElementNS(svg.ns, 'path');
    
    this.path.setAttributeNS(null, 'stroke-width', 3*this.zoom);
    this.path.setAttributeNS(null, 'fill', 'none');
    this.path.setAttributeNS(null, 'stroke', this.color);
    
    this.setPoint1 (a);
    this.setPoint2 (b);
    /*
    var pathStr = this.createPath(a, b);
    this.path.setAttributeNS(null, 'd', pathStr);
    */
    
    var self = this;
    
    this.path.onmouseenter = function (ev) {
      //console.log ("callbackMouseEnter ="+this.callbackMouseEnter);
      if (self.callbackMouseEnter)
        self.callbackMouseEnter (self);
    }
    
    this.path.onmouseleave = function (ev) {
      //this.style.stroke = self.color;
      if (self.callbackMouseLeave)
        self.callbackMouseLeave (self);
    }
        
    //console.log ("pathStr ="+pathStr);
    svg.appendChild(this.path);
  }
  
  remove () {
    this.path.parentElement.removeChild(this.path);
  }

  setConnector1 (c) {
    this.connector1 = c;
  }
  
  setConnector2 (c) {
    this.connector2 = c;
  }
  
  setPoint1 (p) {
    if (this.getLocked ())
      return;

    //p.x *= this.zoom;
    //p.y *= this.zoom;
    
    var pathStr = this.createPath(p, this.end);
    this.path.setAttributeNS(null, 'd', pathStr);
    this.start = p;
  }
  
  setPoint2 (p) {
    if (this.getLocked ())
      return;
      
    //p.x *= this.zoom;
    //p.y *= this.zoom;
          
    var pathStr = this.createPath(this.start, p);
    this.path.setAttributeNS(null, 'd', pathStr);
    this.end = p;
  }
  
  setTrackingPoint (p) {
    if (this.getLocked ())
      return;

    //p.x *= this.zoom;
    //p.y *= this.zoom;
          
    var pStart, pEnd;
    pStart = this.connector1.direction == BPDirection.OUTPUT ? this.start : p;
    pEnd = this.connector1.direction == BPDirection.OUTPUT ? p : this.start;
    
    var pathStr = this.createPath(pStart, pEnd);
    this.path.setAttributeNS(null, 'd', pathStr);
    this.end = p;
  }
  
  redraw () {
    //console.log ("Redrawing edge between "+this.connector1.id+" and "+this.connector2.id);
    //console.log ("Connector 1 coords:  "+this.connector1.getConnectionPoint ().x+" "+this.connector1.getConnectionPoint ().y);
    var cOut = this.connector1.direction == BPDirection.OUTPUT ? this.connector1 : this.connector2;
    var cIn = this.connector1.direction == BPDirection.INPUT ? this.connector1 : this.connector2;
    /*
    var pOut = cOut.getConnectionPoint ();
    pOut.x *= this.zoom;
    pOut.y *= this.zoom;
    
    var pIn = cIn.getConnectionPoint ();
    pIn.x *= this.zoom;
    pIn.y *= this.zoom;
    
    this.setPoint1 (pOut);
    this.setPoint2 (pIn);
    */
    this.setPoint1 (cOut.getConnectionPoint ());
    this.setPoint2 (cIn.getConnectionPoint ());
        
    this.path.setAttributeNS(null, 'stroke-width', 3*this.zoom);
  }
  
  setZoom (z) {
    this.zoom = z;
  }
  
  toString () {
    var s, from, to;
    from = this.connector1.direction == BPDirection.OUTPUT ? this.connector1.id : this.connector2.id;
    to = this.connector1.direction == BPDirection.OUTPUT ? this.connector2.id : this.connector1.id;
    s = '{ "from": '+from+', "to": '+to+' }';
    
    return (s);
  }
};

