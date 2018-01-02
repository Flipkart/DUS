package com.flipkart.dus.models;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

/**
 * Created by surya.kanoria on 15/06/16.
 */
@UseStag
public class ComponentMetaModel {

    @SerializedName("value")
    public String mValue;

    @SerializedName("text")
    public String mType;

    public String getValue() {
        return mValue;
    }

    public void setValue(String value) {
        mValue = value;
    }

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        mType = type;
    }
}
