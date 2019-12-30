package com.flipkart.dus.internals;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.flipkart.dus.DUSConstants;
import com.flipkart.dus.DUSContracts;
import com.flipkart.dus.DusDependencyResolver;
import com.flipkart.dus.dependencies.DusLogger;
import com.flipkart.dus.dependencies.ErrorResponse;
import com.flipkart.dus.dependencies.FileConfigRequestInterface;
import com.flipkart.dus.dependencies.FileConfigResponseInterface;
import com.flipkart.dus.models.FileConfig;
import com.flipkart.dus.utilities.HashUtilities;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by surya.kanoria on 07/02/17.
 */

public class UpdateGraphManager {
    @Nullable
    private FileConfig mFileConfig;
    private FileConfigHelper mFileConfigHelper;
    @NonNull
    private AtomicInteger updateGraphStatus = new AtomicInteger(DUSConstants.NONE);
    private FileConfigRequestInterface mFileConfigRequestInterface;


    public UpdateGraphManager(@NonNull FileConfigHelper fileConfigHelper, @NonNull FileConfigRequestInterface fileConfigRequestInterface, @NonNull Context context) {
        mFileConfigHelper = fileConfigHelper;
        mFileConfigRequestInterface = fileConfigRequestInterface;
        if (mFileConfigHelper.shouldOptimize() && getUpdateGraph() != null) {
            optimizeCache(getUpdateGraph(), context);
        }
    }


    public int refreshUpdateGraph(@NonNull Context context, boolean shouldRetry) {
        int response;
        switch (updateGraphStatus.get()) {
            case DUSConstants.CACHED:
                if (shouldRetry) {
                    updateGraphStatus.set(DUSConstants.DOWNLOADING);
                    downloadNewUpdateGraph(context);
                    response = DUSContracts.LOADING;
                } else {
                    response = DUSContracts.LOADED;
                }
                break;
            case DUSConstants.ERROR:
                if (shouldRetry) {
                    updateGraphStatus.set(DUSConstants.DOWNLOADING);
                    downloadNewUpdateGraph(context);
                    response = DUSContracts.LOADING;
                } else {
                    response = DUSContracts.ERROR;
                }
                break;
            case DUSConstants.DOWNLOADING:
                response = DUSContracts.LOADING;
                break;
            case DUSConstants.NONE:
                updateGraphStatus.set(DUSConstants.DOWNLOADING);
                downloadNewUpdateGraph(context);
                response = DUSContracts.LOADING;
                break;
            default:
                throw new IllegalStateException("Illegal state of update graph: " + updateGraphStatus.get());
        }
        return response;
    }

    public int getUpdateGraphStatus() {
        return updateGraphStatus.get();
    }

    private void downloadNewUpdateGraph(@NonNull final Context context) {
        final String updateGraphVersion = mFileConfigHelper.getFileConfigVersion();
        mFileConfigRequestInterface.getResponseString(DUSConstants.UPDATE_GRAPH_NAME, updateGraphVersion, new FileConfigResponseInterface() {
            @Override
            public void onSuccess(@NonNull FileConfig fileConfig, final String configVersion) {
                mFileConfig = fileConfig;
                updateGraphStatus.set(DUSConstants.CACHED);
                int cachedDbVersion = mFileConfigHelper.getDatabaseVersion(DusDependencyResolver.getDUSDependencyResolver(context).getPackagedDbVersion());
                if (fileConfig.getDatabaseVersion() > cachedDbVersion) {
                    context.getContentResolver().delete(DUSContracts.buildWipeAll(), null, null);
                    mFileConfigHelper.setDatabaseVersion(fileConfig.getDatabaseVersion());

                }
                context.getContentResolver().insert(DUSContracts.buildFetchUpdateGraphUriWithError("false"), null);
                mFileConfigHelper.setShouldOptimize(true);
                mFileConfigHelper.updateFileConfig(mFileConfig);
                mFileConfigHelper.updateFileConfigVersion(configVersion);
                getLoggerInstance(context).log("[SYNC] Update graph downloaded. Version: " + configVersion);
            }

            @Override
            public void onFailure(@Nullable ErrorResponse errorResponse) {
                if (errorResponse != null && errorResponse.getErrorCode() == 204) {
                    updateGraphStatus.set(DUSConstants.CACHED);
                    getLoggerInstance(context).log("[SYNC] Getting 204 for UG version: " + getUpdateGraphVersion());
                    context.getContentResolver().insert(DUSContracts.buildFetchUpdateGraphUriWithError("false"), null);
                } else {
                    updateGraphStatus.set(DUSConstants.ERROR);
                    String error = errorResponse != null ? errorResponse.getErrorResponse() : "";
                    getLoggerInstance(context).log("[SYNC] Update graph download error. Error: " + error);
                    context.getContentResolver().insert(DUSContracts.buildFetchUpdateGraphUriWithError("true"), null);
                }
            }
        });
    }

    private void optimizeCache(@NonNull FileConfig fileConfig, @NonNull Context context) {
        ArrayList<String> componentList = new ArrayList<>(600);
        for (Map.Entry<String, ArrayList<String>> screenList :
                fileConfig.getCurrentUpdateGraph().entrySet()) {
            componentList.addAll(screenList.getValue());
        }
        String[] fileList = new String[fileConfig.getCurrentUpdateGraph().size()];
        context.getContentResolver().delete(DUSContracts.buildFetchContentsUri(""), DUSContracts.getOptimizeStorageWhereQuery(componentList), null);
        int i = 0;
        for (String file : fileConfig.getCurrentUpdateGraph().keySet()) {
            fileList[i] = generateFileKey(file);
            i++;
        }
        context.getContentResolver().delete(DUSContracts.buildFetchPageUri(""), null, fileList);
        mFileConfigHelper.setShouldOptimize(false);
    }

    @Nullable
    private FileConfig getUpdateGraph() {
        if (mFileConfig == null) {
            mFileConfig = mFileConfigHelper.getActiveConfig();
        }
        /* Incase saved file gets corrupted, we will clear everything so that a fresh sync
        * so that a fresh sync can be triggered*/
        if (mFileConfig == null || mFileConfig.getCurrentUpdateGraph() == null) {
            mFileConfig = null;
            mFileConfigHelper.clear();
        }
        return mFileConfig;
    }

    @NonNull
    public String generateFileKey(String screenType) {
        String response = screenType;
        String ugVersion = getUpdateGraph() != null ? getUpdateGraph().getActiveUpdateGraphVersion() : "";
        if (!TextUtils.isEmpty(ugVersion)) {
            response = screenType + HashUtilities.md5(ugVersion);
        }
        return response;
    }

    @Nullable
    public ArrayList<String> getComponents(@NonNull String screenType) {
        FileConfig fileConfig = getUpdateGraph();
        return null != fileConfig ? fileConfig.getComponents(screenType) : null;
    }

    @NonNull
    public String getUpdateGraphVersion() {
        String updateGraphVersion = "";
        if (getUpdateGraph() != null) {
            updateGraphVersion = getUpdateGraph().getActiveUpdateGraphVersion();
        }
        return updateGraphVersion;
    }

    private DusLogger getLoggerInstance(@NonNull Context context) {
        return DusDependencyResolver.getDUSDependencyResolver(context).getDusLogger();
    }
}
