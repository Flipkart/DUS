package com.flipkart.dus.utilities;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.flipkart.dus.DusDependencyResolver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by surya.kanoria on 22/06/16.
 */
public class FileHelper {

    @NonNull
    private final File mDirectory;
    @NonNull
    private final Context currentContext;

    public FileHelper(@NonNull Context context, @NonNull String directoryName) {
        mDirectory = new File(context.getFilesDir(), directoryName);
        //noinspection ResultOfMethodCallIgnored
        mDirectory.mkdirs();
        currentContext = context;
    }

    @Nullable
    public String createFile(@NonNull String fileName, @NonNull String content) throws IOException {
        File file = new File(mDirectory, HashUtilities.md5(fileName));
        FileOutputStream outputStream = new FileOutputStream(file);
        outputStream.write(content.getBytes());
        outputStream.close();
        File renamedFile = new File(mDirectory, fileName);
        if (file.renameTo(renamedFile)) {
            DusDependencyResolver.getDUSDependencyResolver(currentContext).getDusLogger().
                    log("getPath: " + renamedFile.getPath());
            return renamedFile.getPath();
        }
        return null;
    }

    @Nullable
    public String getFilePath(@NonNull final String fileName) {
        File[] files = mDirectory.listFiles(new FileFilter() {
            @Override
            public boolean accept(@NonNull File pathname) {
                return !TextUtils.isEmpty(pathname.getPath()) && pathname.getPath().contains(fileName);
            }
        });
        if (files.length > 0) {
            return files[0].getPath();
        }
        return null;
    }

    @NonNull
    public String readFile(@NonNull String fileName) {
        try {
            FileInputStream inputStream = new FileInputStream(mDirectory.getPath() + "/" + fileName);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder fileOutput = new StringBuilder();
            String receivedString;
            while ((receivedString = bufferedReader.readLine()) != null) {
                fileOutput.append(receivedString);
            }
            inputStream.close();
            return fileOutput.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void deleteFile(@NonNull final String fileName) {
        File[] files = mDirectory.listFiles(new FileFilter() {
            @Override
            public boolean accept(@NonNull File pathname) {
                return (pathname.getName().contains(fileName));
            }
        });
        if (files != null) {
            for (File file :
                    files) {
                file.delete();
            }
        }
    }

    @NonNull
    public List<String> getFileList() {
        ArrayList<String> fileList = new ArrayList<>();
        String[] files = mDirectory.list();
        Collections.addAll(fileList, files);
        return fileList;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void deleteFiles(@NonNull final List<String> fileList) {
        File[] files = mDirectory.listFiles(new FileFilter() {
            @Override
            public boolean accept(@NonNull File pathname) {
                for (String file :
                        fileList) {
                    if (pathname.getPath().contains(file)) {
                        return true;
                    }
                }
                return false;
            }
        });
        for (File file :
                files) {
            file.delete();
        }
    }

    public void deleteRestOfFiles(@NonNull final List<String> fileList) {
        File[] files = mDirectory.listFiles(new FileFilter() {
            @Override
            public boolean accept(@NonNull File pathname) {
                for (String file :
                        fileList) {
                    if (pathname.getPath().contains(file)) {
                        return false;
                    }
                }
                return true;
            }
        });
        for (File file :
                files) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    public void deleteAllFiles() {
        File[] files = mDirectory.listFiles();
        for (File file :
                files) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }
}
