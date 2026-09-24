// ---------------------------------------------------------------------------
// Java - Android Studio project (FPPDFFrameworkDemoAndroidJava)
//
// A customer-facing demo app for the Flyingbee FPPDFFramework Android SDK.
// It is the pure-Java counterpart of the sibling Kotlin demo: identical
// screens, the same output formats, the same per-format option screens,
// the same sample-PDF library — but built with Java + XML layouts + Material
// Components instead of Jetpack Compose.
//
// The SDK is consumed exactly the way a customer app should:
//   SDK/libs/flyingbee/FPPDFFramework-10.3.6.aar (shared direct AAR dependency).
// The app drives the SDK purely through its Java API
// (com.flyingbee.FPPDFFramework.Java.*); the AAR's classes.jar and
// jni/<abi>/ native libraries are merged into the APK by AGP. There is no
// native code, NDK or CMake in this project - for the C++ API integration
// path see the sibling CPP project.
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

rootProject.name = "FPPDFFrameworkDemoAndroidJava"

include(":app")
