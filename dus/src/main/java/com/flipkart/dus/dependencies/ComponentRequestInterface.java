package com.flipkart.dus.dependencies;

import java.util.HashMap;
import java.util.List;

/**
 * Created by surya.kanoria on 27/06/16.
 */
public interface ComponentRequestInterface {

    void getResponseString(List<String> componentList, ResponseInterface<HashMap<String, Object>> responseCallback);
}
