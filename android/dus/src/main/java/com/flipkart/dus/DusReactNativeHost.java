package com.flipkart.dus;

import android.app.Application;

import com.facebook.react.ReactNativeHost;
import com.facebook.react.ReactPackage;
import com.facebook.react.shell.MainReactPackage;

import java.util.Arrays;
import java.util.List;

/**
 * Created by surya.kanoria on 09/01/18.
 */

public class DusReactNativeHost extends ReactNativeHost {

    private String mJSBundle;

    public DusReactNativeHost(Application application) {
        super(application);
    }

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

    @Override
    protected String getJSBundleFile() {
        return mJSBundle;
    }

    public void setJSBundleFile(String jsBundleFile) {
        mJSBundle = jsBundleFile;
    }
}
