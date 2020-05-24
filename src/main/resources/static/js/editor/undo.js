'use strict';

class UndoRedo {

  constructor() {
    this.history = [];
    this.backup = null;
    this.current = 0;
    this.stackCount = 0; // For nested begin... end
    this.maxDepth = 3;
  }
  
  length () {
    return (this.history.length);
  }
  
  /* Store x without adding to history */
  setBackup (x) {
    this.backup = x;
  }
  
  dump () {
    for (var i=0; i<this.history.length; i++) {
      console.log ('history['+i+'] '+ (i == this.current ? '<-' : ''));
    }
  }

  /* Add x to history */
  push (x)  {
    if (x)
      this.history.push (x);
  }
  
  /* Add backed up item to history */
  pushBackup () {
    this.push (this.backup);
    this.backup = null;
  }
  
  /* Add current version to history, if a version whas stored append it before current */
  setCurrent (x) {
    if (this.stackCount > 0 || !x)
      return;
    //console.log ('[undo] [setCurrent] length = '+this.history.length+' current = '+this.current);
    
    if (this.current < this.history.length - 1)
      this.history.splice (this.current + 1, this.history.length - this.current - 1);
      
    this.pushBackup ();
    this.push (x);
    
    if (this.length() > this.maxDepth) {
      this.history = this.history.slice (1);
    }
      
    this.current = this.history.length - 1;

    console.log ('[undo] [setCurrent] length = '+this.history.length+' current = '+this.current);
  }
  
  begin () {
    this.stackCount ++;
    
    /*if (this.stackCount === 1)
      console.log ('[undo] [begin]');*/
  }
  
  end () { 
    //this.working = false;
    
    if (this.stackCount > 0)
      this.stackCount --;
    
    /*if (this.stackCount === 0)
      console.log ('[undo] [end]');*/
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

