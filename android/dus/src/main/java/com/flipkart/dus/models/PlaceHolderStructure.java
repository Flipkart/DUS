package com.flipkart.dus.models;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

/**
 * Created by surya.kanoria on 15/06/16.
 */
@UseStag
public class PlaceHolderStructure {

    @SuppressWarnings("WeakerAccess")
    @SerializedName("fileStructure")
    public List<ComponentMetaModel> mFileStructure;

    public List<ComponentMetaModel> getFileStructure() {
        return mFileStructure;
    }

}
