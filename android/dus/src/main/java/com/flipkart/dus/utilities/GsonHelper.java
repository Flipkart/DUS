package com.flipkart.dus.utilities;

import android.support.annotation.Nullable;

import com.flipkart.dus.stag.generated.Stag;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Created by talha.naqvi on 19/07/16.
 */
public class GsonHelper {
    @Nullable
    private static Gson gsonRef = null;
    private static Stag.Factory reactStagFactory;

    public static Stag.Factory getReactStagFactory() {
        if (null == reactStagFactory) {
            reactStagFactory = new Stag.Factory();
        }
        return reactStagFactory;
    }

    @Nullable
    public static Gson getGsonInstance() {
        if (gsonRef == null) {
            gsonRef = new GsonBuilder()
                    .registerTypeAdapterFactory(getReactStagFactory())
                    .create();
        }
        return gsonRef;
    }
}