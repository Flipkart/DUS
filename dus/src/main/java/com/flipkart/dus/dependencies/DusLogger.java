package com.flipkart.dus.dependencies;

/**
 * Created by surya.kanoria on 17/08/16.
 */
public interface DusLogger {
    void log(String log);
    void logException(Throwable throwable);
}
