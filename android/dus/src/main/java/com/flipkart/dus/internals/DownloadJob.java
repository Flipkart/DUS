package com.flipkart.dus.internals;

import androidx.annotation.NonNull;

import com.flipkart.dus.dependencies.ErrorResponse;
import com.flipkart.dus.dependencies.ResponseInterface;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by surya.kanoria on 15/06/16.
 */
public class DownloadJob {
    private final ResponseInterface<Map<String, String>> callbackInterface;
    @NonNull
    private final HashMap<String, String> mNetworkResponse;
    private List<String> mComponentKeys;
    private String mScreenType;

    public DownloadJob(ResponseInterface<Map<String, String>> callbackInterface) {
        this.mNetworkResponse = new HashMap<>();
        this.callbackInterface = callbackInterface;
    }

    public List<String> getComponentKeys() {
        return mComponentKeys;
    }

    public void setComponentKeys(List<String> componentKeys) {
        this.mComponentKeys = componentKeys;
    }

    public String getScreenType() {
        return mScreenType;
    }

    public void setScreenType(String screenType) {
        this.mScreenType = screenType;
    }

    public void onSuccess(String key, String networkResponse) {
        boolean sendCallBack = false;
        synchronized (this) {
            mComponentKeys.remove(key);
            mNetworkResponse.put(key, networkResponse);
            if (mComponentKeys.size() == 0) {
                sendCallBack = true;
            }
        }
        if (sendCallBack) {
            callbackInterface.OnSuccess(mNetworkResponse);
        }
    }

    public void onFailure(ErrorResponse errorObj) {
        callbackInterface.OnFailure(errorObj);
    }

}
