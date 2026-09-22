// ---------------------------------------------------------------------------
// :app - FPPDFFramework C++ API demo (minimal)
//
// Demonstrates integrating the Flyingbee FPPDFFramework Android SDK through
// its C++ API instead of the bundled Kotlin layer:
//   * the SDK is consumed as a direct AAR dependency from
//     app/libs/flyingbee/FPPDFFramework-10.3.6.aar.  AGP merges the AAR's
//     jni/<abi>/libFPPDFFramework.so into the APK automatically;
//   * app/src/main/cpp/FPPDFFramework_jni.cpp is the app's own JNI bridge that
//     exposes the C++ API (FPPDFDocument / FPPDF2AllConverter / FPPDFOptions)
//     to Kotlin (FPPDFNative.kt).
//
// Why the native link is manual instead of Prefab:
//   libFPPDFFramework.so is a *shared* library with a privately, statically
//   linked libc++ (self-contained delivery, no libc++_shared.so to ship).
//   AGP's Prefab integration categorically rejects that combination for any
//   consumer ("Library is a shared library with a statically linked STL and
//     cannot be used with any library using the STL" / "User requested no STL
//   but library requires libc++"), so the demo unpacks the AAR at build time
//   (task unpackFppdfSdk) and links the imported .so + headers directly in
//   src/main/cpp/CMakeLists.txt.  The public API only crosses the boundary in
//   C types and PODs, so two independent libc++ copies can never interact.
//
// For the zero-native-code integration path, see the sibling
// FPPDFFramework_Demo_Android project (pure Kotlin API).
//
//   ./gradlew :app:assembleDebug
// ---------------------------------------------------------------------------

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// The bundled SDK AAR, unpacked once per build into build/fppdf-sdk so the
// native build can link against jni/<abi>/libFPPDFFramework.so and include
// the three public headers from prefab/modules/FPPDFFramework/include.
val sdkAar = file("libs/flyingbee/FPPDFFramework-10.3.6.aar")
val sdkUnpackDir = layout.buildDirectory.dir("fppdf-sdk")

val unpackFppdfSdk = tasks.register<Copy>("unpackFppdfSdk") {
    group = "native"
    description = "Unpacks FPPDFFramework.aar into build/fppdf-sdk for the native link step."
    from(zipTree(sdkAar))
    into(sdkUnpackDir)
}

android {
    namespace = "com.flyingbee.FPPDFConverterDemoCpp"

    // The NDK that was used to validate this project. Bump it together with
    // the NDK shipped in Android Studio.
    ndkVersion = "28.2.13676358"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.flyingbee.FPPDFConverterDemoCpp"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "10.3.6"

        ndk {
            // Match the ABIs shipped inside FPPDFFramework.aar.
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
        }

        externalNativeBuild {
            cmake {
                // The SDK .so carries a privately, statically linked libc++
                // (self-contained delivery).  The app links it manually (see
                // the header comment) and uses the normal c++_shared STL; the
                // two libc++ copies never interact because the public API only
                // crosses the boundary in C types and PODs.
                arguments += listOf("-DFPPDF_SDK_DIR=${sdkUnpackDir.get().asFile.absolutePath}")
                cppFlags += "-std=c++14"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildFeatures {
        compose = true
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

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    packaging {
        jniLibs {
            // This demo drives the C++ API through its own bridge; the SDK's
            // Kotlin-layer JNI bridge (libFPPDFFrameworkKotlin.so) would be
            // merged in from the AAR's jni/ folder but is not used here.
            excludes += "**/libFPPDFFrameworkKotlin.so"
        }
    }
}

// The CMake configure/build steps read from build/fppdf-sdk, so the AAR must
// be unpacked before any native task runs. Matching on the task names keeps
// this independent from AGP's internal task graph.
tasks.matching {
    it.name.contains("ExternalNativeBuild", ignoreCase = true) ||
        it.name.startsWith("configureCMake") ||
        it.name.startsWith("buildCMake")
}.configureEach {
    dependsOn(unpackFppdfSdk)
}

dependencies {
    // The Flyingbee PDF Conversion SDK, bundled as a direct AAR file.
    // AGP merges its self-contained libFPPDFFramework.so (4 ABIs) from the
    // AAR's jni/ folder into the APK; the native link step uses the unpacked
    // copy instead (see unpackFppdfSdk).  This demo drives the C++ API, so it
    // only needs the .so + headers, not the Kotlin classes.jar.
    implementation(files("libs/flyingbee/FPPDFFramework-10.3.6.aar"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
