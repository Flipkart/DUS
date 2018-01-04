var fs = require('fs');
var md5 = require('md5');
var THRESHOLD_LENGTH = 80;
function generateHashes(filePath) {
    var hashMap = {};
    var placeHolderStructure = {};
    placeHolderStructure.fileStructure = [];
    var placeHolderResponse = generatePlaceHolder(filePath,placeHolderStructure ,hashMap);
    hashMap = placeHolderResponse.hashMap;
    placeHolderStructure = placeHolderResponse.placeHolderStructure;
    var componentList = [];
    var placeHolderJson = JSON.stringify(placeHolderStructure);
    var placeHolderKey = md5(placeHolderJson);
    componentList.push(placeHolderKey);
    (Object.keys(hashMap)).forEach(function(key) {
        componentList.push(key);
    });
    hashMap[placeHolderKey] = placeHolderJson;
}

function generatePlaceHolder(filePath, placeHolderStructure, hashMap) {
    var componentList = readJSFile(filePath);
    componentList.forEach(function (component) {
        var componentMetaModel;
        if (component.length < THRESHOLD_LENGTH) {
            componentMetaModel = {
                text: 'text',
                value: component
            }
        } else {
            componentMetaModel = {
                text: 'component',
                value: md5(component)
            };
            hashMap[md5(component)] = component;
        }
        placeHolderStructure.fileStructure.push(componentMetaModel);
    });
    return {
        placeHolderStructure: placeHolderStructure,
        hashMap: hashMap
    };
}

function readJSFile(filePath) {
    var jsFile = fs.readFileSync(filePath, {encoding: 'utf-8'});
    return jsFile.split("\n");
}

module.exports = generateHashes;