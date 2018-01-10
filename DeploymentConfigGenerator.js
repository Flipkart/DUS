#!/usr/bin/env node
var fs = require('fs');
var path = require('path');

/**
 * Generates the release config which is used
 * to generate update patches and keep a log of
 * deployments
 * @param deploymentConfig - deployment config given by the user
 * @param outputPath - base path of output folder
 * @returns {{releaseConfig}}
 */
function deploymentConfigGenerator(deploymentConfig, outputPath) {
    var shouldDeploy = false;
    var releaseConfig = {};
    releaseConfig.appConfig = {};
    deploymentConfig.deploymentJob.forEach(function (deploymentJob) {
        deploymentJob.appVersions.forEach(function (appVersion) {
            var updateGraphConfig = releaseConfig.appConfig[appVersion];
            if (updateGraphConfig === undefined) {
                updateGraphConfig = {};
                updateGraphConfig.currentUpdateGraph = {};
            }
            if (updateGraphConfig.currentUpdateGraph[deploymentJob.screenType]) {
                throw new Error('Screen type already present for particular updateGraphVersion');
            } else {
                updateGraphConfig.currentUpdateGraph[deploymentJob.bundleName] = {
                    gitBranch: deploymentJob.branchName,
                    gitRepoUrl: deploymentJob.repoUrl,
                    bundlePath: path.join(outputPath, "ReactBundles", appVersion, deploymentJob.bundleName),
                    shouldDeploy: deploymentJob.shouldDeploy
                };
            }
            releaseConfig.appConfig[appVersion] = updateGraphConfig;
        });
        shouldDeploy = deploymentJob.shouldDeploy || shouldDeploy;
        deploymentJob.shouldDeploy = false;
    });

    return releaseConfig;
}

module.exports = deploymentConfigGenerator;