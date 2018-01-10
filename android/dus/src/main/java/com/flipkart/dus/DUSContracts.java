package com.flipkart.dus;

import android.net.Uri;
import android.support.annotation.NonNull;

import java.util.List;

import static com.flipkart.dus.internals.DatabaseHelper.COMPONENT_KEY;


/**
 * Created by surya.kanoria on 27/01/17.
 */

public class DUSContracts {

    /* Response status for file query */
    @NonNull
    public static final int LOADED = 0;
    @NonNull
    public static final int LOADING = 1;
    @NonNull
    public static final int ERROR = 2;


    /* Uri exposed by DUS content provider */
    @NonNull
    static String CONTENT_AUTHORITY = "com.flipkart.dus.duscontentprovider";
    @NonNull
    private static Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    @NonNull
    public static final String PATH_JS_BUNDLE = "screen";
    @NonNull
    static final String PATH_JS_COMPONENTS = "components";
    @NonNull
    static final String PATH_UPDATEGRAPH = "updateGraph";
    @NonNull
    static final String PATH_CLEAR = "clear";
    @NonNull
    public static final String QUERY_SHOULD_RETRY = "shouldRetry";
    @NonNull
    public static final String QUERY_ERROR = "error";
    @NonNull
    public static final String TRUE = "true";
    @NonNull
    public static final String FALSE = "false";

    @NonNull
    public static final String COLUMN_STATUS = "status";
    @NonNull
    public static final String COLUMN_RESPONSE = "response";

    public static final int JS_BUNDLE = 0;
    static final int JS_COMPONENTS = JS_BUNDLE + 1;
    static final int UPDATE_GRAPH = JS_BUNDLE + 2;
    static final int CLEAR = JS_BUNDLE + 3;

    @NonNull
    public static final String FILE_PATH = "filePath";


    public static Uri buildFetchUpdateGraphUri() {
        return BASE_CONTENT_URI.buildUpon().appendPath(PATH_UPDATEGRAPH).build();
    }

    public static Uri buildFetchUpdateGraphUriWithRetry() {
        return buildFetchUpdateGraphUri().buildUpon().appendQueryParameter(QUERY_SHOULD_RETRY, TRUE).build();
    }

    public static Uri buildFetchContentsUri(String screenType) {
        return BASE_CONTENT_URI.buildUpon().appendPath(PATH_JS_COMPONENTS).build().buildUpon().appendPath(screenType).build();
    }

    public static Uri buildFetchContentsUriWithError(String screenType) {
        return buildFetchContentsUri(screenType).buildUpon().appendQueryParameter(QUERY_ERROR, "true").build();
    }

    public static Uri buildFetchPageUri(String screenType) {
        return BASE_CONTENT_URI.buildUpon().appendPath(PATH_JS_BUNDLE).build().buildUpon().appendPath(screenType).build();
    }

    public static Uri buildFetchPageUriWithRetry(String screenType) {
        return buildFetchPageUri(screenType).buildUpon().appendQueryParameter(QUERY_SHOULD_RETRY, TRUE).build();
    }

    public static Uri buildFetchPageUriWithError(String screenType) {
        return buildFetchPageUri(screenType).buildUpon().appendQueryParameter(QUERY_ERROR, "true").build();
    }

    public static Uri buildFetchUpdateGraphUriWithError(String result) {
        return buildFetchUpdateGraphUri().buildUpon().appendQueryParameter(QUERY_ERROR, result).build();
    }

    @NonNull
    public static String getOptimizeStorageWhereQuery(@NonNull List<String> keys) {
        return " WHERE " + COMPONENT_KEY + " NOT IN (" + getInQuery(keys) + ");";
    }

    public static Uri buildWipeAll() {
        return BASE_CONTENT_URI.buildUpon().appendPath(PATH_CLEAR).build();
    }

    @NonNull
    private static String getInQuery(@NonNull List<String> keys) {
        if (keys.size() < 1) {
            throw new RuntimeException("No placeholders");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < keys.size(); i++) {
                sb.append(" \"").append(keys.get(i)).append("\" ");
                if (i != keys.size() - 1) {
                    sb.append(",");
                }
            }
            return sb.toString();
        }
    }

    @NonNull
    public static String getComponentsQuery(@NonNull List<String> keys) {
        return " WHERE " + COMPONENT_KEY + " IN (" + getInQuery(keys) + ");";
    }

    private static String makePlaceholders(int len) {
        if (len < 1) {
            // It will lead to an invalid query anyway ..
            throw new RuntimeException("No placeholders");
        } else {
            StringBuilder sb = new StringBuilder(len * 2 - 1);
            sb.append("?");
            for (int i = 1; i < len; i++) {
                sb.append(",?");
            }
            return sb.toString();
        }
    }

    static void initializeAuthority(@NonNull String authority) {
        CONTENT_AUTHORITY = authority;
        BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    }
}
