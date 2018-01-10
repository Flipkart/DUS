package com.flipkart.dus.dependencies;

/**
 * Network response callback interface
 * Created by talha.naqvi on 03/06/16.
 */
public interface ResponseInterface<T> {

    /**
     * Get called back on network success
     * @param networkResponse generic type
     */
    void OnSuccess(T networkResponse);

    /**
     * Error response, refer {@link ErrorResponse}
     * @param errorObj
     */
    void OnFailure(ErrorResponse errorObj);
}
