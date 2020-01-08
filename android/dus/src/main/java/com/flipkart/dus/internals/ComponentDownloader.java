package com.flipkart.dus.internals;


import androidx.annotation.NonNull;

import com.flipkart.dus.dependencies.ErrorResponse;
import com.flipkart.dus.dependencies.ComponentRequestInterface;
import com.flipkart.dus.dependencies.ResponseInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by surya.kanoria on 06/06/16.
 */
public class ComponentDownloader {
    private final ComponentRequestInterface mNetworkInterface;
    @NonNull
    private final HashMap<String, ArrayList<DownloadJob>> mCallbackMap;

    public ComponentDownloader(ComponentRequestInterface networkInterface) {
        this.mNetworkInterface = networkInterface;
        this.mCallbackMap = new HashMap<>();
    }

    public void addToDownloadQueue(@NonNull final DownloadJob downloadJob) {
        final HashSet<String> downloadList = new HashSet<>(downloadJob.getComponentKeys());
        synchronized (mCallbackMap) {
            for (String key :
                    downloadJob.getComponentKeys()) {
                ArrayList<DownloadJob> callbackList = mCallbackMap.get(key);
                if (callbackList != null) {
                    callbackList.add(downloadJob);
                    downloadList.remove(key);
                } else {
                    callbackList = new ArrayList<>();
                    callbackList.add(downloadJob);
                    mCallbackMap.put(key, callbackList);
                }
            }
        }

        mNetworkInterface.getResponseString(new ArrayList<>(downloadList), new ResponseInterface<HashMap<String, String>>() {
            @Override
            public void OnSuccess(@NonNull HashMap<String, String> networkResponse) {
                if (networkResponse.size() != downloadList.size()) {
                    ErrorResponse errorResponse = new ErrorResponse();
                    errorResponse.setErrorResponse("Could not fetch all files");
                    clearCallbackMap();
                    downloadJob.onFailure(errorResponse);
                } else {
                    for (Map.Entry<String, String> entry :
                            networkResponse.entrySet()) {
                        if (entry != null && entry.getValue() != null && entry.getKey() != null) {
                            downloadList.remove(entry.getKey());
                            ArrayList<DownloadJob> callbackList;
                            synchronized (mCallbackMap) {
                                callbackList = mCallbackMap.get(entry.getKey());
                                mCallbackMap.remove(entry.getKey());
                            }
                            for (DownloadJob callbackInterface :
                                    callbackList) {
                                callbackInterface.onSuccess(entry.getKey(), entry.getValue());
                            }
                        }
                    }
                    if (downloadList.size() > 0) {
                        ErrorResponse errorResponse = new ErrorResponse();
                        errorResponse.setErrorResponse("Could not fetch all files");
                        clearCallbackMap();
                        downloadJob.onFailure(errorResponse);
                    }
                }
            }

            @Override
            public void OnFailure(ErrorResponse errorObj) {
                clearCallbackMap();
                downloadJob.onFailure(errorObj);
            }

            private void clearCallbackMap() {
                HashSet<DownloadJob> callbackList = new HashSet<>();
                synchronized (mCallbackMap) {
                    for (String key :
                            downloadList) {
                        ArrayList<DownloadJob> downloadJobList = mCallbackMap.get(key);
                        if (downloadJobList != null)
                            callbackList.addAll(downloadJobList);
                        mCallbackMap.remove(key);
                    }
                }
                for (DownloadJob downloadJob :
                        callbackList) {
                    if (downloadJob != null) {
                        downloadJob.onFailure(new ErrorResponse());
                    }
                }
            }
        });
    }
}

