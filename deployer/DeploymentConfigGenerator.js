#!/usr/bin/env node
var fs = require('fs');
var path = require('path');
var argv = require('yargs')
    .string('config')
    .argv;
var deploymentConfigFile = fs.readFileSync(argv.config, {encoding: 'utf-8'});
var deploymentConfig = JSON.parse(deploymentConfigFile);
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
            console.log("ReactBundles", appVersion, deploymentJob.bundleName);
            updateGraphConfig.currentUpdateGraph[deploymentJob.bundleName] = {
                gitBranch: deploymentJob.branchName,
                gitRepoUrl: deploymentJob.repoUrl,
                bundlePath: path.join("ReactBundles", appVersion, deploymentJob.bundleName),
                shouldDeploy: deploymentJob.shouldDeploy
            };
        }
        releaseConfig.appConfig[appVersion] = updateGraphConfig;
    });
    shouldDeploy = deploymentJob.shouldDeploy || shouldDeploy;
    deploymentJob.shouldDeploy = false;
});

console.log(JSON.stringify(releaseConfig));