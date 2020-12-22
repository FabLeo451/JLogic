function buildTOC(container, jsections, prefix, depth) {
    if (!container)
        container = document.getElementById("toc");

    if (!depth)
        depth = 0;

    if (!prefix)
        prefix = null;
        
    prefix = prefix ? prefix : '';
        
    t = 1;
        
    for (var i=0; i<jsections.length; i++) {
        elem = document.createElement('div');
        elem.style.marginLeft = depth + "em";
        elem.innerHTML = '<a href="'+jsections[i].url+'">'+prefix + t + ' ' + jsections[i].title+'</a>';
        container.appendChild(elem);
        
        if (jsections[i].hasOwnProperty("children")) {
            buildTOC(container, jsections[i].children, prefix + t + '.', depth+1);
        }
        
        t ++;
    }
}

function toc(id) {
    jexample = [
        { "title": "Nodes and connectors", "url":"nodes.html"},
        { "title": "Blueprints", "url":"blueprints.html", "children": [
                { "title": "The Main blueprint", "url":"blueprints.html#main"},
                { "title": "The Event blueprint", "url":"blueprints.html#events"}
            ]
        },
        { "title": "Programs", "url":"programs.html"},
        { "title": "Flow control", "url":"flow.html"}
    ];
    
    buildTOC(document.getElementById(id ? id : "toc"), jexample);
}
