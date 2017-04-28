//thanks to whoever made this and put it on stackoverflow
var obfuscators = [];
var styleMap = {
    '\u00A70': 'color:#000000',
    '\u00A71': 'color:#0000AA',
    '\u00A72': 'color:#00AA00',
    '\u00A73': 'color:#00AAAA',
    '\u00A74': 'color:#AA0000',
    '\u00A75': 'color:#AA00AA',
    '\u00A76': 'color:#FFAA00',
    '\u00A77': 'color:#AAAAAA',
    '\u00A78': 'color:#555555',
    '\u00A79': 'color:#5555FF',
    '\u00A7a': 'color:#55FF55',
    '\u00A7b': 'color:#55FFFF',
    '\u00A7c': 'color:#FF5555',
    '\u00A7d': 'color:#FF55FF',
    '\u00A7e': 'color:#FFFF55',
    '\u00A7f': 'color:#FFFFFF',
    '\u00A7l': 'font-weight:bold',
    '\u00A7m': 'text-decoration:line-through',
    '\u00A7n': 'text-decoration:underline',
    '\u00A7o': 'font-style:italic',
};

function obfuscate(string, elem) {
    var magicSpan,
        currNode;
    if (string.indexOf('<br>') > -1) {
        elem.innerHTML = string;
        for (var j = 0, len = elem.childNodes.length; j < len; j++) {
            currNode = elem.childNodes[j];
            if (currNode.nodeType === 3) {
                magicSpan = document.createElement('span');
                magicSpan.innerHTML = currNode.nodeValue;
                elem.replaceChild(magicSpan, currNode);
                init(magicSpan);
            }
        }
    } else {
        init(elem, string);
    }

    function init(el, str) {
        var i = 0,
            obsStr = str || el.innerHTML,
            len = obsStr.length;
        obfuscators.push(window.setInterval(function() {
            if (i >= len) i = 0;
            obsStr = replaceRand(obsStr, i);
            el.innerHTML = obsStr;
            i++;
        }, 0));
    }

    function randInt(min, max) {
        return Math.floor(Math.random() * (max - min + 1)) + min;
    }

    function replaceRand(string, i) {
        var randChar = String.fromCharCode(randInt(64, 95));
        return string.substr(0, i) + randChar + string.substr(i + 1, string.length);
    }
}

function applyCode(string, codes) {
    var elem = document.createElement('span'),
        obfuscated = false;
    string = string.replace(/\x00*/g, '');
    for (var i = 0, len = codes.length; i < len; i++) {
        elem.style.cssText += styleMap[codes[i]] + ';';
        if (codes[i] === '\u00A7k') {
            obfuscate(string, elem);
            obfuscated = true;
        }
    }
    if (!obfuscated) elem.innerHTML = string;
    return elem;
}

function parseStyle(string) {
    var codes = string.match(/\u00A7.{1}/g) || [],
        indexes = [],
        apply = [],
        tmpStr,
        deltaIndex,
        noCode,
        final = document.createDocumentFragment(),
        i;
    string = string.replace(/\n|\\n/g, '<br>');
    for (i = 0, len = codes.length; i < len; i++) {
        indexes.push(string.indexOf(codes[i]));
        string = string.replace(codes[i], '\x00\x00');
    }
    if (indexes[0] !== 0) {
        final.appendChild(applyCode(string.substring(0, indexes[0]), []));
    }
    for (i = 0; i < len; i++) {
        indexDelta = indexes[i + 1] - indexes[i];
        if (indexDelta === 2) {
            while (indexDelta === 2) {
                apply.push(codes[i]);
                i++;
                indexDelta = indexes[i + 1] - indexes[i];
            }
            apply.push(codes[i]);
        } else {
            apply.push(codes[i]);
        }
        if (apply.lastIndexOf('\u00A7r') > -1) {
            apply = apply.slice(apply.lastIndexOf('\u00A7r') + 1);
        }
        tmpStr = string.substring(indexes[i], indexes[i + 1]);
        final.appendChild(applyCode(tmpStr, apply));
    }
    return final;
}

function clearObfuscators() {
    var i = obfuscators.length;
    for (; i--;) {
        clearInterval(obfuscators[i]);
    }
    obfuscators = [];
}

function initParser(input, output, isRawInput) {
    clearObfuscators();
    if (isRawInput) {
        var output = document.getElementById(output),
            parsed = parseStyle(input);
        output.innerHTML = '';
        output.appendChild(parsed);
    } else {
        var input = document.getElementById(input),
            output = document.getElementById(output),
            parsed = parseStyle(input.value);
        output.innerHTML = '';
        output.appendChild(parsed);
    }
}
//TODO: remove this
//var parseBtn = document.getElementById('parse');
//
//parseBtn.onclick = function () {
//   initParser("\u00A74RED\u00A7lBOLD\u00A79blue\u00A7nunderline\u00A7r\u00A72\u00A7kOBFUSCATED \u00A74kek", 'output', true);
//};