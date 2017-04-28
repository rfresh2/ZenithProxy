var wsUri = "ws://repo.daporkchop.tk:8888";
var output;

function init() {
    testWebSocket();
}

function testWebSocket() {
    websocket = new WebSocket(wsUri);
    websocket.onopen = function(evt) {
        onOpen(evt)
    };
    websocket.onclose = function(evt) {
        onClose(evt)
    };
    websocket.onmessage = function(evt) {
        onMessage(evt)
    };
    websocket.onerror = function(evt) {
        onError(evt)
    };
}

function onOpen(evt) {
    doSend("connectrquest");
}

function onClose(evt) {
    alert("DISCONNECTED");
}

function onMessage(evt) {
	//console.log(evt.data);
    var command = evt.data.substring(0, 8);
    //writeToScreen(command);
    switch (command) {
        case "connect ":
            //ignore for now, might be used later
            break;
        case "shutdown":
            alert("Bot stopped! Reason: " + evt.data.substring(8));
            break;
        case "tabDiff ":
            var split = evt.data.substring(8).split("SPLIT");
            var header = split[0];
            var footer = split[1];
            //console.log("header: " + header);
            //console.log("footer: " + footer);
            initParser(header, 'tabheader', true);
            initParser(footer, 'tabfooter', true);
            break;
        case "tabAdd  ":
            var split = evt.data.substring(8).split("SPLIT");
            var name = split[0];
            var ping = parseInt(split[1]);

            //console.log(name + " " + ping + "ms");

            var tableRef = document.getElementById('tabplayers').getElementsByTagName('tbody')[0];
            var newRow = tableRef.rows[0];
            var newCell = newRow.insertCell(0);

            newCell.setAttribute('id', 'player_' + name);

            var img = document.createElement("img");
            img.setAttribute('src', 'https://crafatar.com/avatars/' + name + '?size=32&overlay');
            img.setAttribute('height', '32px');
            img.setAttribute('width', '32px');
            newCell.appendChild(img);
			
            var newText = document.createElement("p");
            newText.setAttribute('id', 'playername_' + name);
            newText.setAttribute('style', 'display:inline');
            newText.innerHTML = ' ' + name + ' ';
            newCell.appendChild(newText);
			
			var ping = document.createElement("img");
			ping.setAttribute('id', 'playerping_' + name);
			ping.setAttribute('src', getIconFromPing(ping));
            ping.setAttribute('height', '32px');
            ping.setAttribute('width', '40px');
            newCell.appendChild(ping);
            break;
        case "tabPing ":
            var split = evt.data.substring(8).split("SPLIT");
            var name = split[0];
            var ping = parseInt(split[1]);
			
			//console.log(ping);

            var img = document.getElementById('playerping_' + name);
            if (img != null) {
                img.src = getIconFromPing(ping);
            }
            break;
        case "tabDel  ":
            var name = evt.data.substring(8);

            document.getElementById('player_' + name).remove();
            break;
    }
}

function onError(evt) {
    writeToScreen('<span style="color: red;">ERROR:</span> ' + evt.data);
}

function doSend(message) {
    websocket.send(message);
}

function writeToScreen(message) {
    var tableRef = document.getElementById('tabplayers').getElementsByTagName('tbody')[0];
    var newRow = tableRef.rows[0];
    var newCell = newRow.insertCell(tableRef.rows.length - 1);
    var newText = document.createTextNode(message + " ");
    newCell.appendChild(newText);
}

function getIconFromPing(ping)	{
	if (ping < 25)	{
		return "5bar.png";
	} else if (ping < 50)	{
		return "4bar.png";
	} else if (ping < 75)	{
		return "3bar.png";
	} else if (ping < 100)	{
		return "2bar.png";
	} else {
		return "1bar.png";
	}
}

window.addEventListener("load", init, false);