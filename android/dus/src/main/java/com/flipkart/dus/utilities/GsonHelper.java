package com.flipkart.dus.utilities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.flipkart.dus.stag.generated.Stag;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Created by talha.naqvi on 19/07/16.
 */
public class GsonHelper {
    @Nullable
    private static Gson gsonRef = null;

    @NonNull
    public static Gson getGsonInstance() {
        if (gsonRef == null) {
            gsonRef = new GsonBuilder()
                    .registerTypeAdapterFactory(new Stag.Factory())
                    .create();
        }
        return gsonRef;
    }

    public static <T> String toJson(TypeAdapter<T> typeAdapter, T value) {
        StringWriter writer = new StringWriter();
        try {
            JsonWriter jsonWriter = getGsonInstance().newJsonWriter(writer);
            typeAdapter.write(jsonWriter, value);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return writer.toString();
    }
}
