package com.flipkart.dus.models;

import android.database.Cursor;
import android.support.annotation.NonNull;

import com.flipkart.dus.internals.DatabaseHelper;

/**
 * Created by surya.kanoria on 15/06/16.
 */

@SuppressWarnings("unused")
class HashMeta {

    private String componentKey;

    private String componentValue;

    HashMeta() {
    }

    public HashMeta(@NonNull Cursor cursor) {
        setComponentKey(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COMPONENT_KEY)));
        setComponentValue(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COMPONENT_VALUE)));
    }

    String getComponentKey() {
        return componentKey;
    }

    void setComponentKey(String componentKey) {
        this.componentKey = componentKey;
    }

    public String getComponentValue() {
        return componentValue;
    }

    void setComponentValue(String componentValue) {
        this.componentValue = componentValue;
    }
}
