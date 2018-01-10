package com.flipkart.dus.dependencies;

/**
 * Created by surya.kanoria on 29/06/16.
 */
public interface FileConfigRequestInterface {

    void getResponseString(String updateGraphName, String updateGraphVersion, FileConfigResponseInterface responseCallback);
}
