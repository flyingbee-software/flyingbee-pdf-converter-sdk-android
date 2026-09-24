package com.flyingbee.FPPDFConverterDemo

import android.app.Application

// ---------------------------------------------------------------------------
// DemoApplication : creates the process-wide ConverterController singleton in
// onCreate so every Activity (Home + the 8 sub-screens) shares the same state,
// mirroring the Java demo's DemoApplication.
// ---------------------------------------------------------------------------

class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ConverterController.init(this)
    }
}
