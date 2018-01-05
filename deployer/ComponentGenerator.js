var fs = require('fs');
var md5 = require('md5');
var THRESHOLD_LENGTH = 80;
/**
 * returns the componentMap generated for a given React Native bundle
 * and the list of components which needs to be merged to generate
 * the bundle on the client end
 * @param {string} filePath - filePath of the React Native bundle
 */
function generateComponents(filePath) {
    var hashMap = {};
    var placeHolderStructure = {};
    placeHolderStructure.fileStructure = [];
    var placeHolderResponse = generatePlaceHolder(filePath, placeHolderStructure, hashMap);
    hashMap = placeHolderResponse.hashMap;
    placeHolderStructure = placeHolderResponse.placeHolderStructure;
    var componentList = [];
    var placeHolderJson = JSON.stringify(placeHolderStructure);
    var placeHolderKey = md5(placeHolderJson);
    componentList.push(placeHolderKey);
    (Object.keys(hashMap)).forEach(function (key) {
        componentList.push(key);
    });
    hashMap[placeHolderKey] = placeHolderJson;
    return {
        hashMap: hashMap,
        componentList: componentList
    };
}

/**
 * Reads the react native bundle from the filePath, splits it into components and adds them
 * into the hashMap and generates the placeHolderStructure
 * @param {string} filePath - filePath of the React Native bundle
 * @param {{fileStructure-[]}} placeHolderStructure - placeHolderStructure which describes 
 * the concatenation logic of the components for the bundle
 * @param {{}} hashMap - map of all the components that are generated
 */
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

/**
 * This function reads the react native bundle and
 * returns an array of string generated from the split of the file
 * by the newline character
 * @param {string} filePath - file path of the react native bundle
 */
function readJSFile(filePath) {
    var jsFile = fs.readFileSync(filePath, { encoding: 'utf-8' });
    return jsFile.split("\n");
}

module.exports = generateComponents;