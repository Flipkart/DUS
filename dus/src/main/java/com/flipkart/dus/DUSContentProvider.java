package com.flipkart.dus;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.flipkart.dus.dependencies.DusLogger;
import com.flipkart.dus.internals.ComponentDownloader;
import com.flipkart.dus.internals.DatabaseHelper;
import com.flipkart.dus.internals.FileConfigHelper;
import com.flipkart.dus.internals.ScreenMaker;
import com.flipkart.dus.internals.UpdateGraphManager;
import com.flipkart.dus.utilities.FileHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.flipkart.dus.internals.DatabaseHelper.TABLE_COMPONENTS;


/**
 * Created by surya.kanoria on 25/01/17.
 */

public class DUSContentProvider extends ContentProvider {

    private static UriMatcher sUriMatcher;

    @NonNull
    private static final String TAG = "DUSCONTENTPROVIDER";
    @NonNull
    private final ConcurrentHashMap<String, ScreenInfo> mCachedScreenInfo = new ConcurrentHashMap<>();
    @NonNull
    private final ThreadLocal<BatchUriSet> mApplyBatch = new ThreadLocal<>();
    @Nullable
    private volatile FileHelper mFileHelper;
    @Nullable
    private volatile ScreenMaker mScreenMaker;
    @Nullable
    private DatabaseHelper mDatabaseHelper;
    @NonNull
    private AtomicBoolean refreshUpdateGraph = new AtomicBoolean(false);

    @Override
    public boolean onCreate() {
        return true;
    }

    @NonNull
    private FileHelper getFileHelper() {
        if (mFileHelper == null) {
            synchronized (this) {
                if (null == mFileHelper) {
                    mFileHelper = new FileHelper(getContext(), DUSConstants.JS_RESOURCE_DIRECTORY);
                }
            }
        }
        return mFileHelper;
    }

    @NonNull
    private DatabaseHelper getDatabaseHelper() {
        if (mDatabaseHelper == null) {
            synchronized (this) {
                if (mDatabaseHelper == null) {
                    mDatabaseHelper = new DatabaseHelper(getContext(), DusDependencyResolver.getDUSDependencyResolver(getContext()).getPackagedDbName());
                }
            }
        }
        return mDatabaseHelper;
    }

    @NonNull
    private ScreenMaker getScreenMaker() {
        if (mScreenMaker == null) {
            synchronized (this) {
                if (null == mScreenMaker) {
                    FileHelper fileHelper = new FileHelper(getContext(), DUSConstants.FILE_CONFIG_STORAGE);
                    FileConfigHelper fileConfigHelper = new FileConfigHelper(fileHelper, getContext());
                    ComponentDownloader componentDownloader = new ComponentDownloader(DusDependencyResolver.getDUSDependencyResolver(getContext()).getComponentRequestInterface());
                    UpdateGraphManager updateGraphManager = new UpdateGraphManager(fileConfigHelper, DusDependencyResolver.getDUSDependencyResolver(getContext()).getFileConfigRequestInterface(), getContext());
                    mScreenMaker = new ScreenMaker(updateGraphManager, componentDownloader, getContext());
                }
            }
        }
        return mScreenMaker;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;
        getLoggerInstance().log("[SYNC] [QUERY]" + uri.toString());
        switch (sUriMatcher.match(uri)) {
            case DUSContracts.JS_BUNDLE:
                String screenType = uri.getLastPathSegment();
                ScreenInfo screenInfo = mCachedScreenInfo.get(screenType);
                boolean shouldRetry = DUSContracts.TRUE.equalsIgnoreCase(uri.getQueryParameter(DUSContracts.QUERY_SHOULD_RETRY));
                boolean shouldRefresh = false;
                if (screenInfo != null) {
                    if (DUSContracts.LOADED.equalsIgnoreCase(screenInfo.status)) {
                        cursor = generateResponse(screenInfo.status, screenInfo.filePath);
                    } else if (DUSContracts.LOADING.equalsIgnoreCase(screenInfo.status)) {
                        cursor = generateResponse(screenInfo.status, "");
                    } else if (DUSContracts.ERROR.equalsIgnoreCase(screenInfo.status)) {
                        if (shouldRetry) {
                            cursor = generateResponse(DUSContracts.LOADING, "");
                            shouldRefresh = getScreenMaker().fetchPage(screenType, getContext());
                        } else {
                            cursor = generateResponse(DUSContracts.ERROR, "");
                        }
                    }
                } else {
                    String filePath = getFileHelper().getFilePath(getScreenMaker().getFileKey(screenType));
                    if (TextUtils.isEmpty(filePath)) {
                        shouldRefresh = getScreenMaker().fetchPage(screenType, getContext());
                        cursor = generateResponse(DUSContracts.LOADING, "");
                    } else {
                        screenInfo = new ScreenInfo();
                        screenInfo.status = DUSContracts.LOADED;
                        screenInfo.filePath = filePath;
                        mCachedScreenInfo.put(screenType, screenInfo);
                        cursor = generateResponse(DUSContracts.LOADED, filePath);
                    }
                }
                if (shouldRefresh) {
                    if (screenInfo == null) {
                        screenInfo = new ScreenInfo();
                    }
                    screenInfo.status = DUSContracts.LOADING;
                    mCachedScreenInfo.put(screenType, screenInfo);
                    query(DUSContracts.buildFetchUpdateGraphUriWithRetry(), null, null, null, null);
                }
                break;
            case DUSContracts.JS_COMPONENTS:
                SQLiteDatabase database = getDatabaseHelper().getReadableDatabase();
                cursor = database.query(TABLE_COMPONENTS, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case DUSContracts.UPDATE_GRAPH:
                boolean shouldRetryFetchingUG = DUSContracts.TRUE.equalsIgnoreCase(uri.getQueryParameter(DUSContracts.QUERY_SHOULD_RETRY)) || refreshUpdateGraph.get();
                refreshUpdateGraph.set(false);
                String status = getScreenMaker().refreshUpdateGraph(getContext(), shouldRetryFetchingUG);
                String response = "";
                if (DUSContracts.LOADED.equalsIgnoreCase(status)) {
                    response = getScreenMaker().getUpdateGraphVersion();
                }
                cursor = generateResponse(status, response);
                break;
        }
        if (cursor != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    @NonNull
    private MatrixCursor generateResponse(String status, String response) {
        String[] columnNames = new String[2];
        columnNames[0] = DUSContracts.COLUMN_STATUS;
        columnNames[1] = DUSContracts.COLUMN_RESPONSE;
        MatrixCursor cursor = new MatrixCursor(columnNames, 1);
        String[] row = new String[2];
        row[0] = status;
        row[1] = response;
        cursor.addRow(row);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        getLoggerInstance().log("[SYNC] [INSERT]" + uri.toString());
        switch (sUriMatcher.match(uri)) {
            case DUSContracts.JS_BUNDLE:
                String screenType = uri.getLastPathSegment();
                if (DUSContracts.TRUE.equalsIgnoreCase(uri.getQueryParameter(DUSContracts.QUERY_ERROR))) {
                    ScreenInfo screenInfo = new ScreenInfo();
                    screenInfo.status = DUSContracts.ERROR;
                    mCachedScreenInfo.put(screenType, screenInfo);
                } else {
                    mCachedScreenInfo.remove(screenType);
                }
                break;
            case DUSContracts.UPDATE_GRAPH:
                boolean isError = DUSContracts.TRUE.equalsIgnoreCase(uri.getQueryParameter(DUSContracts.QUERY_ERROR));
                if (isError) {
                    refreshUpdateGraph.set(true);
                }
                for (Map.Entry<String, ScreenInfo> cachedScreenInfo :
                        mCachedScreenInfo.entrySet()) {
                    if (DUSContracts.LOADING.equalsIgnoreCase(cachedScreenInfo.getValue().status)) {
                        boolean shouldRefresh = true;
                        Context context = getContext();
                        if (context != null) {
                            shouldRefresh = getScreenMaker().fetchPage(cachedScreenInfo.getKey(), context);
                        }
                        if (shouldRefresh) {
                            insert(DUSContracts.buildFetchPageUriWithError(cachedScreenInfo.getKey()), null);
                        }
                    }
                }
                 /*Since a new update graph is fetched, we want to clear the memory cache
                  so that the old file paths are deleted */
                mCachedScreenInfo.clear();
                break;
        }
        if (mApplyBatch.get() == null) {
            if (getContext() != null && getContext().getContentResolver() != null) {
                getContext().getContentResolver().notifyChange(uri, null);
            }
        } else {
            mApplyBatch.get().uriToNotify.add(uri);
        }
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, @Nullable String[] selectionArgs) {
        getLoggerInstance().log("[SYNC] [DELETE]" + uri.toString());
        switch (sUriMatcher.match(uri)) {
            case DUSContracts.JS_COMPONENTS:
                getDatabaseHelper().getWritableDatabase().execSQL("DELETE FROM " + TABLE_COMPONENTS + selection);
                break;
            case DUSContracts.JS_BUNDLE:
                if (selectionArgs != null) {
                    for (String screenType :
                            selectionArgs) {
                        mCachedScreenInfo.remove(screenType);
                    }
                    ArrayList<String> filesToKeep = new ArrayList<>(selectionArgs.length);
                    for (String screenType : selectionArgs) {
                        filesToKeep.add(getScreenMaker().getFileKey(screenType));
                    }
                    getFileHelper().deleteRestOfFiles(filesToKeep);
                }
                break;
            case DUSContracts.CLEAR:
                getDatabaseHelper().getWritableDatabase().delete(TABLE_COMPONENTS, null, null);
                getFileHelper().deleteAllFiles();
        }
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        getLoggerInstance().log("[SYNC] [UPDATE] " + uri.toString());
        switch (sUriMatcher.match(uri)) {
            case DUSContracts.UPDATE_GRAPH:
                getScreenMaker().refreshUpdateGraph(getContext(), true);
        }
        return 0;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        getLoggerInstance().log("[SYNC] [BULK_INSERT] " + uri.toString());
        switch (sUriMatcher.match(uri)) {
            case DUSContracts.JS_COMPONENTS: {
                SQLiteDatabase database = getDatabaseHelper().getWritableDatabase();
                database.beginTransaction();
                try {
                    for (ContentValues contentValue :
                            values) {
                        database.insertWithOnConflict(TABLE_COMPONENTS, null, contentValue, SQLiteDatabase.CONFLICT_REPLACE);
                    }
                    database.setTransactionSuccessful();
                } finally {
                    database.endTransaction();
                }
            }
        }
        return values.length;
    }

    @NonNull
    @Override
    public ContentProviderResult[] applyBatch(@NonNull ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        SQLiteDatabase sqLiteDatabase = getDatabaseHelper().getWritableDatabase();
        BatchUriSet batchUriSet = new BatchUriSet(operations.size());
        mApplyBatch.set(batchUriSet);
        try {
            sqLiteDatabase.beginTransaction();
            ContentProviderResult[] result = super.applyBatch(operations);
            sqLiteDatabase.setTransactionSuccessful();
            if (null != getContext()) {
                for (Uri uri : batchUriSet.uriToNotify) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }
            }
            return result;
        } finally {
            sqLiteDatabase.endTransaction();
            mApplyBatch.set(null);
        }
    }

    private static class ScreenInfo {
        @NonNull
        String status;
        @NonNull
        String filePath;
    }

    private DusLogger getLoggerInstance() {
        return DusDependencyResolver.getDUSDependencyResolver(getContext()).getDusLogger();
    }

    private static class BatchUriSet {
        Set<Uri> uriToNotify;

        BatchUriSet(int capacity) {
            this.uriToNotify = new HashSet<>(capacity);
        }

        public BatchUriSet() {
            this.uriToNotify = new HashSet<>();
        }
    }

    @Override
    public void attachInfo(Context context, @NonNull ProviderInfo info) {
        Log.d(TAG, "Setting authority: " + info.authority);
        DUSContracts.initializeAuthority(info.authority);
        Log.d(TAG, "Getting authority: " + DUSContracts.CONTENT_AUTHORITY);
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(DUSContracts.CONTENT_AUTHORITY, DUSContracts.PATH_JS_BUNDLE + "/*", DUSContracts.JS_BUNDLE);
        sUriMatcher.addURI(DUSContracts.CONTENT_AUTHORITY, DUSContracts.PATH_JS_COMPONENTS + "/*", DUSContracts.JS_COMPONENTS);
        sUriMatcher.addURI(DUSContracts.CONTENT_AUTHORITY, DUSContracts.PATH_UPDATEGRAPH, DUSContracts.UPDATE_GRAPH);
        sUriMatcher.addURI(DUSContracts.CONTENT_AUTHORITY, DUSContracts.PATH_CLEAR, DUSContracts.CLEAR);
        super.attachInfo(context, info);
    }
}
