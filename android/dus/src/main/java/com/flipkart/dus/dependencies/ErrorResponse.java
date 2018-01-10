package com.flipkart.dus.dependencies;

/**
 * Error response class
 * Created by surya.kanoria on 18/05/16.
 */
public class ErrorResponse {

    /**
     * error response sent from network or the library
     */
    private String errorResponse;

    private int ErrorCode;

    public String getErrorResponse() {
        return errorResponse;
    }

    public void setErrorResponse(String errorResponse) {
        this.errorResponse = errorResponse;
    }

    public int getErrorCode() {
        return ErrorCode;
    }

    public void setErrorCode(int errorCode) {
        ErrorCode = errorCode;
    }
}