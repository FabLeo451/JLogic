var menubar;

const ActionKey = {
  F1: 112, F2: 113, F3: 114, F4: 115, F5: 116, F6: 117,
  F7: 118, F8: 119, F9: 120, F10: 121, F11: 122, F12: 123
};

class Action {

  constructor() {
    this.id = 0;
    this.ctrl = false;
    this.alt = false;
    this.shift = false;
    this.keypressed = false;
    this.keyreleased = true;
    this.key = 0;
    this.callback = null;
  }
  
  setCallback (f) {
     this.callback = f;
  }
  
  toString () {
    var k;
    
    if ((this.key >= '0'.charCodeAt(0) && this.key <= '9'.charCodeAt(0)) || // numeric (0-9)
        //(this.key >= 'a'.charCodeAt(0) && this.key <= 'z'.charCodeAt(0)) || // upper alpha (A-Z)
        (this.key >= 'A'.charCodeAt(0) && this.key <= 'Z'.charCodeAt(0))) {
      k = String.fromCharCode(this.key).toUpperCase();
    }
    else {
      switch (this.key) {
        case 46:
          k = "Canc";
          break;
        default:
          if (this.key >= 112 && this.key <= 123)
            k = "F"+(this.key - 111);
          else
            k = String.fromCharCode(this.key);
          break;
      }
    }
    
    //console.log("[toString] "+this.key+" "+k);

    return ((this.ctrl ? "Ctrl+" : "") + (this.alt ? "Alt+" : "") + (this.shift ? "Shift+" : "") + k);
  }
}

class Menubar {

  constructor() {
    this.menubarElement = null;
    this.toolbarElement = null;
    this.dropdownList = [ ];
    this.actions = [ ];
    this.active = false;
    this.actionCallback = null;
  }
  
  init (id) {
    this.menubarElement = document.getElementById(id);  
  }
  
  setActionCallback (cb) { this.actionCallback = cb; }
  
  hideAll () {
    for (var i=0; i<this.dropdownList.length; i++) {
      //if (this.dropdownList[i].classList.contains('show')) {
        this.dropdownList[i].classList.remove('show');
      //}
    }
  }
  /*
  showItems (e) {
    console.log("[showItems] "+e.target.id+" menubar.active = "+ (menubar.active ? "true" : "false"));
    this.hideAll();
    
    if (this.active) {
      var id = e.target.id.replace ("dropbtn", "dropdown");
      document.getElementById(id).classList.toggle("show");
    }
  }
  */
  showItems (e) {
    console.log("[showItems] "+e.tagName+" "+e.id+" menubar.active = "+ (menubar.active ? "true" : "false"));
    this.hideAll();
    
    if (this.active) {
      var id = e.id.replace ("dropbtn", "dropdown");
      console.log("[showItems] Showing "+id);
      document.getElementById(id).classList.add("show");
    }
  }
  
  activate (active) { 
    this.active = active; 
    
    if (!active)
      this.hideAll ();
  }
  
  getAction (id) {
    for (var i = 0; i < this.actions.length; i++)
      if (this.actions[i].id == id)
        return (this.actions[i]);
      
    return (null);
  }
  
  matchAction (key, ctrl, alt, shift) {
    for (var i = 0; i < this.actions.length; i++) {
      if (this.actions[i].key == key &&
          this.actions[i].ctrl == ctrl &&
          this.actions[i].alt == alt &&
          this.actions[i].shift == shift)
        return (this.actions[i]);
    }
    
    return (null);
  }
  
  fromJson (j) {
    this.actions.length = 0;
    var jactions = j["actions"];
    
    /* Load actions */
    for (var i = 0; i < jactions.length; i++) {
      var action = jactions[i];
      var a = new Action();
      
      a.id = action["id"];
      a.ctrl = action["ctrl"] != null ? action["ctrl"] : false;
      a.alt = action["alt"] != null ? action["alt"] : false;
      a.shift = action["shift"] != null ? action["shift"] : false;
      a.keypressed = action["keypressed"] != null ? action["keypressed"] : a.keypressed;
      a.keyreleased = action["keyreleased"] != null ? action["keyreleased"] : a.keyreleased;
      a.key =  (action["key"] >= '0' && action["key"] <= '9') || 
               (action["key"] >= 'a' && action["key"] <= 'z') || 
               (action["key"] >= 'A' && action["key"] <= 'Z') ||
               (action["key"] == '+') ? action["key"].charCodeAt(0) : action["key"];
      a.setCallback(this.actionCallback);
      
      //console.log("Adding action "+a.key+" "+a.toString());
      
      this.actions.push (a);
    }
    
    /* Load menu */
    var menubar = j["menubar"];
    
    for (var i = 0; i < menubar.length; i++) {
      var menu = menubar[i];
      //console.log(JSON.stringify(menu));
      var name = menu["menu"];
      var items = menu["items"];
      //console.log("name = "+name);
      
      var menuElement = document.createElement('div');
      menuElement.className = "dropdown";
      
      var buttonElement = document.createElement('div');
      buttonElement.className = "dropbtn";
      buttonElement.innerHTML = name;
      
      var menuSelf = this;
      
      //buttonElement.onclick = mbActivate;
      buttonElement.onclick = function (e) {
        e.preventDefault();
        console.log("[onclick] "+e.currentTarget.id);
        menuSelf.activate (true);
        menuSelf.showItems (e.currentTarget);
      }
      
      buttonElement.onmouseenter = function (e) {
        //console.log("[showItems] "+e.target.id+" menubar.active = "+ (menubar.active ? "true" : "false"));
        menuSelf.hideAll();
        
        if (menuSelf.active) {
          var id = e.target.id.replace ("dropbtn", "dropdown");
          document.getElementById(id).classList.toggle("show");
        }
      }
      
      buttonElement.setAttribute('id', 'dropbtn_'+i);
      menuElement.appendChild(buttonElement);
      
      var contentElement = document.createElement('div');
      contentElement.className = "dropdown-content";
      contentElement.setAttribute('id', 'dropdown_'+i);
      menuElement.appendChild(contentElement);
      this.dropdownList.push (contentElement);

      this.menubarElement.appendChild(menuElement);

      for (var k = 0; k < items.length; k++) {
        var item = items[k];        
        var itemElement;
        var shortcut, icon;
        
        if (items[k]["separator"]) {
          itemElement = document.createElement('hr');
          itemElement.style.margin = "2px";
        }
        else {
          itemElement = document.createElement('div');
          itemElement.className = "menuitem";
          itemElement.setAttribute('id', 'menuitem_'+item["id"]);
          
          if (item["text"])
            shortcut = item["text"];
          else
            shortcut = this.getAction(item["id"]) ? this.getAction(item["id"]).toString() : "";
            
          if (item["checked"])
            icon = '<i class="icon i-check"></i>';
          else
            icon = item["icon"] ? item["icon"] : "";
            
          itemElement.innerHTML = '<span class="menuitem-icon">'+icon+'</span><span class="menuitem-label">'+item["item"]+'</span><span class="menuitem-shortcut">'+shortcut+'</span>';
          
          //itemElement.onclick = mbItemSelected;
          itemElement.onclick = function(e) {
            //console.log("[onclick] id="+e.currentTarget.id+ " class="+e.currentTarget.class+" tag="+e.currentTarget.tagName);
            menuSelf.activate(false);
            var actionID = e.currentTarget.id.replace ("menuitem_", "");
            console.log("[item onclick] actionID = "+actionID);
            
            var a = menuSelf.getAction(actionID);
            
            if (a) {
              a.callback(a);
            }
          }
        }
        
        contentElement.appendChild(itemElement);
      }
    }
    
    /* Load toolbar */
    
    this.toolbarElement = document.getElementById('toolbar');
    //this.menubarElement.appendChild(this.toolbarElement);
    
    var toolbar = j["toolbar"];
    
    for (var i = 0; i < toolbar.length; i++) {
      if (toolbar[i]["separator"]) {
        var sep = document.createElement('div');
        sep.className = "tbSeparator";
        this.toolbarElement.appendChild(sep);
      }
      else {
        var tbButton = document.createElement('button');
        tbButton.className = "tbBtn";
        tbButton.setAttribute('_actionid', toolbar[i].id);
        tbButton.innerHTML = '<div style="padding-bottom:4px">'+toolbar[i].icon+'</div><div>'+toolbar[i].item+'</div>';
        this.toolbarElement.appendChild(tbButton);
        
        tbButton.onclick = function(e) {
          var actionID = e.currentTarget.getAttribute('_actionid');
          var a = menuSelf.getAction(actionID);
          
          if (a) {
            a.callback(a);
          }
        }
      }
    }
  }
  
  getMenuItem (id) {
    return (document.getElementById("menuitem_"+id));
  }
}

