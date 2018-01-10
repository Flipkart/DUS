package com.flipkart.dus.models;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.ArrayList;

/**
 * Created by surya.kanoria on 09/06/16.
 */
@SuppressWarnings("WeakerAccess")
@UseStag
public class FileConfig {

    @SerializedName("updateGraph")
    public ArrayMap<String, ArrayList<String>> currentUpdateGraph;

    @SerializedName("currentUpdateGraphVersion")
    public String currentUpdateGraphVersion;

    @SerializedName("wipeAll")
    public boolean wipeAll;

    @SerializedName("databaseVersion")
    public int databaseVersion;

    public int getDatabaseVersion() {
        return databaseVersion;
    }

    public void setDatabaseVersion(int databaseVersion) {
        this.databaseVersion = databaseVersion;
    }

    public ArrayMap<String, ArrayList<String>> getCurrentUpdateGraph() {
        return currentUpdateGraph;
    }

    public void setCurrentUpdateGraph(ArrayMap<String, ArrayList<String>> currentUpdateGraph) {
        this.currentUpdateGraph = currentUpdateGraph;
    }


    @Nullable
    public ArrayMap<String, ArrayList<String>> getActiveUpdateGraph() {
        return getCurrentUpdateGraph();
    }

    public String getActiveUpdateGraphVersion() {
        return currentUpdateGraphVersion;
    }

    public String getCurrentUpdateGraphVersion() {
        return currentUpdateGraphVersion;
    }

    public void setCurrentUpdateGraphVersion(String currentUpdateGraphVersion) {
        this.currentUpdateGraphVersion = currentUpdateGraphVersion;
    }

    public boolean isWipeAll() {
        return wipeAll;
    }

    public void setWipeAll(boolean wipeAll) {
        this.wipeAll = wipeAll;
    }

    @Nullable
    public ArrayList<String> getComponents(@NonNull String screenType) {
        ArrayMap<String, ArrayList<String>> componentsMap = getActiveUpdateGraph();
        return null != componentsMap ? componentsMap.get(screenType) : null;
    }
}
