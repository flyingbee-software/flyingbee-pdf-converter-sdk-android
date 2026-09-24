# Changelog

All notable changes to the **Flyingbee PDF Conversion SDK for Android** are documented here.
The SDK version matches `FPPDFFramework_Version` in `FPPDFFramework.h`.

## Unreleased — Demo repository restructuring

- **Demo folders renamed.** `FPPDFFramework_Demo_Android` → `Kotlin/` and `FPPDFFramework_Demo_Android_CPP` → `CPP/`, so the three demos sit side by side as `Kotlin/`, `Java/` and `CPP/`. The Gradle `rootProject.name` values are now `FPPDFFrameworkDemoAndroidKotlin` / `FPPDFFrameworkDemoAndroidJava` / `FPPDFFrameworkDemoAndroidCpp`.
- **Java demo added.** `Java/` is the pure-Java counterpart of the Kotlin demo (applicationId `com.flyingbee.FPPDFConverterDemoJava`): identical screens, output formats, per-format option screens and sample-PDF library, built with Java + XML layouts + Material Components and driving the SDK through its Java API (`convertAsync` + `ConvertCallback`) — no coroutines, no native code.
- **Kotlin demo navigation.** Switched from a single-activity `NavHost` to multi-Activity navigation (`DemoApplication.kt` + `ScreenActivities.kt`), aligned with the Java demo's per-screen Activities.
- **Resources.bundle consolidated.** The three identical per-demo copies of `Resources.bundle/` (~74 MB each) were replaced by a single repo-root `Shared/assets/Resources.bundle/`, pulled into every demo APK through `assets.srcDirs` in each `app/build.gradle.kts`. Clone the repo once, build all three demos with no extra copying.
- **SDK AAR consolidated.** The three identical per-demo copies of `FPPDFFramework-10.3.6.aar` (~64 MB each) were replaced by a single repo-root `Shared/libs/flyingbee/FPPDFFramework-10.3.6.aar`, consumed by all three demos through a relative file dependency (`implementation(files("../../Shared/libs/flyingbee/..."))`). The per-demo `app/libs/` folders are gone.
- **CI.** GitHub Actions workflows updated for the new folder names; a Java demo workflow was added.

## 10.3.6.0 (Android) — 2026-09

First Android release of the Flyingbee PDF Conversion SDK, distributed as a self-contained `libFPPDFFramework.so` (inside `FPPDFFramework.aar`) for 4 ABIs: `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`.

### Added
- **Android SDK build.** `FPPDFFramework.aar` packages one self-contained shared library per ABI that statically links its own libc++ and exports only the public C++ API via a linker version script. Works with any host-app STL configuration.
- **Resource root API.** `FPPDF2AllConverter::SetResourceRootFolder(path)` so an Android app can point the SDK at its unpacked `Resources.bundle` (PDF CMaps, OOXML templates, Tesseract `tessdata`). Required on Android because there is no executable folder to default to.
- **Android license binding.** Commercial licenses are bound to the app's package name (applicationId), mirroring the iOS/macOS Bundle-ID binding. New `kSDKLicense_Android_Flyingbee.h` + `FPLicense_Android.cpp`.
- **Kotlin API layer.** The AAR now ships first-class Kotlin classes (`com.flyingbee.FPPDFFramework.*`): `FPPDFFramework.initialize()`, `FPPDFDocument`, `FPPDFConverter` with coroutine `convert()` / `Flow`-based progress, and a `ConversionOptions` DSL mirroring the native option tree. Apps using the Kotlin API need no NDK, CMake or JNI code; `FPPDFFramework.initialize()` also unpacks the bundled `Resources.bundle` automatically. The raw C++ API remains available through the three public headers.
- **Image output formats.** TIFF (LZW/Deflate, 300 DPI metadata), JPEG2000 (OpenJPEG v2), GIF (giflib, median-cut quantizer) and TGA (Aseprite tga, 32 bpp RLE) added to the image converter. JPEG, PNG, BMP also supported.
- **Demo apps.** Full Jetpack Compose demo (`Kotlin/`, originally `FPPDFFramework_Demo_Android`) driving the Kotlin API with per-format option screens, OCR language picker, bundled sample PDFs, and progress + elapsed-time display — zero native code. A minimal sibling (`CPP/`, originally `FPPDFFramework_Demo_Android_CPP`) demonstrates the C++ integration path with its own JNI bridge showing the correct thread-attach/detach pattern. It supports DOCX and PNG conversion, an OCR path (Tesseract eng + LSTM engine, output side-buffered to `<base>_ocr.docx`), per-conversion elapsed-time display, and a clickable result list that opens each produced file through `FileProvider` (Android 7+).

### Fixed
- **Crash on "Stop Conversion".** Rapid cancel-then-restart during an OCR conversion could free the in-flight exporter while worker threads were still using it (use-after-free). The converter now rejects a new request while a previous conversion is still running.
- **`pthread_detach` abort on Android.** Cancelling a conversion called `pthread_detach` on an invalid handle; bionic aborted the process. Removed (the cancel path uses `join`).
- **JPEG color misalignment.** RGBA pixmaps were fed to libjpeg as RGB, shifting colors. Now extracts R/G/B when the pixmap is not 3-channel.
- **Converter re-creation crash.** `GlobalParams` was initialized via `std::call_once`, which crashed when a converter was destroyed and recreated. Switched to a mutex + null-check for reentrant setup.

### Notes
- Minimum supported Android: **6.0 (API 23)**.
- All processing is offline; no runtime permissions required for internal-storage I/O.
