package com.flipkart.dus;

/**
 * Created by surya.kanoria on 28/12/17.
 */

public interface DusApplication {
    DusDependencyResolver getDusDependencyResolver();

    DusReactNativeHost getDusReactNativeHost();
}
