var fs = require('fs-extra');
var componentGenerator = require('./ComponentGenerator');
var generatedUpdatePatch = {};
var hashMap = {};
/**
 * Generates the update patch for the given release config
 * @param configFilePath - releaseconfig generated after generation of React Native Bundles
 * @param existingPatchPath - updateGraphs already present in production
 * @param updateGraphVersion - version number for the new update graph
 */
function generateUpdatePatch(configFilePath, existingPatchPath, updateGraphVersion) {
    var configJson = fs.readFileSync(configFilePath, { encoding: 'utf-8' });
    var config = JSON.parse(configJson);
    if (config.appConfig) {
        (Object.keys(config.appConfig)).forEach(function (appVersion) {
            var updateGraphConfig = config.appConfig[appVersion];
            console.log('Generating update patch for appVersion: ', appVersion);
            generateUpdateGraph(updateGraphConfig, existingPatchPath, appVersion, updateGraphVersion);
        })
    }
    return {
        componentMap: hashMap,
        updatePatch: generatedUpdatePatch
    }
}

/**
 * generates update graph for a given appversion and inserts it
 * in the generatedUpdatePatch corresponding to the appVersion
 * @param updateGraphConfig - config for the given appVersion
 * @param existingPatchPath - updateGraphs already present in production
 * @param appVersion
 * @param updateGraphVersion - version number for the new update graph
 */
function generateUpdateGraph(updateGraphConfig, existingPatchPath, appVersion, updateGraphVersion) {
    var updateGraph = getExistingUpdateGraphForAppVersion(appVersion, existingPatchPath);
    console.log('existing update graph', JSON.stringify(updateGraph));
    var newUpdateGraph = {};
    newUpdateGraph.currentUpdateGraph = sanitizeUpdateGraph(updateGraph.currentUpdateGraph, Object.keys(updateGraphConfig.currentUpdateGraph));
    console.log('santized update graph', JSON.stringify(newUpdateGraph));
    newUpdateGraph = generateUpdateGraphForAppVersion(updateGraphConfig, newUpdateGraph);
    console.log('generated update graph', JSON.stringify(newUpdateGraph));
    newUpdateGraph.currentUpdateGraphVersion = updateGraphVersion;
    generatedUpdatePatch[appVersion] = newUpdateGraph;
}

/**
 *
 * @param appVersion - appVersion for which update graph is required
 * @param existingPatchPath - filePath for update patches in production
 */
function getExistingUpdateGraphForAppVersion(appVersion, existingPatchPath) {
    var existingPatch = getExistingUpdatePatch(existingPatchPath);
    return Object.assign({}, existingPatch[appVersion]);
}

/**
 * gets update patches present in production from the filePath
 * @param existingPatchPath - filePath for update patches in production
 * @returns {{production update patches}}
 */
function getExistingUpdatePatch(existingPatchPath) {
    var existingPatch = {};
    if (existingPatchPath && existingPatchPath !== '' && fs.pathExistsSync(existingPatchPath)) {
        var existingPatchJSON = fs.readFileSync(existingPatchPath, { encoding: 'utf-8' });
        if (existingPatchJSON !== '') {
            existingPatch = JSON.stringify(existingPatchJSON);
        }
    }
    return existingPatch;
}

/**
 * Removes all bundles in production which are not to be included in the
 * new update graph
 * @param updateGraph - existing update graph
 * @param bundleNames - bundle names to be included in the new update graph
 * @returns {{}}
 */
function sanitizeUpdateGraph(updateGraph, bundleNames) {
    var sanitizedUpdateGraph = {};
    if (updateGraph) {
        bundleNames.forEach(function (bundleName) {
            if (updateGraph[bundleName]) {
                sanitizedUpdateGraph[bundleName] = updateGraph[bundleName];
            }
        })
    }
    return sanitizedUpdateGraph;
}

/**
 * generates update graph and components for an app version config and adds components to the componentMap
 * @param updateGraphConfig - updategraph config for a particular appVersion
 * @param productionUpdateGraph - updategraph already existing in production
 * @returns {generated update graph}
 */
function generateUpdateGraphForAppVersion(updateGraphConfig, productionUpdateGraph) {
    (Object.keys(updateGraphConfig.currentUpdateGraph)).forEach(function (bundleName) {
        var screenConfig = updateGraphConfig.currentUpdateGraph[bundleName];
        if (screenConfig.shouldDeploy) {
            var components = componentGenerator(screenConfig.bundlePath);
            productionUpdateGraph.currentUpdateGraph[bundleName] = components.componentList;
            hashMap = Object.assign({}, hashMap, components.hashMap);
        }
    });
    return productionUpdateGraph;
}

module.exports = generateUpdatePatch;