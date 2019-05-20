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

//thanks to whoever made this and put it on stackoverflow
var MC_COLORS = {
    "black": "color:#000000; ",
    "dark_blue": "color:#0000AA; ",
    "dark_green": "color:#00AA00; ",
    "dark_aqua": "color:#00AAAA; ",
    "dark_red": "color:#AA0000; ",
    "dark_purple": "color:#AA00AA; ",
    "gold": "color:#FFAA00; ",
    "gray": "color:#AAAAAA; ",
    "dark_gray": "color:#555555; ",
    "blue": "color:#5555FF; ",
    "green": "color:#55FF55; ",
    "aqua": "color:#55FFFF; ",
    "red": "color:#FF5555; ",
    "light_purple": "color:#FF55FF; ",
    "yellow": "color:#FFFF55; ",
    "white": "color:#FFFFFF; "
};

function parseJsonText(msg) {
    return internal_parseStyle(JSON.parse(msg));
}

function parseLoadedText(msg) {
    return internal_parseStyle(msg);
}


/*
{
  "extra": [
    {
      "text": "<ChatSpammer> "
    },
    {
      "color": "green",
      "text": "> Join the most well respected group "
    },
    {
      "color": "green",
      "clickEvent": {
        "action": "open_url",
        "value": "https://discord.gg/73C9bUs"
      },
      "text": "https://discord.gg/73C9bUs"
    }
  ],
  "text": ""
}
 */
function internal_parseStyle(msg) {
    var text = "";
    var style = "";
    if (typeof msg === 'string' || msg instanceof String) {
        text = msg;
    } else {
        if (msg["bold"] === true) {
            style += "font-weight:bold; ";
        }
        if (msg.italic === true) {
            style += "font-style:italic; ";
        }
        if (msg.underline === true) {
            style += "text-decoration:underline; ";
        }
        if (msg.strikethrough === true) {
            style += "text-decoration:line-through; ";
        }
        if (msg.color) {
            style += MC_COLORS[msg.color];
        }
        if (msg["text"]) {
            text = msg["text"];
        }
    }
    var html = "<span style=\"" + style + "\">" + text + "</span>";
    if (!(typeof msg === 'string' || msg instanceof String) && msg.extra) {
        for (var i = 0; i < msg.extra.length; i++) {
            html += internal_parseStyle(msg.extra[i]);
        }
    }
    return html;
}
