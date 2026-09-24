// ---------------------------------------------------------------------------
// :app - FPPDFFramework demo application (pure Kotlin + Jetpack Compose)
//
// Demonstrates the Flyingbee FPPDFFramework Android SDK end-to-end through its
// Kotlin API layer:
//   * the SDK is consumed as a direct AAR dependency from the repo-root
//     libs/flyingbee/FPPDFFramework-10.3.6.aar (shared by all three demos);
//   * the AAR carries the public Kotlin classes (com.flyingbee.FPPDFFramework)
//     plus the self-contained native libraries (libFPPDFFramework.so and the
//     libFPPDFFrameworkKotlin.so JNI bridge, 4 ABIs), which AGP merges into
//     the APK automatically;
//   * the app contains NO native code: no NDK, no CMake, no JNI of its own.
//     Call sites are in ConverterController.kt (FPPDFFramework.initialize /
//     FPPDFDocument.open / FPPDFConverter.convert).
//
// For the C++ API integration path (manual CMake linking against the same
// AAR), see the sibling CPP project.
//
//   ./gradlew :app:assembleDebug
// ---------------------------------------------------------------------------

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.flyingbee.FPPDFConverterDemo"

    compileSdk = 37

    defaultConfig {
        applicationId = "com.flyingbee.FPPDFConverterDemo"
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
        compose = true
    }

    sourceSets {
        getByName("main") {
            // Resources.bundle is shared by all three demos from the repo-root
            // shared-assets/ folder (see shared-assets/README.md); AGP merges
            // it into the APK's assets alongside the local samples/ folder.
            assets.directories.add("../../shared-assets")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Evaluation APK: no production key yet, sign with debug keystore
            // so the APK is directly installable (same as the debug build
            // used to be).  Add a signingConfigs block with your real
            // keystore when you ship to end users.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
}

dependencies {
    // The Flyingbee PDF Conversion SDK, bundled as a direct AAR file shared by
    // all three demos from the repo-root libs/flyingbee/ folder. The
    // Kotlin API layer ships inside the same AAR (classes.jar + the JNI bridge
    // libraries); nothing else is needed on the consumer side.
    implementation(files("../../libs/flyingbee/FPPDFFramework-10.3.6.aar"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // The SDK's Kotlin API exposes suspend functions / Flows built on
    // coroutines; the demo drives conversions with them.
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
