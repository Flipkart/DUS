var fs = require('fs-extra');
var shell = require('shelljs');
var path = require('path');
fs.removeSync('repositories');
fs.removeSync('bundles');
var argv = require('yargs')
    .string('config')
    .string('platform')
    .boolean('react16')
    .argv;
var deploymentConfigFile = fs.readFileSync(argv.config, {encoding: 'utf-8'});
var deploymentConfig = JSON.parse(deploymentConfigFile);
fs.ensureDirSync('repositories');
fs.ensureDirSync('bundles');
if (fs.pathExistsSync('temp')) {
    fs.removeSync('temp');
}
if (fs.pathExistsSync('ReactBundles')) {
    fs.moveSync('ReactBundles', 'temp');
}
deploymentConfig.deploymentJob.forEach(function (deploymentJob, index) {
    console.log(index);
    if (deploymentJob.shouldDeploy) {
        fs.ensureDirSync(path.join('repositories', index.toString(), 'fk-react-native'));
        executeShellCommand('git clone -b ' + deploymentJob.branchName + ' ' + deploymentJob.repoUrl
            + ' ' + path.join('repositories', index.toString(), 'fk-react-native'));
        executeShellCommand('cd ' + path.join('repositories', index.toString(), 'fk-react-native')
            + ' && ' + 'npm install --save react@15.3.0');
        executeShellCommand('cd ' + path.join('repositories', index.toString(), 'fk-react-native')
            + ' && ' + 'npm install', true);
        fs.copySync('index.js', path.join('repositories', index.toString(), 'fk-react-native', 'node_modules',
            'react-native', 'packager', 'react-packager', 'src', 'Bundle', 'index.js'), {overwrite: true});
        executeShellCommand('cd ' + path.join('repositories', index.toString(), 'fk-react-native')
            + ' && ' + deploymentJob.preCompileScript);

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
        deploymentJob.appVersions.forEach(function (appVersion) {
            fs.ensureDirSync(path.join('ReactBundles', appVersion));
            fs.copySync(path.join('bundles', index.toString() + '.bundle'), path.join('ReactBundles', appVersion, deploymentJob.bundleName))
        });
    } else {
        deploymentJob.appVersions.forEach(function (appVersion) {
            fs.ensureDirSync(path.join('ReactBundles', appVersion));
            if (fs.pathExistsSync(path.join('temp', appVersion, deploymentJob.bundleName))) {
                fs.copySync(path.join('temp', appVersion, deploymentJob.bundleName), path.join('ReactBundles', appVersion, deploymentJob.bundleName))
            }
        });
    }
});

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