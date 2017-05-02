var wsUri = "ws://repo.daporkchop.tk:8888";
var output;
var shutdown = false;
var password;

function updateScroll(){
    var element = document.getElementById("chat");
    element.scrollTop = element.scrollHeight;
}

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
    document.getElementById("chat").innerHTML = "\n\nTrying to connect to chat...";
    document.getElementById("chatinput").style.display = 'none';
    document.getElementById("chatsendbutton").style.display = 'none';
    document.getElementById("loggingintext").style.display = 'none';
}

function onOpen(evt) {
    doSend("connectrequest");
}

function onClose(evt) {
    if (!shutdown)   {
        var error = createCORSRequest('GET', "http://home.daporkchop.net/misc/2b2terror.txt");
        var newText = document.getElementById("chat").innerHTML + "\n\n\u00A7c\u00A7lWe seem to have been disconnected, or are unable to connect to the bot! Error:\n\n" + error;
        initParser(newText, 'chat', true);
    }
    shutdown = false;
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
            var newText = document.getElementById("chat").innerHTML + "\n\n\u00A7c\u00A7lWe've been disconnected because: <strong>" + evt.data.substring(8)+ "</strong>\n\u00A7c\u00A7lReload the page in a few seconds!";
            initParser(newText, 'chat', true);
            shutdown = true;
            break;
        case "tabDiff ":
            var split = evt.data.substring(8).split("\u2001");
            var header = split[0];
            var footer = split[1];
            initParser(header, 'tabheader', true);
            initParser(footer, 'tabfooter', true);
            break;
        case "tabAdd  ":
            var split = evt.data.substring(8).split("\u2001");
            var name = split[0];
            var ping = parseInt(split[1]);

            var tableRef = document.getElementById('tabplayers').getElementsByTagName('tbody')[0];
            var newRow = tableRef.rows[0];
            var newCell = newRow.insertCell(0);

            newCell.setAttribute('id', 'player_' + name);

            var img = document.createElement("img");
            img.setAttribute('src', 'https://crafatar.com/avatars/' + name + '?size=24&overlay');
            img.setAttribute('height', '24px');
            img.setAttribute('width', '24px');
            newCell.appendChild(img);

            var newText = document.createElement("p");
            newText.setAttribute('id', 'playername_' + name);
            newText.setAttribute('style', 'display:inline; font-size: 12px;');
            newText.innerHTML = ' ' + name;
            newCell.appendChild(newText);

            var ping = document.createElement("img");
            ping.setAttribute('id', 'playerping_' + name);
            ping.setAttribute('src', getIconFromPing(ping));
            ping.setAttribute('height', '32px');
            ping.setAttribute('width', '40px');
            newCell.appendChild(ping);
            break;
        case "tabPing ":
            var split = evt.data.substring(8).split("\u2001");
            var name = split[0];
            var ping = parseInt(split[1]);

            //console.log(ping);

            var img = document.getElementById('playerping_' + name);
            if (img != null) {
                img.src = getIconFromPing(ping);
            }
            updateScroll();
            break;
        case "tabDel  ":
            var name = evt.data.substring(8);

            var toRemove = document.getElementById('player_' + name);
            if (toRemove != null)   {
                toRemove.remove();
            }
            break;
        case "chat    ":
            var text = evt.data.substring(8);
            var oldTextSplit = document.getElementById("chat").innerHTML.split("\u2000");
            if (oldTextSplit.length > 499)  {
                oldTextSplit.splice(0, 1);
            }
            oldTextSplit.push("\n" + text);
            var newText = oldTextSplit.join("\u2000");
            initParser(newText, 'chat', true);
            updateScroll();
            break;
        case "chatSent":
            document.getElementById("chatinput").value = "";
            break;
        case "loginErr":
            document.getElementById("loggingintext").style.display = '';
            document.getElementById("loggingintext").innerHTML = "<br>" + evt.data.substring(8);
            break;
        case "loginOk ":
            document.getElementById("loggingintext").style.display = 'none';
            document.getElementById("chatinput").style.display = '';
            document.getElementById("chatsendbutton").style.display = '';
            break;
    }
}

function onError(evt) {

}

function createCORSRequest(method, url) {
  var xhr = new XMLHttpRequest();
  if ("withCredentials" in xhr) {
    xhr.open(method, url, false);
  } else if (typeof XDomainRequest != "undefined") {
    xhr = new XDomainRequest();
    xhr.open(method, url);
  } else {
    xhr = "Your browser doesn't support CORS, unable to fetch the error lol";
  }
  xhr.send();
  return xhr.responseText;
}

function doSend(message) {
    websocket.send(message);
}

function getIconFromPing(ping)  {
  if (ping < 25)  {
    return "5bar.png";
  } else if (ping < 75)  {
    return "4bar.png";
  } else if (ping < 150)  {
    return "3bar.png";
  } else if (ping < 225)  {
    return "2bar.png";
  } else {
    return "1bar.png";
  }
}

function sendChat() {
    var textInput = document.getElementById("chatinput");
    var toSend = textInput.value;
    doSend("sendChat" + toSend + "\u2001" + username + "\u2001" + password);
}

function login()    {
    var enteredUsername = document.getElementById("usernamein").value;
    var enteredPassword = document.getElementById("passwordin").value;
    username = enteredUsername;
    password = enteredPassword;
    document.getElementById("usernamein").style.display = 'none';
    document.getElementById("passwordin").style.display = 'none';
    document.getElementById("loginbutton").style.display = 'none';
    document.getElementById("loggingintext").style.display = '';
    doSend("login   " + enteredUsername + "\u2001" + enteredPassword);
}

window.addEventListener("load", init, false);