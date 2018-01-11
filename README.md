# DUS - Dynamic Update Service
__DUS__ is a Over The Air (__OTA__) update system that allows React Native developers to deploy mobile app updates directly to the devices of the users. This system has the following features:

* __Differential downloading__ - Incase a React Native bundle is already present on the device, it downloads just the components that has changed
* __Multiple bundles__ - It allows more than one bundles to be deployed on just one application which improves the load time by reducing the parse time of the bundle and allow different teams to maintain their own React Native bundles
* __Instant Updates__ - Once an update is released, it guarantees that only the latest bundle is run on the device and not an older bundle from the cache.
* __Flexibility__ - It allows complete flexibility in the way update patches are kept on your server and the network calls that the app makes to fetch the update patch.

## Setup

* Install dus-deployer on your machine: ```npm install -g dus-deployer```

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

The specifications of these dependencies can be found here: \*link\*