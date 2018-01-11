#!/usr/bin/env node

var fs = require('fs-extra');
var shell = require('shelljs');
var path = require('path');
var deploymentConfigGenerator = require('./DeploymentConfigGenerator');
var updateGraphGenerator = require('./UpdateGraphGenerator');

/**
 * Clearing old bundles and repositories
 */
fs.removeSync('repositories');
fs.removeSync('bundles');
fs.ensureDirSync('repositories');
fs.ensureDirSync('bundles');


/**
 * Moving existing react native bundles to a temp directory.
 * The bundles would be copied from here for the repositories
 * which need not be deployed
 */
if (fs.pathExistsSync('temp')) {
    fs.removeSync('temp');
}
if (fs.pathExistsSync('ReactBundles')) {
    fs.moveSync('ReactBundles', 'temp');
}


var argv = require('yargs')
    .string('config').describe('config','DeploymentConfig for the deployer')
    .choices('platform',['android','ios']).describe('platform', 'platform to generate bundles for')
    .string('updateGraphVersion').describe('updateGraphVersion','Enter a update graph version higher than the previous deployment')
    .string('outputPath')
    .string('prodUpdateGraph').describe('prodUpdateGraph','filepath of update patch already in production')
    .boolean('react16')
    .argv;
var deploymentConfigFile = fs.readFileSync(argv.config, { encoding: 'utf-8' });
var deploymentConfig = JSON.parse(deploymentConfigFile);

deploymentConfig.deploymentJob.forEach(function (deploymentJob, index) {
    console.log(index);
    if (deploymentJob.shouldDeploy) {
        /**Cloning the repositories */
        fs.ensureDirSync(path.join('repositories', index.toString(), 'fk-react-native'));
        executeShellCommand('git clone -b ' + deploymentJob.branchName + ' ' + deploymentJob.repoUrl
            + ' ' + path.join('repositories', index.toString(), 'fk-react-native'));
        /**installing react@15.3.0 so that index.js for obfuscation could be directly replaced */
        //TODO: install the right app version
        // executeShellCommand('cd ' + path.join('repositories', index.toString(), 'fk-react-native')
        // + ' && ' + 'npm install --save react@15.3.0');
        /**running npm install */
        executeShellCommand('cd ' + path.join('repositories', index.toString(), 'fk-react-native')
            + ' && ' + 'npm install', true);
        /**replacing index.js in mode_modules/react-native/packager/react-packager/src/Bundle/index.js
         * with custom implementation of module id generation
         */
        // fs.copySync(path.join(__dirname, 'index.js'), path.join('repositories', index.toString(), 'fk-react-native', 'node_modules',
            // 'react-native', 'packager', 'react-packager', 'src', 'Bundle', 'index.js'), { overwrite: true });
        /**
         * running precompile script for each deployment job
         */
        executeShellCommand('cd ' + path.join('repositories', index.toString(), 'fk-react-native')
            + ' && ' + deploymentJob.preCompileScript);

        /**
         * Generating the react native bundle
         */
        if (argv.react16) {
            if (argv.platform === 'android') {
                executeShellCommand('cd ' + path.join('repositories', index.toString(), 'fk-react-native') + ' && ' +
                    'react-native bundle --platform android --dev false --entry-file index.android.js --bundle-output ../../../bundles/' + index + '.bundle');
            } else {
                executeShellCommand('cd ' + path.join('repositories', index.toString(), 'fk-react-native') + ' && ' +
                    'react-native bundle --platform ios --dev false --entry-file index.ios.js --bundle-output ../../../bundles/' + index + '.bundle');
            }
        } else {
            if (argv.platform === 'android') {
                executeShellCommand('cd ' + path.join('repositories', index.toString(), 'fk-react-native') + ' && ' +
                    'react-native bundle --platform android --dev false --minify true --entry-file index.android.js --bundle-output ../../../bundles/' + index + '.bundle');
            } else {
                executeShellCommand('cd ' + path.join('repositories', index.toString(), 'fk-react-native') + ' && ' +
                    'react-native bundle --platform ios --dev false --minify true --entry-file index.ios.js --bundle-output ../../../bundles/' + index + '.bundle')
            }
        }
        /**Copying the generated bundles to respective app versions directory */
        deploymentJob.appVersions.forEach(function (appVersion) {
            fs.ensureDirSync(path.join(argv.outputPath, 'ReactBundles', appVersion));
            fs.copySync(path.join('bundles', index.toString() + '.bundle'), path.join(argv.outputPath, 'ReactBundles', appVersion, deploymentJob.bundleName))
        });
    } else {
        /** If shouldDeploy for a particular deployment job is false, we use the previous bundle */
        deploymentJob.appVersions.forEach(function (appVersion) {
            fs.ensureDirSync(path.join(argv.outputPath, 'ReactBundles', appVersion));
            if (fs.pathExistsSync(path.join('temp', appVersion, deploymentJob.bundleName))) {
                fs.copySync(path.join('temp', appVersion, deploymentJob.bundleName), path.join(argv.outputPath, 'ReactBundles', appVersion, deploymentJob.bundleName))
            }
        });
    }
});
var releaseConfig = deploymentConfigGenerator(deploymentConfig, argv.outputPath);
fs.writeJSONSync('ReleaseConfig.json', releaseConfig);
var update = updateGraphGenerator('ReleaseConfig.json', argv.prodUpdateGraph, argv.updateGraphVersion);

/**
 * Writing output to filepath specified in output path
 */
fs.writeJSONSync(path.join(argv.outputPath, 'UpdatePatch.json'), update.updatePatch);
fs.writeJSONSync(path.join(argv.outputPath, 'ComponentMap.json'), update.componentMap);
fs.ensureDirSync(path.join(argv.outputPath, 'components'));
(Object.keys(update.componentMap)).forEach(function (componentKey) {
    fs.writeFileSync(path.join(argv.outputPath, 'components', componentKey), update.componentMap[componentKey]);
});

/**
 * Clearing all the temporary directories
 */
fs.removeSync('temp');
fs.removeSync('bundles');
fs.removeSync('repositories');


/**
 * executes the given shell command in the same process
 * @param command - command to be executed
 * @param printOutput - [boolean] if outsput should be printed to the console
 */
function executeShellCommand(command, printOutput) {
    var shellResult = shell.exec(command);
    if (printOutput) {
        console.log(shellResult.stdout);
    }
    if (shellResult.code !== 0) {
        shell.echo(command + ' failed!');
        shell.exit(1);
    }
}