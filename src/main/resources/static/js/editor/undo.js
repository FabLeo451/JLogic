'use strict';

class UndoRedo {

  constructor() {
    this.history = [];
    //this.backup = null;
    this.current = 0;
    this.stackCount = 0; // For nested begin... end
    this.maxDepth = 30;
  }
  
  length () {
    return (this.history.length);
  }
  
  /* Store x without adding to history */
  /*
  setBackup (x) {
    this.backup = x;
  }*/
  
  setMaxDepth(n) {
    if (n > 0)
      this.maxDepth = n;
  }
  
  dump () {
    var s;
    
    console.log ('History max depth: '+this.maxDepth);
    
    for (var i=0; i<this.history.length; i++) {
      s = this.history[i].tag+" "+this.history[i].name+(i == this.current ? ' <-' : '');
      console.log ('history['+i+'] '+s);
    }
  }

  /* Add x to history */
  push (x)  {
    if (x)
      this.history.push (x);
  }
  
  /* Add backed up item to history */
  /*
  pushBackup () {
    this.push (this.backup);
    this.backup = null;
  }*/
  
  /* Add current version to history, if a version was stored append it before current */
  setCurrent (x) {
    if (this.stackCount > 0 || !x)
      return;
      
    //console.log ('[undo] Adding to history...');
    
    if (this.current < this.history.length - 1)
      this.history.splice (this.current + 1, this.history.length - this.current - 1);
      
    //this.pushBackup ();
    this.push (x);
    
    if (this.length() > this.maxDepth) {
      this.history = this.history.slice (1);
    }
      
    this.current = this.history.length - 1;

    //console.log ('[undo] [setCurrent] length = '+this.history.length+' current = '+this.current);
  }
  
  begin () {
    this.stackCount ++;
    //console.log("[undo] stackCount = "+this.stackCount);
  }
  
  end () { 
    if (this.stackCount > 0)
      this.stackCount --;
      
    //console.log("[undo] stackCount = "+this.stackCount);
  }
  
  undo () {
    if (this.current == 0)
      return (null);
      
    this.current --;
    return (this.history[this.current]);
  }
  
  redo () {
    if (this.current == this.history.length - 1)
      return (null);
      
    this.current ++;
    return (this.history[this.current]);
  }
}

