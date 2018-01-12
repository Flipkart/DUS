# DUS - Dynamic Update Service
__DUS__ is a Over The Air (__OTA__) update system that allows React Native developers to deploy mobile app updates directly to the devices of the users. It allows you to use your own servers to maintain release patches and provides configurability in the way these patches are downloaded on your Native Apps.

* **[Features](#features)**
* **[Why DUS?](#why-dus)**
* **[How it works?](#how-it-works)**
* **[Setup](#setup)**
    * **[Integration (For Android)](integration-for-android)**
* **[Deployment](#deployment)**
* **[Sample Project](#sample-project)**
* **[Contributing](#contributing)**
* **[License](#license)**
* **[Contact us](#contact-us)**

## Features

* __Differential downloading__ - Incase a React Native bundle is already present on the device, it downloads just the components that has changed and not the entire new bundle.
* __Multiple bundles__ - It allows more than one bundles to be deployed on just one application which improves the load time by reducing the parse time of the bundle and allow different teams to maintain their own React Native bundles.
*  __Code Sharing__ - While DUS encourages multiple bundles, for an application, it ensures that the common code between any set of bundles are downloaded just once and avoids redundant download of common code.
* __Instant Updates__ - Once an update is released, it guarantees that only the latest bundle is run on the device and not an older bundle from the cache.
* __Flexibility__ - It allows complete flexibility in the way update patches are kept on your server and the network calls that the app makes to fetch the update patch.

## Why DUS?

Once a React Native application is released, updating the code involves a recompilation of the new code and releasing a fresh APK/IPA. The adoption of the new APK/IPA and the review time associated with the review process makes it difficult for your latest code to reach your entire users instantly.

DUS allows you to push your latest improvements/bug fixes to all your users instantly. This introduces the agility of web into your Native Applications. It also allows multiple teams to work on the same applications where each team maintains and releases their React Native bundles for their own screens without affecting other bundles.

During the update process, DUS only downloads the missing/changed components in your React Native code reducing the download size by ~90%. While React Native does not allow sharing of code across multiple bundles, DUS avoids redundant download of common code during the bundle creation process at the native clients. 

## How it works? (WIP)

Dus Deployer pulls the repositories specified in a configuration file called ``DeploymentConfig.json`` creates a bundle for each repository, splits it into chunks and generates update patches which contains a config called **Update Graph** for each version of the Android/iOS application. These patches are then uploaded to the server and the chunks are uploaded to a key-value storage pair/CDN. 

The update graph for a app version specifies the chunks to be combined to generate a specific bundle. During the launch of the application, a new update graph is downloaded. When a new bundle is to be generated it fetches the chunks required for the bundle from the cache, downloading the missing chunks and combines them to form a new bundle.

This [ReactFoo video](https://www.youtube.com/watch?v=3G6tMg29Wnw) explains the working of DUS. A detailed guide on the workings of DUS can be found here(*link*)

## Supported React Native Versions

Each version of React Native has a corresponding branch 0.\<react-native-version\>-stable.

We are doing our best to support each React Native version and respond to new React Native releases. In most cases, if support for a particular React Native version is unavailable, the branch supporting the previous version should work.

|React Native Version   | DUS Version   |Branch Name|
|:-----------------------|:-------------|-----------|
|<0.30|WIP
|0.30.x|WIP|
|0.31.x|WIP|
|0.32.x|WIP|
|0.33.x|WIP|
|0.34.x|WIP|
|0.35.x|WIP|
|0.36.x|WIP|
|0.37.x|WIP|
|0.38.x|WIP|
|0.39.x|WIP|
|0.40.x|WIP|
|0.41.x|WIP|
|0.42.x|WIP|
|0.43.x|WIP|
|0.44.x|WIP|
|0.45.x|WIP|
|0.46.x|WIP|
|0.47.x|1.47.8|0.47-stable|
|0.48.x|WIP|
|0.49.x|WIP|
|0.50.x|WIP|
|0.51.x|WIP|
|0.52.x|WIP|


## Setup

* Install dus-deployer on your machine: ```npm install -g dus-deployer```

*NOTE: This guide assumes you have used the react-native init command to initialize your React Native project. As of March 2017, the command create-react-native-app can also be used to initialize a React Native project. If using this command, please run npm run eject in your project's home directory to get a project very similar to what react-native init would have created.* 

*Even if you have a different project structure, dus could be easily integrated by setting the appropriate filepath for dus in your ``android/settings.gradle``*

### Integration (For Android)
* Add dus-deployer to your app: ```npm install --save dus-deployer```
In your ``android/settings.gradle`` import the native dependencies of dus in your project using the following code:
```javascript   
include ':dus'
project(':dus').projectDir = new File(
        rootProject.projectDir, '../node_modules/dus-deployer/android/dus')
```

* In your ``android/app/build.gradle`` include dus as a dependency by adding ``compile project(":dus")`` in the dependencies:
```javascript
dependencies {
    compile fileTree(dir: "libs", include: ["*.jar"])
    compile "com.android.support:appcompat-v7:23.0.1"
    compile "com.facebook.react:react-native:+"  // From node_modules
    compile project(":dus")
}
```

* Update the MainActivity.java to use DUS by making the following changes. Specify the bundle name that needs to be fetched from DUS to launch React Native on this activity.
```
public class MainActivity extends ReactActivity {

    /**
     * The name of the React Native bundle that needs
     * to be fetched from DUS in this activity. This
     * name could also be recieved from the intent fired to open
     * this activity
     */
    private final String bundleName = "example";

    /**
     * Returns the name of the main component registered from JavaScript.
     * This is used to schedule rendering of the component.
     */
    @Override
    protected String getMainComponentName() {
        return "dusdemo";
    }

    @Override
    protected ReactActivityDelegate createReactActivityDelegate() {
        return new DusReactApplicationDelegate(this, getMainComponentName(), bundleName);
    }
}
```
* Update MainApplication.java by making the following changes:
```
public class MainApplication extends Application implements ReactApplication, DusApplication {

    /**
     * We create an instance of DusReactNativeHost instead
     * of ReactNativeHost to use DUS
     */
    private final DusReactNativeHost mReactNativeHost = new DusReactNativeHost(this) {
        @Override
        public boolean getUseDeveloperSupport() {
            return false;
        }

        @Override
        protected List<ReactPackage> getPackages() {
            return Arrays.<ReactPackage>asList(
                    new MainReactPackage()
            );
        }
    };

    @Override
    public ReactNativeHost getReactNativeHost() {
        return mReactNativeHost;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SoLoader.init(this, /* native exopackage */ false);
        /**
         * Fetches the latest update graph on app launch
         * which can be used to fetch new bundles on demand
         */
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                Cursor cursor = getApplicationContext().getContentResolver().query(DUSContracts.buildFetchUpdateGraphUri(), null, null, null, null);
                if (cursor != null) {
                    cursor.close();
                }
            }
        });
    }

    /**
     * We inject all the dependencies into DUS: ComponentDownloader, FileConfigDownloader
     * PackagedDbName, DusLogger and packagedDbVersion
     * @return DusDependencyResolver - all dependencies required by DUS
     */
    @Override
    public DusDependencyResolver getDusDependencyResolver() {
        DusDependencyResolver dependencyResolver = new DusDependencyResolver();
        dependencyResolver.setComponentRequestInterface(new ComponentDownloader())
                .setDusLogger(new DusLoggerResolver())
                .setFileConfigRequestInterface(new FileConfigDownloader())
                .setPackagedDbName("")
                .setPackagedDbVersion(0);
        return dependencyResolver;
    }

    @Override
    public DusReactNativeHost getDusReactNativeHost() {
        return mReactNativeHost;
    }
}
```
* Inject all custom implementations into DUS:
	* _ComponentDownloader_ - takes a list of components to be fetched and returns the components fetched from server
	* _FileConfigDependencyResolver_ - downloads the latest update graph from the server and returns it to DUS
	* _DUSLogger_ - A custom logger to log all events related to DUS on console/logging framework like Firebase
	* _PackagedDbName_ - A sqlite database that contains all the components that you is packaged in the APK.
	* _PackagedVersion_ - The version of the database that is shipped in the APK. This version number needs to be bumped up if this database changes with a new APK update.

The specifications of these dependencies can be found [here](https://github.com/Flipkart/DUS/blob/master/docs/AndroidSpecs.md)

## Deployment

* Create a file ``DeploymentConfig.json``. This file contains a list of jobs for a particular application. Each job specifies the react native repository url, the branch name, the app versions, a script to be executed immediately before generating a bundle and the name of the bundle.

**Sample File**

```javascript
{
  "deploymentJob": [
    {
      "repoUrl": "git@github.com:surya-kanoria/DUS-Sample-App.git",
      "appVersions": [
        "default"
      ],
      "branchName": "master",
      "shouldDeploy": true,
      "preCompileScript": "ls",
      "bundleName": "example"
    }
  ]
}
  ```
* Run the following command to generate the update patch for your application:
  ``dus-deployer --config DeploymentConfig.json --platform android --react16 true --updateGraphVersion <updateGraphVersion> --outputPath output --prodUpdateGraph <Update Patch generated during last deployment>
``  

	A Sample command would look like this:
 ``dus-deployer --config DeploymentConfig.json --platform android --react16 true --updateGraphVersion 0.0.0.0 --outputPath output --prodUpdateGraph output/UpdatePatch.json``



* In the output folder, an UpdatePatch.json would be generated which is a map of app version vs the update graphs that are generated which needs to be uploaded to your server.
* In the same output folder, ComponentMap.json would be generated. It is a map of component keys vs the components that needs to be uploaded to your server.

For advanced usage please refer this doc(\*link\*).

## Sample Project

[https://github.com/surya-kanoria/DUS-Sample-App](https://github.com/surya-kanoria/DUS-Sample-App)

## Contributing

### How?

The easiest way to contribute is by [forking the repo](https://help.github.com/articles/fork-a-repo/), making your changes and [creating a pull request](https://help.github.com/articles/creating-a-pull-request/).

### What?

* iOS support [WIP]
* Adding support for other versions of React Native. [WIP]
* Docs for integration with CI [WIP]
* Assets support
* Adding Wiki.
* Completing TODOs
* Writing unit tests.
* Finding bugs and issues. (submit [here](https://github.com/Flipkart/DUS/issues))
* Fixing bugs and issues.

## License

[Apache v2.0](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))

## Contact us

Please open issues for any bugs that you encounter. You can reach out to me on twitter [@suryakanoria](https://www.twitter.com/suryakanoria) or, write to cross-platform@flipkart.com for any questions that
you might have.