package com.flipkart.dus.internals;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.flipkart.dus.utilities.FileHelper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by surya.kanoria on 22/07/16.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    //Component Table Column Names
    @NonNull
    public static final String COMPONENT_KEY = "componentKey";
    @NonNull
    public static final String COMPONENT_VALUE = "componentValue";
    @NonNull
    private static final String SHARED_PREFERENCES_KEY = "ReactDatabaseHelper";
    @NonNull
    private static final String DATABASE_IMPORTED = "DatabaseImported";
    @NonNull
    private static final String DATABASE_NAME = "ReactNative";

    //Database Version
    private static final int DATABASE_VERSION = 1;

    //Table names
    @NonNull
    public static final String TABLE_COMPONENTS = "componentMeta";

    @NonNull
    private final String mImportDatabaseName;
    @NonNull
    private final Context mContext;
    private String databasePath;
    private SharedPreferences mSharedPreferences;
    @NonNull
    private FileHelper fileHelper;


    public DatabaseHelper(@NonNull Context context, @NonNull FileHelper fileHelper, @NonNull String importDatabaseName, int importDatabaseVersion) {
        super(context, DATABASE_NAME, null, importDatabaseVersion);
        this.fileHelper = fileHelper;
        if (context.getApplicationInfo() != null) {
            this.databasePath = context.getApplicationInfo().dataDir + "/databases/" + DATABASE_NAME;
        }
        this.mContext = context;
        mSharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        mImportDatabaseName = importDatabaseName;
    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        synchronized (this) {
            if (isDatabaseNotImported()) {
                copyDatabase(mImportDatabaseName, databasePath);
            }
            return super.getReadableDatabase();
        }
    }

    public SQLiteDatabase getWritableDatabase() {
        synchronized (this) {
            if (isDatabaseNotImported()) {
                copyDatabase(mImportDatabaseName, databasePath);
            }
            return super.getWritableDatabase();
        }
    }


    @Override
    public void onCreate(@NonNull SQLiteDatabase db) {
        String CREATE_COMPONENTS_TABLE = "CREATE TABLE " + TABLE_COMPONENTS + "("
                + COMPONENT_KEY + " TEXT PRIMARY KEY," + COMPONENT_VALUE + " TEXT );";
        db.execSQL(CREATE_COMPONENTS_TABLE);
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COMPONENTS);
        onCreate(db);
        fileHelper.deleteAllFiles();
        //TODO: Set is database upgraded to false so that new database could be copied
    }

    private void copyDatabase(@Nullable String importDatabaseName, @NonNull String databasePath) {
        if (TextUtils.isEmpty(importDatabaseName)) {
            setIsDatabaseImported();
            return;
        }
        boolean isDatabaseCopied = true;
        OutputStream fileOutputStream = null;
        InputStream dbInputFile = null;
        try {
            dbInputFile = mContext.getAssets().open(importDatabaseName);
            fileOutputStream = new FileOutputStream(databasePath);

            byte[] buffer = new byte[8096];
            int length;
            while ((length = dbInputFile.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, length);
            }
        } catch (IOException e) {
            isDatabaseCopied = false;
            e.printStackTrace();
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.flush();
                }
            } catch (IOException e) {
                isDatabaseCopied = false;
                e.printStackTrace();
            }
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                isDatabaseCopied = false;
                e.printStackTrace();
            }
            try {
                if (dbInputFile != null) {
                    dbInputFile.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (isDatabaseCopied) {
            setIsDatabaseImported();
        }
    }

    private boolean isDatabaseNotImported() {
        return !mSharedPreferences.getBoolean(DATABASE_IMPORTED, false);
    }

    private void setIsDatabaseImported() {
        mSharedPreferences.edit().putBoolean(DATABASE_IMPORTED, true).apply();
    }
}
