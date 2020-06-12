
var contextMenu;

function activate_ctx_item_cb(id, data)
{
  console.log ("[activate_ctx_item_cb] "+id+" "+data);
  contextMenu.remove();
  
  if (contextMenu.callback)
    contextMenu.callback (/*contextMenu.x, contextMenu.y,*/ id, data);
}

function setVisibility (menu, show)
{
  menu.style.display = show === true ? "block" : "none";
};

function getVisibility (menu)
{
  return (menu.style.display == "block" ? true : false);
};

function setPosition (menu, left, top)
{
  menu.style.left = `${left}px`;
  menu.style.top = `${top}px`;
};

function ctxMenuSearch () {
  var filter, ul, li, i;
  
  console.log ("search()");
  
  input = document.getElementById("mySearch");
  filter = input.value.toUpperCase();
  
  //console.log (filter);
  
  ul = document.getElementById("objects");
  li = ul.getElementsByTagName("li");

  for (i = 0; i < li.length; i++) {
    if (li[i].innerHTML.toUpperCase().indexOf(filter) > -1) {
      li[i].style.display = "";
    } else {
      li[i].style.display = "none";
    }
  }
}

function ContextMenu () {
	var ul, input;
	var x, y;
	var searchEnabled;
	
  this.menu = null;
  
  this.setCallback = function (f) {
    this.callback = f;
  };

  this.show = function(x, y) {
  
    console.log("window.innerWidth = "+window.innerWidth);
    console.log("clientWidth = "+this.menu.parentNode.clientWidth);
  
    //setPosition(this.menu, x, y);
    setVisibility(this.menu, true);
    
    const w = window.innerWidth;
    const h = window.innerHeight;

    const mw = this.menu.offsetWidth;
    const mh = this.menu.offsetHeight;
    
    console.log("menu.offsetWidth = "+this.menu.offsetWidth);

    if (x + mw > w) { x = x - mw; }
    if (y + mh > h) { y = y - mh; }

    this.menu.style.left = x + 'px';
    this.menu.style.top = y + 'px';
    
    console.log("Showing context menu in ("+this.menu.style.left+", "+this.menu.style.top+") searchEnabled = "+this.searchEnabled);
    
    this.x = x;
    this.y = y;
    
    contextMenu = this; /* Global object */
    
    if (this.searchEnabled)
      document.getElementById("mySearch").focus();
    
  };
  
  this.remove = function() {
    //var element = document.getElementById("element-id");
    
    if (this.menu) {
      this.menu.parentNode.removeChild(this.menu);
      this.menu = null;
    }
    
    
  }

  this.activateItem = function() {
      console.log ("activateItem()");
      this.remove();
  };

  this.init = function() {
    this.searchEnabled = false;
  };

  this.enableSearch = function(enabled) {
    this.searchEnabled = enabled;
  };

  this.createFromJson = function(j) {
    var li;
    this.menu = document.createElement('div');
    this.menu.className = "menu";
    //this.menu.setAttribute("onclick", "this.activateItem");
    
    //console.log ("searchEnabled = "+this.searchEnabled);
    
    if (this.searchEnabled) {
      this.searchElem = document.createElement('div');
      this.searchElem.innerHTML = '<input class="ctxSearchEntry" type="text" id="mySearch" onkeyup="ctxMenuSearch();" placeholder="Search..." title="Type in a category">';
      this.menu.appendChild (this.searchElem);
    }

    this.menuContent = document.createElement('div');
    this.menuContent.className = "context-menu-content";
    this.menu.appendChild (this.menuContent);
    
    this.ul = document.createElement('ul');
    this.ul.className = "menu-options";
    this.ul.id = "objects";
    //this.menu.appendChild (this.ul);
    this.menuContent.appendChild (this.ul);
    
    for (var i = 0; i < j["items"].length; i++) {
      item = j["items"][i];
      
      if (item["item"] == "separator") {
        const hr = document.createElement('hr');
        hr.className = "menu";
        this.ul.appendChild (hr);
      }
      else {
        li = document.createElement('li');
        li.className = "menu-option";
        li.innerHTML = (item.hasOwnProperty("icon") ? item.icon+' ' : '')+item.item;
        //li.setAttribute("onclick", item["callback"]);
        
        if (item.hasOwnProperty("action"))
          li.addEventListener('click', item.action);
        else
          li.setAttribute("onclick", "activate_ctx_item_cb("+item["id"]+", '"+item["data"]+"')");
        
        this.ul.appendChild (li);
      }
    }
    
    document.getElementsByTagName('body')[0].appendChild(this.menu);
              
    //contextMenu = this;
    
    //console.log ("createFromJson()");
  };
  
  this.setMinWidth = function (w) {
    this.menu.style.minWidth=w+"px";
  };
  
  this.setWidthAuto = function () {
    this.menu.style.width="auto";
  };
}

