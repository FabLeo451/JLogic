
const BPConsoleTextType = {
  SUCCESS: 0,
  WARNING: 1,
  ERROR:3
};

class BPConsole {
  constructor() {
    this.consoleElem = null;
    this.toolbarElem = null;
    this.textElem = null;
    this.visible = true;
  }
  
  linkToDiv (id) {
    this.consoleElem = document.getElementById(id);
    this.toolbarElem = this.consoleElem.getElementsByClassName("bpconsoleToolbar")[0];
    this.textElem = this.consoleElem.getElementsByClassName("terminal")[0];
  }
  
  getVisible () { return (this.visible); }
  
  setVisible (visible) {
    console.log ("setVisible "+visible);
    this.consoleElem.style.display = visible ? 'block' : 'none';
    this.visible = visible;
  }
  
  toggleVisibility () {
    this.setVisible (!this.visible);
    return (this.visible);
  }
  
  clear () {
    this.textElem.innerHTML = '';
  }
  
  append (text, type) {
    var color;
    
    switch (type) {
      case BPConsoleTextType.SUCCESS:
        color = 'style="color:green;"';
        break;
      case BPConsoleTextType.WARNING:
        color = 'style="color:darkorange;"';
        break;
      case BPConsoleTextType.ERROR:
        color = 'style="color:red;"';
        break;
      default:
        break;
    }
    
    /*
    var c, p, lines = text.match(/[^\r\n]+/g);
    
    for (var i=0; i<lines.length; i++) {
      p = lines[i];
      
      if (p.includes ("[TRACE] "))
        c = 'style="color:darkorange; font-weight:bold;"';
      else
        c = color;
        
      p = '<span '+c+'>'+lines[i]+'</span>\n';
      
      this.textElem.innerHTML += p;      
    }
    */
    var p = '<span '+color+'>'+text+'</span>\n';
    this.textElem.innerHTML += p;
    
    /* Scroll to bottom */
    this.textElem.scrollTop = this.textElem.scrollHeight;
  }
}
