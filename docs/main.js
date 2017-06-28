var wsUri = "ws://repo.daporkchop.tk:8888";
var output;
var shutdown = false;

var username;
var password;

function updateScroll() {
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
    document.getElementById("chatinput").style.display = 'none';
    document.getElementById("chatsendbutton").style.display = 'none';
    document.getElementById("loggingintext").style.display = 'none';
    document.getElementById("usernameinput").style.display = 'none';
    if (Notification.permission == "granted") {
        document.getElementById("requestpermsbutton").style.display = 'none';
    }
    var error = createCORSRequest('GET', "http://www.daporkchop.net/Pork2b2tBot/error.txt");
        error = document.getElementById("chat").innerHTML + "\n\nTrying to connect to chat...\nBot status: \u00A7" + error;
    initParser(error, 'chat', true);
}

function onOpen(evt) {
    doSend("connectrequest");
}

function onClose(evt) {
    if (!shutdown) {
        var error = createCORSRequest('GET', "http://www.daporkchop.net/Pork2b2tBot/error.txt");
        var newText = "\n\n\u00A7c\u00A7lWe seem to have been disconnected, or are unable to connect to the bot! Error:\n\n\u00A7r\u00A7f\u00A7" + error;
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
            document.getElementById("chat").innerHTML = "\n\n";
            break;
        case "shutdown":
            var newText = document.getElementById("chat").innerHTML + "\n\n\u00A7c\u00A7lWe've been disconnected because: <strong>" + evt.data.substring(8) + "</strong>\n\u00A7c\u00A7lReload the page in a few seconds!";
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
            var uuid = split[2];

            //if (document.body.contains('player_' + name))  {

            var tableRef = document.getElementById('tabplayers').getElementsByTagName('tbody')[0];
            var newRow = tableRef.rows[0];
            var newCell = newRow.insertCell(0);

            newCell.setAttribute('id', 'player_' + name);

            var img = document.createElement("img");
            img.setAttribute('src', 'https://crafatar.com/avatars/' + uuid + '?size=24&overlay');
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
            //}
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
            if (toRemove != null) {
                toRemove.remove();
            }
            break;
        case "chat    ":
            var text = evt.data.substring(8);
            var oldTextSplit = document.getElementById("chat").innerHTML.split("\u2000");
            if (oldTextSplit.length > 499) {
                oldTextSplit.splice(0, 1);
            }
            oldTextSplit.push("\n" + text);
            var newText = oldTextSplit.join("\u2000");
            initParser(newText, 'chat', true);
            updateScroll();
            if (text.startsWith("\u00A7d")) {
                var msgSplit = text.substring(2).split("\u00A7d");
                if (Notification.permission == "granted") {
                    new Notification("Message from " + msgSplit[0], {
                        body: msgSplit[2],
                        icon: "https://crafatar.com/avatars/" + msgSplit[0] + "?size=256&overlay",
                        requireInteraction: false
                    });
                }
            }
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
            document.getElementById("usernameinput").style.display = '';
            document.getElementById("chatsendbutton").style.display = '';
            var split = evt.data.substring(8).split("\u2001");
            username = split[0];
            password = split[1];
            console.log("Authenticated with server! Username: " + username + " Access token: " + password);
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

function getIconFromPing(ping) {
    if (ping < 25) {
        return "5bar.png";
    } else if (ping < 75) {
        return "4bar.png";
    } else if (ping < 150) {
        return "3bar.png";
    } else if (ping < 225) {
        return "2bar.png";
    } else {
        return "1bar.png";
    }
}

function sendChat() {
    var textInput = document.getElementById("chatinput");
    var usernameinput = document.getElementById("usernameinput");
    var toSend = textInput.value.replace(/[^\x00-\x7F]/g, ""); //remove all non-ASCII chars from string, as they'll get the bot kicked
    var toSendname = usernameinput.value.replace(/[^\x00-\x7F]/g, ""); //remove all non-ASCII chars from string, as they'll get the bot kicked
    if (toSend.length > 0) {
        doSend("sendChat" + toSend + "\u2001" + toSendname + "\u2001" + username + "\u2001" + password);
    }
}

function login() {
    var enteredUsername = document.getElementById("usernamein").value;
    var enteredPassword = document.getElementById("passwordin").value;

    var shaObj = new jsSHA("SHA-1", "TEXT");
    shaObj.update(enteredPassword);
    var hashedPassword = shaObj.getHash("HEX");

    document.getElementById("usernamein").style.display = 'none';
    document.getElementById("passwordin").style.display = 'none';
    document.getElementById("loginbutton").style.display = 'none';
    document.getElementById("loggingintext").style.display = '';
    doSend("login   " + enteredUsername + "\u2001" + hashedPassword);
}

function requestPerms() {
    if (Notification.permission != 'granted') {
        Notification.requestPermission(function(result) {
            if (result == 'granted') {
                navigator.serviceWorker.ready.then(function(registration) {
                    registration.showNotification('It worked!', {
                        body: 'Permission granted!',
                        icon: 'https://mcapi.ca/query/2b2t.org/icon',
                        requireInteraction: false,
                        tag: 'granted'
                    });
                    document.getElementById("requestpermsbutton").style.display = 'none';
                });
            }
        });
    }
}

window.addEventListener("load", init, false);