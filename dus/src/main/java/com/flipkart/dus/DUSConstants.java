package com.flipkart.dus;

import android.support.annotation.NonNull;

/**
 * Created by surya.kanoria on 27/01/17.
 */

public class DUSConstants {
    @NonNull
    public static final String JS_RESOURCE_DIRECTORY = "JSFileStorage";
    @NonNull
    public static final String FILE_CONFIG_STORAGE = "FileConfigStorage";
    //Status Values for Component Table
    public static final int CACHED = 0;
    public static final int DOWNLOADING = 1;
    public static final int ERROR = 2;
    public static final int NONE = 3;
    @NonNull
    public static String UPDATE_GRAPH_NAME = "RNUpdateGraph";
}