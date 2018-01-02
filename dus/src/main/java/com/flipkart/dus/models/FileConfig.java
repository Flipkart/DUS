package com.flipkart.dus.models;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by surya.kanoria on 09/06/16.
 */
@SuppressWarnings("WeakerAccess")
@UseStag
public class FileConfig {

    @SerializedName("currentUpdateGraph")
    public ArrayMap<String, ArrayList<String>> currentUpdateGraph;

    @SerializedName("nextUpdateGraph")
    public ArrayMap<String, ArrayList<String>> nextUpdateGraph;

    @SerializedName("cutoverTime")
    public long cutoverTime;

    @SerializedName("fileChecksums")
    public HashMap<String, HashMap<String, String>> fileChecksums;

    @SerializedName("currentUpdateGraphVersion")
    public String currentUpdateGraphVersion;

    @SerializedName("nextUpdateGraphVersion")
    public String nextUpdateGraphVersion;

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

    public HashMap<String, HashMap<String, String>> getFileChecksums() {
        return fileChecksums;
    }

    public void setFileChecksums(HashMap<String, HashMap<String, String>> fileChecksums) {
        this.fileChecksums = fileChecksums;
    }

    public ArrayMap<String, ArrayList<String>> getCurrentUpdateGraph() {
        return currentUpdateGraph;
    }

    public void setCurrentUpdateGraph(ArrayMap<String, ArrayList<String>> currentUpdateGraph) {
        this.currentUpdateGraph = currentUpdateGraph;
    }

    @Nullable
    public ArrayMap<String, ArrayList<String>> getNextUpdateGraph() {
        return nextUpdateGraph;
    }

    public void setNextUpdateGraph(ArrayMap<String, ArrayList<String>> nextUpdateGraph) {
        this.nextUpdateGraph = nextUpdateGraph;
    }

    public long getCutoverTime() {
        return cutoverTime;
    }

    public void setCutoverTime(long cutoverTime) {
        this.cutoverTime = cutoverTime;
    }

    @Nullable
    public ArrayMap<String, ArrayList<String>> getActiveUpdateGraph() {
        long currentTime = System.currentTimeMillis() / 1000;
        if (currentTime >= cutoverTime && getNextUpdateGraph() != null) {
            return getNextUpdateGraph();
        } else {
            return getCurrentUpdateGraph();
        }
    }

    @Nullable
    public ArrayMap<String, ArrayList<String>> getInActiveUpdateGraph() {
        long currentTime = System.currentTimeMillis() / 1000;
        if (currentTime < cutoverTime) {
            return getNextUpdateGraph();
        } else {
            return getCurrentUpdateGraph();
        }
    }

    public String getActiveUpdateGraphVersion() {
        long currentTime = System.currentTimeMillis() / 1000;
        if (currentTime >= cutoverTime) {
            return nextUpdateGraphVersion;
        } else {
            return currentUpdateGraphVersion;
        }
    }

    public String getInactiveUpdateGraphVersion() {
        long currentTime = System.currentTimeMillis() / 1000;
        if (currentTime < cutoverTime) {
            return nextUpdateGraphVersion;
        } else {
            return currentUpdateGraphVersion;
        }
    }

    public String getCurrentUpdateGraphVersion() {
        return currentUpdateGraphVersion;
    }

    public void setCurrentUpdateGraphVersion(String currentUpdateGraphVersion) {
        this.currentUpdateGraphVersion = currentUpdateGraphVersion;
    }

    public String getNextUpdateGraphVersion() {
        return nextUpdateGraphVersion;
    }

    public void setNextUpdateGraphVersion(String nextUpdateGraphVersion) {
        this.nextUpdateGraphVersion = nextUpdateGraphVersion;
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
