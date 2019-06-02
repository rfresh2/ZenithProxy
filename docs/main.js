/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016-2019 DaPorkchop_
 *
 * Permission is hereby granted to any persons and/or organizations using this software to copy, modify, merge, publish, and distribute it.
 * Said persons and/or organizations are not allowed to use the software or any derivatives of the work for commercial use or any other means to generate income, nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing and/or trademarking this software without explicit permission from DaPorkchop_. 
 *
 * Any persons and/or organizations using this software must disclose their source code and have it publicly available, include this license, provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

var chatCount = 0;
var maxChatCount = 512;

var autoScroll = true;

function init() {
    var address = get("address");
    if (!address)   {
        address = "wss://ws.daporkchop.net/2pork2bot";
    }
    var websocket = new WebSocket(address);
    websocket.onopen = onOpen;
    websocket.onclose = onClose;
    websocket.onmessage = onMessage;
    websocket.onerror = onError;
    addChat("<span style=\"color:#5f5\">Connecting...</span>");
}

function onOpen(evt) {
    document.getElementById("tabheader").innerHTML = "";
    document.getElementById("tabfooter").innerHTML = "";
    document.getElementById("players").innerHTML = "";
    document.getElementById("chat").innerHTML = "";
}

function onClose(evt) {
    addChat(" ");
    addChat(" ");
    addChat("<span style=\"color:#f55\">Disconnected.</span>")
}

function onMessage(evt) {
    var msg = JSON.parse(evt.data);
    switch (msg.command)    {
        case "init": {
            maxChatCount = msg.maxChatCount;
        }
        break;
        case "chat": {
            addChat(parseLoadedText(msg.chat));
        }
        break;
        case "player": {
            if (msg.name === "")    {
                break;
            }

            var element = document.getElementById("player-" + msg.uuid);
            var name;
            var ping;

            var existed = element;
            if (existed) {
                name = document.getElementById("name-" + msg.uuid);
                ping = document.getElementById("ping-" + msg.uuid);
            } else {
                element = document.createElement("LI");
                element.id = "player-" + msg.uuid;

                name = document.createElement("SPAN");
                name.id = ["name-", msg.uuid].join("");
                element.appendChild(name);

                var icon = document.createElement("IMG");
                icon.classList.add("playericon");
                icon.src = ["https://crafatar.daporkchop.net/avatars/", msg.uuid, "?overlay=true?size=32"].join("");
                icon.onload = function () {
                    icon.onload = null;
                    document.getElementById(["player-", msg.uuid].join("")).insertBefore(icon, document.getElementById(["name-", msg.uuid].join("")));
                };

                ping = document.createElement("IMG");
                ping.id = ["ping-", msg.uuid].join("");
                ping.classList.add("ping");
                ping.src = "ping/1bar.png";
                ping.onload = function () {
                    ping.onload = null;
                    document.getElementById(["player-", msg.uuid].join("")).appendChild(ping);
                };
            }

            var players = document.getElementById("players");
            var entries = players.children;
            var added = false;
            for (var i = 0; i < entries.length; i++) {
                if (entries[i].innerText > msg.name) {
                    if (existed)    {
                        element.parentElement.removeChild(element);
                    }
                    players.insertBefore(element, entries[i]);
                    added = true;
                    break;
                }
            }
            if (!added) {
                if (existed)    {
                    element.parentElement.removeChild(element);
                }
                players.appendChild(element);
            }

            name.innerText = msg.name;
            ping.src = getIconFromPing(msg.ping);
        }
        break;
        case "removePlayer": {
            var element = document.getElementById("player-" + msg.uuid);
            if (element)    {
                element.parentElement.removeChild(element);
            }
        }
        break;
        case "tabData": {
            console.log("Tab data!");
            document.getElementById("tabheader").innerHTML = parseLoadedText(msg.header);
            document.getElementById("tabfooter").innerHTML = parseLoadedText(msg.footer);
        }
        break;
        case "reset": {
            onOpen(null);
        }
        break;
    }
}

function onError(evt) {
}

function getIconFromPing(ping) {
    if (ping < 150) {
        return "ping/5bar.png";
    } else if (ping < 300) {
        return "ping/4bar.png";
    } else if (ping < 600) {
        return "ping/3bar.png";
    } else if (ping < 1000) {
        return "ping/2bar.png";
    } else {
        return "ping/1bar.png";
    }
}

window.addEventListener("load", init, false);

function get(name){
   if(name=(new RegExp('[?&]'+encodeURIComponent(name)+'=([^&]*)')).exec(location.search)) {
       return decodeURIComponent(name[1]);
   }
}

function addChat(msg) {
    var chat = document.getElementById("chat");
    var id = chatCount++;
    var element;

    if (id != 0) {
        element = document.createElement("BR");
        element.classList.add("chat-" + (id - 1));
        chat.appendChild(element);
    }

    element = document.createElement("SPAN");
    element.classList.add("chat", "chat-" + id);
    element.innerHTML = msg;
    chat.appendChild(element);


    element = document.getElementsByClassName("chat-" + (id - maxChatCount));
    while (element[0]) {
        element[0].parentNode.removeChild(element[0]);
    }

    if (autoScroll) {
        chat.scrollTop = chat.scrollHeight;
    }
}

document.getElementById("chat").addEventListener('scroll', function(event) {
    var element = event.target;
    autoScroll = element.scrollHeight - element.scrollTop === element.clientHeight;
});

function getTextWidth(text) {
    var textSize = document.getElementById("textSize");
    textSize.innerHTML = text;
    return textSize.clientWidth + 1;
}

setInterval(function () {
    var players = document.getElementById("players");
    var maxWidth = 0;
    var entries = players.children;
    for (var i = 0; i < entries.length; i++) {
        var width = getTextWidth(entries[i].innerText);
        if (width > maxWidth)   {
            maxWidth = width;
        }
    }
    players.style.columnWidth = (maxWidth + (32 + 5) + 10 +(5 + 32)) + "px";
}, 1000);
