function buildTOC(container, jsections, prefix, depth) {
    if (!depth)
        depth = 0;

    if (!prefix)
        prefix = null;

    prefix = prefix ? prefix : '';

    var t = 1;

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
    jtoc = [
        { "title": "Getting started", "url":"getting-started.html", "children": [
            { "title": "Requirements", "url":"getting-started.html#requirements" },
            { "title": "Starting server", "url":"getting-started.html#start" },
            { "title": "First access", "url":"getting-started.html#first-access" }
            ]
        },
        { "title": "Users and roles", "url":"users-roles.html"},
        { "title": "Blueprints", "url":"blueprints.html", "children": [
                { "title": "Nodes and connectors", "url":"blueprints.html#nodes-connectors"}
            ]
        },
        { "title": "Programs", "url":"programs.html", "children": [
                { "title": "The <i>Main</i> blueprint", "url":"programs.html#main"},
                { "title": "The <i>Events</i> blueprint", "url":"programs.html#events"}
            ]
        },
        { "title": "Flow control", "url":"flow.html"},
        { "title": "Installing plugins", "url":"plugins.html"}
    ];

    buildTOC(document.getElementById(id ? id : "toc"), jtoc);
}
