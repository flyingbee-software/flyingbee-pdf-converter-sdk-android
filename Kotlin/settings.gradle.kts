// ---------------------------------------------------------------------------
// Kotlin - Android Studio project (FPPDFFrameworkDemoAndroidKotlin)
//
// A customer-facing demo app for the Flyingbee FPPDFFramework Android SDK.
// It mirrors the iOS demo (FPPDFFramework_Demo_iOS): the same output formats,
// the same per-format option screens, the same sample-PDF library.
//
// The SDK is consumed exactly the way a customer app should:
//   SDK/libs/flyingbee/FPPDFFramework-10.3.6.aar (shared direct AAR dependency).
// The app drives the SDK purely through its Kotlin API
// (com.flyingbee.FPPDFFramework.*); the AAR's classes.jar and
// jni/<abi>/ native libraries are merged into the APK by AGP. There is no
// native code, NDK or CMake in this project - for the C++ API integration
// path see the sibling CPP project; for the pure-Java counterpart see
// the sibling Java project.
//
//   ./gradlew :app:assembleDebug
// ---------------------------------------------------------------------------

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FPPDFFrameworkDemoAndroidKotlin"

include(":app")
