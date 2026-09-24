package com.flyingbee.FPPDFConverterDemoJava;

import android.app.Application;

/**
 * Boots the process-wide {@link ConverterController} singleton so every screen
 * shares the same state (the Java counterpart of the Kotlin demo's shared
 * AndroidViewModel).
 */
public final class DemoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ConverterController.init(this);
    }
}
