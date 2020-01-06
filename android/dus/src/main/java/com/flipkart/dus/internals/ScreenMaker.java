package com.flipkart.dus.internals;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import android.text.TextUtils;

import com.flipkart.dus.DUSConstants;
import com.flipkart.dus.DUSContracts;
import com.flipkart.dus.DusDependencyResolver;
import com.flipkart.dus.dependencies.DusLogger;
import com.flipkart.dus.dependencies.ErrorResponse;
import com.flipkart.dus.dependencies.ResponseInterface;
import com.flipkart.dus.models.ComponentMetaModel;
import com.flipkart.dus.models.PlaceHolderStructure;
import com.flipkart.dus.models.PlaceHolderStructure$TypeAdapter;
import com.flipkart.dus.utilities.FileHelper;
import com.flipkart.dus.utilities.GsonHelper;
import com.google.gson.TypeAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by surya.kanoria on 30/01/17.
 */

public class ScreenMaker {
    @NonNull
    private final HashSet<String> screenTypeBeingProcessed = new HashSet<>();
    private FileHelper mFileHelper;
    private ComponentDownloader mComponentDownloader;
    private UpdateGraphManager mUpdateGraphManager;

    public ScreenMaker(@NonNull UpdateGraphManager updateGraphManager, @NonNull ComponentDownloader componentDownloader, @NonNull Context context) {
        mFileHelper = new FileHelper(context, DUSConstants.JS_RESOURCE_DIRECTORY);
        mComponentDownloader = componentDownloader;
        mUpdateGraphManager = updateGraphManager;
    }

    public boolean fetchPage(@NonNull final String screenType, @NonNull final Context context) {
        boolean refreshGraph = false;
        boolean shouldProcessRequest = true;
        synchronized (screenTypeBeingProcessed) {
            if (screenTypeBeingProcessed.contains(screenType)) {
                shouldProcessRequest = false;
            } else {
                screenTypeBeingProcessed.add(screenType);
            }
        }
        if (shouldProcessRequest) {
            final ArrayList<String> componentsKeys = mUpdateGraphManager.getComponents(screenType);
            if (componentsKeys == null) {
                refreshGraph = true;
                synchronized (screenTypeBeingProcessed) {
                    screenTypeBeingProcessed.remove(screenType);
                }
            } else {
                AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                    @Override
                    public void run() {
                        fetchComponents(screenType, context, componentsKeys);
                    }
                });
            }
        }
        return refreshGraph;
    }


    private void fetchComponents(@NonNull final String screenName, @NonNull final Context context, @NonNull final ArrayList<String> componentsKeys) {
        ArrayList<String> keysToDownload = new ArrayList<>(componentsKeys.size());
        keysToDownload.addAll(componentsKeys);
        final ArrayMap<String, String> componentMap = new ArrayMap<>(componentsKeys.size());
        try {
            Cursor cachedComponents = context.getContentResolver().query(DUSContracts.buildFetchContentsUri(screenName), null, DUSContracts.getComponentsQuery(componentsKeys), null, null);
            if (cachedComponents != null) {
                if (cachedComponents.getCount() > 0) {
                    cachedComponents.moveToFirst();
                    do {
                        String key = cachedComponents.getString(cachedComponents.getColumnIndex(DatabaseHelper.COMPONENT_KEY));
                        String value = cachedComponents.getString(cachedComponents.getColumnIndex(DatabaseHelper.COMPONENT_VALUE));
                        componentMap.put(key, value);
                        keysToDownload.remove(key);
                    } while (cachedComponents.moveToNext());
                }
                cachedComponents.close();
            }
        } catch (SQLException e) {
            //DO nothing as the chunks would get downloaded.
        }

        if (keysToDownload.size() == 0) {
            createPage(componentMap, componentsKeys, screenName, context);
        } else {
            downloadMissingComponents(keysToDownload, screenName, new ResponseInterface<Map<String, String>>() {
                @Override
                public void OnSuccess(@NonNull final Map<String, String> networkResponse) {
                    componentMap.putAll(networkResponse);
                    createPage(componentMap, componentsKeys, screenName, context);
                    addMissingComponentsToDB(networkResponse, screenName, context);
                }

                @Override
                public void OnFailure(@Nullable ErrorResponse errorObj) {
                    String error = errorObj != null ? errorObj.getErrorResponse() : "";
                    getCrashLoggerInstance(context).log("[SYNC] Components download failed. Error: " + error);
                    sendErrorResponse(screenName, context);
                }
            });
        }
    }


    private void downloadMissingComponents(List<String> keys, String screenType, ResponseInterface<Map<String, String>> callbackInterface) {
        DownloadJob downloadJob = new DownloadJob(callbackInterface);
        downloadJob.setComponentKeys(keys);
        downloadJob.setScreenType(screenType);
        mComponentDownloader.addToDownloadQueue(downloadJob);
    }

    private void createPage(@NonNull ArrayMap<String, String> componentKeys, @NonNull List<String> keys, @NonNull String screenName, @NonNull Context context) {
        String componentStructureJson = componentKeys.get(keys.get(0));
        if (!TextUtils.isEmpty(componentStructureJson)) {
            PlaceHolderStructure componentStructure = null;
            try {
                TypeAdapter<PlaceHolderStructure> typeAdapter = GsonHelper.getGsonInstance().getAdapter(PlaceHolderStructure$TypeAdapter.TYPE_TOKEN);
                componentStructure = typeAdapter.fromJson(componentStructureJson);
            } catch (Exception e) {
                sendErrorResponse(screenName, context);
            }
            if (componentStructure != null) {
                StringBuilder page = new StringBuilder();
                for (ComponentMetaModel component :
                        componentStructure.getFileStructure()) {
                    String componentString = getValue(component, componentKeys);
                    if (!TextUtils.isEmpty(componentString)) {
                        page.append(componentString);
                    } else {
                        sendErrorResponse(screenName, context);
                    }
                }
                createPageAndNotify(screenName, page.toString(), context);
            }
        } else {
            StringBuilder page = new StringBuilder();
            for (String hash :
                    keys) {
                String component = componentKeys.get(hash);
                if (!TextUtils.isEmpty(component)) {
                    page.append(component);
                }
            }
            createPageAndNotify(screenName, page.toString(), context);
        }
    }

    private void addMissingComponentsToDB(@NonNull Map<String, String> componentMap, String screenName, @NonNull Context context) {
        ContentValues[] contentValues = new ContentValues[componentMap.size()];
        int i = 0;
        for (Map.Entry<String, String> component : componentMap.entrySet()) {
            ContentValues contentValue = new ContentValues();
            contentValue.put(DatabaseHelper.COMPONENT_KEY, component.getKey());
            contentValue.put(DatabaseHelper.COMPONENT_VALUE, component.getValue());
            contentValues[i] = contentValue;
            i++;
        }
        context.getContentResolver().bulkInsert(DUSContracts.buildFetchContentsUri(screenName), contentValues);
    }

    @Nullable
    private String getValue(@Nullable ComponentMetaModel model, @NonNull ArrayMap<String, String> componentMap) {
        if (model != null && model.getType() != null) {
            if (model.getType().contentEquals("text")) {
                return model.getValue();
            } else {
                String key = model.getValue();
                if (componentMap.containsKey(key)) {
                    return componentMap.get(key);
                }
            }
        }
        return null;
    }

    private void sendErrorResponse(String screenName, @NonNull Context context) {
        synchronized (screenTypeBeingProcessed) {
            screenTypeBeingProcessed.remove(screenName);
        }
        context.getContentResolver().insert(DUSContracts.buildFetchPageUriWithError(screenName), null);
    }

    private void createPageAndNotify(@NonNull String screenName, @NonNull String page, @NonNull Context context) {
        try {
            String filePath = mFileHelper.createFile(mUpdateGraphManager.generateFileKey(screenName), page);
            ContentValues contentValues = new ContentValues();
            contentValues.put(DUSContracts.FILE_PATH, filePath);
            context.getContentResolver().insert(DUSContracts.buildFetchPageUri(screenName), contentValues);
            synchronized (screenTypeBeingProcessed) {
                screenTypeBeingProcessed.remove(screenName);
            }
        } catch (IOException e) {
            sendErrorResponse(screenName, context);
        }
    }

    public int getUpdateGraphStatus() {
        return mUpdateGraphManager.getUpdateGraphStatus();
    }

    @NonNull
    public String getFileKey(@NonNull String screenType) {
        return mUpdateGraphManager.generateFileKey(screenType);
    }

    public int refreshUpdateGraph(@NonNull Context context, boolean shouldRetry) {
        return mUpdateGraphManager.refreshUpdateGraph(context, shouldRetry);
    }

    public int resetUpdateGraph(@NonNull Context context, boolean shouldRetry) {
        return mUpdateGraphManager.resetUpdateGraph(context, shouldRetry);
    }

    @NonNull
    public String getUpdateGraphVersion() {
        return mUpdateGraphManager.getUpdateGraphVersion();
    }

    private DusLogger getCrashLoggerInstance(@NonNull Context context) {
        return DusDependencyResolver.getDUSDependencyResolver(context).getDusLogger();
    }
}
