// ---------------------------------------------------------------------------
// :app - FPPDFFramework demo application (pure Java + XML layouts)
//
// Demonstrates the Flyingbee FPPDFFramework Android SDK end-to-end through its
// Java API layer:
//   * the SDK is consumed as a direct AAR dependency from the repo-root
//     SDK/libs/flyingbee/FPPDFFramework-10.3.6.aar (shared by all three demos);
//   * the AAR carries the public Java classes
//     (com.flyingbee.FPPDFFramework.Java.*) plus the self-contained native
//     libraries (libFPPDFFramework.so and the libFPPDFFrameworkJava.so JNI
//     bridge, 4 ABIs), which AGP merges into the APK automatically;
//   * the app contains NO native code: no NDK, no CMake, no JNI of its own.
//     Call sites are in ConverterController.java (FPPDFFramework.initialize /
//     FPPDFDocument.open / FPPDFConverter.convertAsync).
//
// This is the Java counterpart of the Kotlin + Jetpack Compose demo; the UI is
// built with XML layouts + Material Components instead.
//
//   ./gradlew :app:assembleDebug
// ---------------------------------------------------------------------------

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.flyingbee.FPPDFConverterDemoJava"

    compileSdk = 37

    defaultConfig {
        applicationId = "com.flyingbee.FPPDFConverterDemoJava"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "10.3.6"

        ndk {
            // Match the ABIs shipped inside FPPDFFramework.aar.
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
        }
    }

    buildFeatures {
        viewBinding = true
    }

    sourceSets {
        getByName("main") {
            // Resources.bundle and the samples/ PDF library are shared by all
            // three demos from the repo-root SDK/assets/ folder (see
            // SDK/assets/README.md); AGP merges them into the APK's assets.
            assets.directories.add("../../SDK/assets")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Evaluation APK: no production key yet, sign with debug keystore
            // so the APK is directly installable.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // The Flyingbee PDF Conversion SDK, bundled as a direct AAR file shared by
    // all three demos from the repo-root SDK/libs/flyingbee/ folder. The
    // Java API layer ships inside the same AAR (classes.jar + the JNI bridge
    // libraries); nothing else is needed on the consumer side.
    implementation(files("../../SDK/libs/flyingbee/FPPDFFramework-10.3.6.aar"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
}
