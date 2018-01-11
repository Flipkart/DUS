# Android Specifications for custom implementations

All the specifications for implementation of modules for custom implementation of DUS.

* **[ComponentDownloader](###ComponentDownloader)**
* **[FileConfigDownloader](###FileConfigDownloader)**
* **[DusLogger](###DusLogger)**
* **[PackagedDbName](###PackagedDbName)**
* **[PackagedDbVersion](###PackagedDbVersion)**

### ComponentDownloader

ComponentDownloader takes a list of keys for the components to be fetched and returns a map of key vs components that are downloaded

```
 @Override
    public void getResponseString(List<String> componentList, ResponseInterface<HashMap<String, String>> responseCallback) {
       		HashMap<String,String> componentsFetched = //downloaded components
            responseCallback.OnSuccess(componentsFetched.components); 
    }
```

### FileConfigDownloader

FileConfigDownloader takes the name of the update graph that needs to be downloaded (usually a constant for an application), the existing version of the file that it has.
It returns the latest update graph and it's version to dus

```
public class FileConfigDownloader implements FileConfigRequestInterface {
    @Override
    public void getResponseString(String updateGraphName, String updateGraphVersion, FileConfigResponseInterface responseCallback) {
            FileConfig fileConfig = new FileConfig();
            /** Set the new update graph */
            fileConfig.setCurrentUpdateGraph(responseObject.updateGraph.currentUpdateGraph);
            /** Set the version of the update graph */
            fileConfig.setCurrentUpdateGraphVersion(responseObject.updateGraph.currentUpdateGraphVersion);
            /** returns the update graph and the document version on the server side*/
            responseCallback.onSuccess(fileConfig, "server side version");
    }
}
```

### DusLogger

A custom implementation for a logger for DUS

```
public class DusLoggerResolver implements DusLogger {
    @Override
    public void log(String log) {
        System.out.println(log);
    }

    @Override
    public void logException(Throwable throwable) {
        System.err.println(throwable.getStackTrace().toString());
    }
}
```

### PackagedDbName (optional)

Path of the sqlite database that can be packaged inside the APK. It should contain the following table:

Table name: ComponentMeta
Columns: ComponentKey (Text) , ComponentValue (Text)


### PackagedDbVersion (optional)

Version number of the database that is shipped with the application. Incase, a new database is shipped with the new APK update, the version number should be bumped up.