package com.flipkart.dus;

import android.app.Activity;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.facebook.react.ReactActivityDelegate;
import com.facebook.react.bridge.UiThreadUtil;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

/**
 * Created by surya.kanoria on 08/01/18.
 */

public class DusReactApplicationDelegate extends ReactActivityDelegate {
    private String mBundleName;
    private Context appContext;

    public DusReactApplicationDelegate(Activity activity, @Nullable String mainComponentName, @NonNull String bundleName) {
        super(activity, mainComponentName);
        mBundleName = bundleName;
        appContext = activity;
    }

    @Override
    protected void loadApp(final String appKey) {
        final AtomicBoolean loaded = new AtomicBoolean(false);
        appContext.getContentResolver().registerContentObserver(DUSContracts.buildFetchPageUri(mBundleName), true, new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange, final Uri uri) {
                appContext.getContentResolver().unregisterContentObserver(this);
                AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (DUSContracts.TRUE.equalsIgnoreCase(uri.getQueryParameter(DUSContracts.QUERY_ERROR))) {
                            DusReactApplicationDelegate.super.loadApp(appKey);
                        } else {
                            Cursor requeryCursor = appContext.getContentResolver().query(DUSContracts.buildFetchPageUri(mBundleName), null, null, null, null);
                            if (requeryCursor != null) {
                                requeryCursor.moveToFirst();
                                final String response = requeryCursor.getString(requeryCursor.getColumnIndex(DUSContracts.COLUMN_RESPONSE));
                                if (TextUtils.isEmpty(response)) {
                                    System.out.println("Bundle fetch failed");
                                } else {
                                    if (!loaded.get()) {
                                        loaded.set(true);
                                        ((DusApplication) appContext.getApplicationContext()).getDusReactNativeHost().setJSBundleFile(response);
                                        UiThreadUtil.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                DusReactApplicationDelegate.super.loadApp(appKey);
                                                onResume();
                                            }
                                        });
                                    }
                                }
                                requeryCursor.close();
                            } else {
                                UiThreadUtil.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        DusReactApplicationDelegate.super.loadApp(appKey);
                                        onResume();
                                    }
                                });
                            }
                        }
                    }
                });
            }
        });
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                Uri uri = DUSContracts.buildFetchPageUri(mBundleName);
                final Cursor cursor = appContext.getContentResolver().query(uri, null, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    final int status = Integer.parseInt(cursor.getString(cursor.getColumnIndex(DUSContracts.COLUMN_STATUS)));
                    if (DUSContracts.LOADED == status) {
                        final String response = cursor.getString(cursor.getColumnIndex(DUSContracts.COLUMN_RESPONSE));
                        if (!loaded.get()) {
                            loaded.set(true);
                            ((DusApplication) appContext.getApplicationContext()).getDusReactNativeHost().setJSBundleFile(response);
                            UiThreadUtil.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    DusReactApplicationDelegate.super.loadApp(appKey);
                                    onResume();
                                }
                            });
                        }
                    } else if (DUSContracts.ERROR == status) {
                        System.out.println("Bundle Fetch failed");
                    }
                    cursor.close();
                }
            }
        });
    }
}
