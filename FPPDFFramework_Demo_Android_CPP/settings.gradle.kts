// ---------------------------------------------------------------------------
// FPPDFFramework_Demo_Android_CPP - Android Studio project (minimal C++ demo)
//
// A compact teaching app for integrating the Flyingbee FPPDFFramework Android
// SDK through its C++ API: the app ships its own JNI bridge
// (app/src/main/cpp/FPPDFFramework_jni.cpp) that links manually against the
// self-contained libFPPDFFramework.so unpacked from the AAR (the SDK shape is
// incompatible with AGP's Prefab integration - see app/build.gradle.kts).
//
// The UI is deliberately minimal: it converts a bundled sample PDF to DOCX
// and PNG so the whole native integration path is visible at a glance. For
// the full-featured, zero-native-code integration, open the sibling
// FPPDFFramework_Demo_Android project instead.
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

rootProject.name = "FPPDFFrameworkDemoAndroidCpp"

include(":app")
