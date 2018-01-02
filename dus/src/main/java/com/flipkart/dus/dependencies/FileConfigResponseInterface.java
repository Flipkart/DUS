package com.flipkart.dus.dependencies;


import com.flipkart.dus.models.FileConfig;

/**
 * Created by surya.kanoria on 29/07/16.
 */
public interface FileConfigResponseInterface {
    void onSuccess(FileConfig fileConfig, String configVersion);
    void onFailure(ErrorResponse errorResponse);
}
