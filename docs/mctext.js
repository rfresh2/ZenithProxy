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

var LEGACY_COLORS = {
    "0": "color:#000000; ",
    "1": "color:#0000AA; ",
    "2": "color:#00AA00; ",
    "3": "color:#00AAAA; ",
    "4": "color:#AA0000; ",
    "5": "color:#AA00AA; ",
    "6": "color:#FFAA00; ",
    "7": "color:#AAAAAA; ",
    "8": "color:#555555; ",
    "9": "color:#5555FF; ",
    "a": "color:#55FF55; ",
    "b": "color:#55FFFF; ",
    "c": "color:#FF5555; ",
    "d": "color:#FF55FF; ",
    "e": "color:#FFFF55; ",
    "f": "color:#FFFFFF; "
};

var REPLACEMENTS = {
    "<": "&lt;",
    ">": "&gt;"
};

function parseJsonText(msg) {
    return internal_parseStyle(JSON.parse(msg), []).join("");
}

function parseLoadedText(msg) {
    return internal_parseStyle(msg, []).join("");
}

function internal_parseStyle(msg, out) {
    var text = "";
    out.push("<span style=\"");
    if (typeof msg === "string" || msg instanceof String) {
        text = msg;
    } else {
        if (msg["bold"] === true) {
            out.push("font-weight:bold; ");
        }
        if (msg.italic === true) {
            out.push("font-style:italic; ");
        }
        if (msg.underline === true) {
            out.push("text-decoration:underline; ");
        }
        if (msg.strikethrough === true) {
            out.push("text-decoration:line-through; ");
        }
        if (msg.color) {
            out.push(MC_COLORS[msg.color]);
        }
        if (msg["text"]) {
            text = internal_sanitize(msg["text"]);
        }
    }
    out.push("\">");
    if (text.includes("\u00A7"))    {
        internal_parseLegacyText(text, out);
    } else {
        out.push(text);
    }
    out.push("</span>");
    if (!(typeof msg === "string" || msg instanceof String) && msg.extra) {
        for (var i = 0; i < msg.extra.length; i++) {
            internal_parseStyle(msg.extra[i], out);
        }
    }
    return out;
}

function internal_parseLegacyText(text, out) {
    var length = text.length;

    var bold = false;
    var italic = false;
    var underline = false;
    var strikethrough = false;
    var color = "f";

    var escape = false;
    out.push("<span style=\"", LEGACY_COLORS[color], "\">");

    for (var i = 0; i < length; i++)    {
        var char = text[i];
        if (escape) {
            escape = false;
            if (char === "l")   {
                bold = true;
            } else if (char === "m")    {
                strikethrough = true;
            } else if (char === "n")    {
                underline = true;
            } else if (char === "o")    {
                italic = true;
            } else {
                bold = italic = underline = strikethrough = false;
                if (LEGACY_COLORS[char]){
                    color = char;
                }
            }
            //rebuild style
            out.push("</span><span style=\"");
            if (bold)   {
                out.push("font-weight:bold; ");
            }
            if (italic) {
                out.push("font-style:italic; ");
            }
            if (underline)  {
                out.push("text-decoration:underline; ");
            }
            if (strikethrough)  {
                out.push("text-decoration:line-through; ");
            }
            out.push(LEGACY_COLORS[color], "\">");
        } else if (char === "\u00A7") {
            escape = true;
        } else if (char === "\n")   {
            out.push("<br>");
        } else {
            out.push(char);
        }
    }

    out.push("</span>");
    return out.join("");
}

function internal_sanitize(text) {
    return text.replace(/[<>]/g, function (s) {
        return REPLACEMENTS[s];
    });
}
