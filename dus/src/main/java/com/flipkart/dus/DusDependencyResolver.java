package com.flipkart.dus;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.flipkart.dus.dependencies.ComponentRequestInterface;
import com.flipkart.dus.dependencies.DusLogger;
import com.flipkart.dus.dependencies.FileConfigRequestInterface;

/**
 * Created by surya.kanoria on 28/12/17.
 */

@SuppressWarnings("WeakerAccess")
public class DusDependencyResolver {
    private ComponentRequestInterface mComponentRequestInterface;
    private FileConfigRequestInterface mFileConfigRequestInterface;
    @Nullable
    private String packagedDbName;
    private int packagedDbVersion;
    private DusLogger mDusLogger;


    ComponentRequestInterface getComponentRequestInterface() {
        return mComponentRequestInterface;
    }

    @NonNull
    public DusDependencyResolver setComponentRequestInterface(@NonNull ComponentRequestInterface componentRequestInterface) {
        mComponentRequestInterface = componentRequestInterface;
        return this;
    }

    FileConfigRequestInterface getFileConfigRequestInterface() {
        return mFileConfigRequestInterface;
    }

    @NonNull
    public DusDependencyResolver setFileConfigRequestInterface(@NonNull FileConfigRequestInterface fileConfigRequestInterface) {
        mFileConfigRequestInterface = fileConfigRequestInterface;
        return this;
    }

    @Nullable
    String getPackagedDbName() {
        return packagedDbName;
    }

    @NonNull
    public DusDependencyResolver setPackagedDbName(@Nullable String packagedDbName) {
        this.packagedDbName = packagedDbName;
        return this;
    }

    public int getPackagedDbVersion() {
        return packagedDbVersion;
    }

    @NonNull
    public DusDependencyResolver setPackagedDbVersion(int packagedDbVersion) {
        this.packagedDbVersion = packagedDbVersion;
        return this;
    }

    public DusLogger getDusLogger() {
        return mDusLogger;
    }

    @NonNull
    public DusDependencyResolver setDusLogger(@NonNull DusLogger dusLogger) {
        mDusLogger = dusLogger;
        return this;
    }

    @NonNull
    public static DusDependencyResolver getDUSDependencyResolver(@NonNull Context context) {
        return ((DusApplication) (context.getApplicationContext())).getDusDependencyResolver();
    }

}
