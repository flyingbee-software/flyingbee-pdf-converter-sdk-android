# FPPDFFramework SDK Android Integration Guide

Welcome to the official integration guide for the **FPPDFFramework SDK for Android**. This guide shows how to convert PDFs to Word, Excel, PowerPoint, HTML, Images and more on Android using the SDK's **Kotlin API** — no NDK, no CMake, and no JNI code of your own.

The SDK ships as a single AAR (`FPPDFFramework-10.3.6.aar`) containing the Kotlin API layer (`com.flyingbee.FPPDFFramework.*`), the self-contained native core (`libFPPDFFramework.so`, 4 ABIs), the runtime resources (`Resources.bundle` in assets), and three public C++ headers for advanced native use. This guide covers both integration paths:

| Path | API | Demo project |
| :--- | :--- | :--- |
| **Kotlin** (recommended) | `com.flyingbee.FPPDFFramework.*` — coroutines, `Flow` progress, options DSL | `FPPDFFramework_Demo_Android` |
| **C++** | `FPPDFDocument` / `FPPDF2AllConverter` / `FPPDFOptions` | `FPPDFFramework_Demo_Android_CPP` |

## 🕹️ Try the Free Online Web Demo

Before writing any integration code, you can exercise the same conversion engine in your browser. Our web demo is always powered by the latest version of the Flyingbee SDK, so you can validate PDF → Word / Excel / PowerPoint quality and OCR accuracy against your own documents first.

[🚀 Launch the Free Web Demo](https://www.flyingbee.com/pdf-converter/?utm_source=github_guide_conversion_sdk_android&utm_medium=referral&utm_campaign=github_guide_conversion_sdk_android)

## Table of Contents

- [Before You Start](#before-you-start)
- [Quick Start & SDK Integration](#quick-start--sdk-integration)
  - [1. Add the AAR](#1-add-the-aar)
  - [2. Initialize the SDK (required)](#2-initialize-the-sdk-required)
  - [3. License verification](#3-license-verification)
- [Comprehensive ConversionOptions Configuration](#comprehensive-conversionoptions-configuration)
- [Supported Output Formats & Directory Management](#supported-output-formats--directory-management)
- [Complete Conversion Workflow](#complete-conversion-workflow)
- [Using the C++ API Directly](#using-the-c-api-directly)
- [Platform Notes](#platform-notes)
- [Troubleshooting & FAQ](#troubleshooting--faq)
- [License Options and Free Trial](#license-options-and-free-trial)
- [Technical Support](#technical-support)

> 📖 Companion documents: the repository **[README.md](README.md)** (demo walkthrough, feature list, repository layout) and **[CHANGELOG.md](CHANGELOG.md)** (release notes).

## Before You Start

| Item | Kotlin path | C++ path |
| :--- | :--- | :--- |
| Android Studio | 2026.1.4+ (**Quail 4** or newer) — AGP 9.4 is only supported from Quail 4 onward | same |
| Minimum Android OS | 6.0 (API 23) — `minSdk = 23` | same |
| ABIs | `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86` | same |
| Android Gradle Plugin | 9.4.0 (requires Gradle 9.6.0+) | 9.4.0 |
| Android NDK | **Not required** | Required — 28.2.13676358 |
| CMake | — | 3.22.1 |
| SDK Build Tools | 36.0.0 or later | 36.0.0 or later |
| Extra dependency | `kotlinx-coroutines-android` | — |
| JDK | 17 minimum; the project pins 25 via `gradle/gradle-daemon-jvm.properties` (auto-provisioned on first sync — needs network) | same |
| Runtime permissions | none (internal-storage I/O) | none |

The SDK never touches the network, camera or location: all processing is on-device.

## Quick Start & SDK Integration

### 1. Add the AAR

Copy `app/libs/flyingbee/FPPDFFramework-10.3.6.aar` from the demo into your project (e.g. `app/libs/`) and declare it as a file dependency. The Kotlin API uses coroutines, so add `kotlinx-coroutines-android` as well:

```kotlin
dependencies {
    implementation(files("libs/FPPDFFramework-10.3.6.aar"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
```

No `settings.gradle.kts` repository changes are needed. AGP merges the Kotlin classes, the per-ABI native libraries, and the `Resources.bundle` assets into your APK automatically.

> The AAR is a commercial binary and is **not committed to git**. Drop it into `app/libs/flyingbee/` before the first build; see `app/libs/flyingbee/README.md` in either demo project.

### 2. Initialize the SDK (required)

```kotlin
import com.flyingbee.FPPDFFramework.FPPDFFramework

// Once per process, before any FPPDFDocument / FPPDFConverter.
// Any thread works; first launch unpacks ~75 MB of resources to internal
// storage, so call it from a background thread on cold start.
// Idempotent and thread-safe.
FPPDFFramework.initialize(applicationContext)
```

`initialize()` unpacks the bundled `Resources.bundle` (PDF CMaps, OOXML templates, Tesseract `tessdata`), points the native SDK at it (the Android equivalent of `FPPDF2AllConverter::SetResourceRootFolder`), and verifies the Kotlin layer and native core come from the same SDK build. Skipping it makes every other API throw. If initialization fails it throws `FPPDFFrameworkException`.

### 3. License verification

The commercial license is bound to your **Android application ID (package name)** and an expiry date, and is compiled into the SDK binary we deliver. Check its state at runtime:

```kotlin
val lic = FPPDFFramework.license      // organization / expiredDate / isExpired
if (lic.isExpired) { /* surface a message to the user */ }
```

If the package name does not match the licensed application ID, conversions fail with `Invalid Lib Key.`

## Comprehensive ConversionOptions Configuration

`ConversionOptions` mirrors the native `FPPDFOptions` tree field-for-field. Construct one directly or with the `conversionOptions { }` DSL; every field has a sensible default, so you only set what you need.

### General & Threading Options

- **`threadMax`**: Integer. Multi-threading control. `0` = auto-detect. Max `20`; on phones/tablets `2–3` is recommended (see memory note in FAQ). Default `2`.
- **`parseAnnotations`**: Parse and retain PDF annotations. Default `true`.
- **`enableOCR`**: Enable Optical Character Recognition for scanned documents. Default `false`.
- **`imageDPI`** / **`imageQuality`**: Default render DPI (default `300`) and lossy-output fidelity (`0.3–1.0`, default `0.92`).
- **`outputToFolder`**: Emit folder-style output where the format supports it.

### Word (DOCX) Options (`word { }`)

- **`mergeParagraphs`**: Merge adjacent paragraphs for cleaner text flow.
- **`trimBlankSpace`**: Trim trailing whitespace. Default `true`.
- **`outlineType`**: `OutlineType.NONE` / `PDF_OUTLINE` (default) / `DETECT`.
- **`shapeToImage`**: Rasterize complex vector shapes for compatibility. Default `true`.
- **`mergeIntersectImages`**: Merge overlapping images to avoid rendering artifacts. Default `true`.

### HTML Options (`html { }`)

- **`layoutMode`**: `HtmlLayoutMode.EXACT_PAGE` (page view) / `TEXT_FLOW`.
- **`navigationBar`**: `HtmlNavigationBar.NONE` / `PDF_VIEWER` (default).
- **`mergeResource`**: `HtmlMergeResource.NONE` / `CSS_JS` / `CSS_JS_SMALL_IMAGES` (default) / `CSS_JS_ALL_IMAGES`.
- **`packageAsZip`**: Package the HTML output into a single ZIP archive.
- **`textFlowParagraph`**: `HtmlTextFlowParagraph.LINE_BREAK` / `FIRST_LINE_INDENT`.

### Excel (XLSX/CSV) Options (`excel { }`)

- **`formatOption`**: `ExcelFormatOption.KEEP_ORIGINAL_FORMATTING` (default) / `RETAIN_DATA_STRUCTURE` preserves table layout / `SPECIAL_VERSION`.
- **`thousandSeparator`**: `ExcelThousandSeparator.AUTO` for automatic number formatting (also `COMMA` / `DOT` / `BLANK` / `APOSTROPHE`).
- **`allInOneSheet`** / **`allInOneSheetAddToRow`**: Combine pages into one sheet; with the row mode, pages append as rows instead of blocks.
- **`recognizeNumber`**: Intelligent number recognition. Default `true`.
- **`overlapText`**: `ExcelOverlapText.AUTO` handles overlapping text gracefully (also `MERGE` / `SPLIT`).
- **`csvPackageZip`**: Package CSV output into a ZIP archive.

### Image Options (`image { }`)

- **`format`**: `ImageFormat.JPEG` / `PNG` (default) / `BMP` / `GIF` / `TIFF` / `TGA` / `JPEG2000`.
- **`dpi`**: Supported values `36`, `72`, `144`, `300`, `600`, `1202`.
- **`quality`**: Float `0.3–1.0`.
- **`antiAlias`**: Font smoothing. Default `true`.
- **`packageAsZip`**: Package image output into a ZIP.

### Element Options (`element { }`)

- **`quality`**, **`packageAsZip`** for XML-element output.

### OCR Options (`ocr { }`)

- **`language`**: String, e.g. `"eng"`, `"chi_sim+eng"`, `"jpn"`.
- **`engineMode`**: `OcrEngineMode.LSTM_ONLY` (default) for modern neural recognition.
- **`resizeDPI`**: Recommended `300` for best OCR accuracy.
- **`minConfidence`**: Float `0–100`. **Set it explicitly** — the demo uses `10`, while the native `FPPDFOCROptions` header documents a default of `50`.
- **`enableImageScan`**: Force image-scan preprocessing for heavily graphical PDFs.

> `pageSegmentationMode` exists in the native `FPPDFOCROptions` struct but is marked *not used* on this platform; there is no Kotlin equivalent.

## Supported Output Formats & Directory Management

### Format Categories

Pass a `ConversionFormat` as `ConversionRequest.format`:

| Category | Formats | Output |
| :--- | :--- | :--- |
| **File-based** | `WORD`, `TEXT`, `POWERPOINT`, `EXCEL` | Single file |
| **Folder-based** | `HTML`, `CSV`, `ELEMENT`, `IMAGE` | Folder |
| **Image codecs** | via `image { format = ImageFormat.* }` | Folder, or ZIP if `packageAsZip` |

> The output file/folder name is derived from the source name and format (`filename.docx` for file-based, `filename_png/` for folder-based). The produced path is returned as `ConversionResult.Success.output` — you never build it yourself.

Two conversions exist **only in the C++ API** and have no `ConversionFormat` counterpart: **Images → PDF** (`convertImages2PDF`) and **Text → Word** (`convertText2Word`). RTF is not available on Android.

### Output Directory Setup

Write to your app's private external files directory (no runtime permission needed):

```kotlin
val outputDir = File(context.getExternalFilesDir(null), "FPPDFOutput")
```

## Complete Conversion Workflow

```kotlin
import com.flyingbee.FPPDFFramework.*
import kotlinx.coroutines.*
import java.io.File

class ConverterViewModel(app: Application) : AndroidViewModel(app) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var converter: FPPDFConverter? = null

    fun startup() = scope.launch {
        withContext(Dispatchers.IO) {
            FPPDFFramework.initialize(getApplication())
            converter = FPPDFConverter()
        }
        // FPPDFFramework.version / .license are now readable for the UI.
    }

    fun convert(pdf: File, outputDir: File) {
        val conv = converter ?: return
        scope.launch {
            // Page-by-page progress (1-based):
            val progressJob = launch {
                conv.progress.collect { p -> _ui.update(PageProgress(p.page, p.totalPages)) }
            }
            val result = withContext(Dispatchers.IO) {
                conv.convert(
                    ConversionRequest(
                        pdfFile = pdf,
                        password = null,
                        pages = PageRange.All,
                        outputDir = outputDir,
                        format = ConversionFormat.WORD,
                        options = conversionOptions {
                            threadMax = 2
                            enableOCR = true
                            word { outlineType = OutlineType.PDF_OUTLINE }
                            ocr { language = "eng+chi_sim" }
                        },
                    )
                )
            }
            progressJob.cancel()
            when (result) {
                is ConversionResult.Success -> _ui.done(result.output, result.elapsedSeconds)
                is ConversionResult.Failure -> _ui.error(result.message)
                ConversionResult.Cancelled  -> _ui.cancelled()
            }
        }
    }

    fun cancel() { converter?.cancel() }

    override fun onCleared() {
        converter?.close()
        scope.cancel()
    }
}
```

The converter serialises requests on one instance (internal `Mutex`), so `convert()` calls never race, and cancelling then immediately restarting is safe: the in-flight call returns `ConversionResult.Cancelled` before the next one starts.

For Java callers (no coroutines), use the callback form:

```java
converter.convertAsync(request, new ConvertCallback() {
    @Override public void onProgress(ProgressUpdate u) { /* page-by-page */ }
    @Override public void onComplete(ConversionResult r) { /* Success / Failure / Cancelled */ }
});
```

### Canceling a Conversion

```kotlin
converter.cancel()
```

Cancellation is cooperative: the worker threads finish the current page (which can take a moment with OCR) and then stop; the in-flight `convert()` returns `ConversionResult.Cancelled`.

### Inspecting a PDF

```kotlin
val doc = FPPDFDocument.open(pdfFile, ownerPassword = null, userPassword = password)
try {
    if (doc.isOpen) {
        val pages = doc.pageCount
        val encrypted = doc.isEncrypted
    }
} finally {
    doc.close()
}
```

## Using the C++ API Directly

If you need the raw C++ API (`FPPDFDocument`, `FPPDF2AllConverter`, `FPPDFOptions`) from native code, use the sibling **`FPPDFFramework_Demo_Android_CPP`** project as your reference.

### Linking

AGP's Prefab integration rejects a *shared* library that statically links its own libc++ ("Library is a shared library with a statically linked STL..."). This is a Prefab policy, not an SDK limitation — unpack the AAR at build time and import the `.so` + headers manually in CMake:

```cmake
add_library(FPPDFFramework::FPPDFFramework SHARED IMPORTED)
set_target_properties(FPPDFFramework::FPPDFFramework PROPERTIES
    IMPORTED_LOCATION "${FPPDF_SDK_DIR}/jni/${CMAKE_ANDROID_ARCH_ABI}/libFPPDFFramework.so"
    INTERFACE_INCLUDE_DIRECTORIES "${FPPDF_SDK_DIR}/prefab/modules/FPPDFFramework/include")

add_library(your_jni SHARED your_jni.cpp)
target_link_libraries(your_jni PRIVATE FPPDFFramework::FPPDFFramework android log)
```

Exclude the unused Kotlin bridge from your APK if you go native-only: `packaging { jniLibs { excludes += "**/libFPPDFFrameworkKotlin.so" } }`.

### API surface

```cpp
#include "FPPDFFramework.h"

// Point the SDK at the unpacked Resources.bundle once before the first
// conversion (Android has no executable folder to default to).
FPPDF2AllConverter::SetResourceRootFolder(resourceRoot);

// Inspect a document
FPPDFDocument doc(path, /*ownerPassword=*/nullptr, /*userPassword=*/nullptr);
if (doc.isOpenSuccess()) { int pages = doc.pageCount(); }

// Convert PDF → Office / image / text / element
FPPDF2AllConverter converter;                 // or pass a FPPDF2AllConverterDelegate*
FPPDFOptions options;                         // see FPPDFOptions.h
int pageIndexes[] = { 0, 2, 4 };              // zero-based; pass nullptr / 0 for all pages
converter.convertPDFItem(inputPath, password, pageIndexes, 3,
                         "docx", destPath, &options, /*isInBackground=*/true);

// Images → PDF (C++ API only)
const char* images[] = { img1, img2 };
converter.convertImages2PDF(images, 2, outPdfPath, /*isPaperSizeAuto=*/true, 0, 0, 0,
                            FPPDFOptions_PageOrientationPortrait, FPPDFOptions_ScaleMethodFit,
                            /*isCropWidth=*/false, /*isCropHeight=*/false,
                            "Title", nullptr, nullptr, nullptr, nullptr, false);

// Text → Word (C++ API only)
converter.convertText2Word(inputTextPath, outDocxPath, /*isPaperSizeAuto=*/true, 0, 0, 0,
                           /*isOrientationLandscape=*/false, "Helvetica", 12.0f,
                           /*columnCount=*/1, false);

converter.cancelConversion();
```

### Key points

- Call `FPPDF2AllConverter::SetResourceRootFolder(<folder containing Resources.bundle>)` once before the first converter — the CPP demo unpacks the AAR's assets itself (`SdkResources.kt`) to do this.
- The public API only crosses the JNI boundary in C types and PODs, so your app's STL choice (`c++_shared` / `c++_static` / none) never conflicts with the SDK's private libc++.
- If your own JNI bridge attaches an SDK worker thread to the JVM to invoke Kotlin/Java code, it must `DetachCurrentThread` before the thread exits — on Android below 11 (API 30) ART aborts the process otherwise. `FPPDFFramework_jni.cpp` shows the RAII guard.

## Platform Notes

- **Resources.** `Resources.bundle` lives in the AAR's assets and `FPPDFFramework.initialize()` copies it to internal storage on first launch (a few seconds for ~75 MB). Don't ship a second copy in your own assets.
- **Memory.** Peak ~290 MB PSS during page rendering on a 4-page OCR conversion, ~150 MB PSS steady state (arm64). Keep `threadMax` at 1–2 on low-RAM devices.
- **APK size.** ~17 MB per ABI plus ~75 MB of resources. Use App Bundles / ABI splits.
- **Threading.** The Kotlin `convert()` is a `suspend` function; the SDK runs the work on its own pthreads and reports progress through `Flow`s. No threading setup is required on your side.
- **Offline.** No network access, no runtime permissions, no device features required.
- **Debug logging.** The SDK's optional file logging is anchored to the executable folder and is unavailable in the Android app form.

## Troubleshooting & FAQ

**Q: Which demo project should I start from?**

A: `FPPDFFramework_Demo_Android` — it drives the Kotlin API, needs no NDK/CMake/JNI in your app, and exercises every output format plus the full options tree. Use `FPPDFFramework_Demo_Android_CPP` only if you intend to call the C++ API from your own native code.

**Q: What is the recommended thread count?**

A: On phones/tablets, `threadMax` of `2–3` balances speed against memory and thermals. `0` auto-detects. Peak memory during a 4-page OCR conversion is ~290 MB PSS, steady state ~150 MB PSS.

**Q: Why is my OCR conversion slow?**

A: OCR speed depends on `resizeDPI` and `engineMode`. Use `ocr { resizeDPI = 300; engineMode = OcrEngineMode.LSTM_ONLY }` for the best accuracy-to-speed ratio. Confirm your license is active, and leave OCR off for PDFs that already have a text layer.

**Q: Conversion fails with "Invalid Lib Key."**

A: The running app's package name (applicationId) is not the one bound in your license, or the license has expired. Commercial licenses are bound to a specific applicationId — send your exact applicationId to sales.

**Q: The app throws on `FPPDFFramework.initialize()`.**

A: Usually a missing or mismatched native library: `initialize()` checks that `libFPPDFFrameworkKotlin.so` and `libFPPDFFramework.so` in the APK come from the same SDK build (ABI check). Make sure you did not mix AAR versions, and that ABI splits did not drop one of the libraries.

**Q: Do I need to ship or unpack `Resources.bundle` myself?**

A: No. It lives inside the AAR's assets, and `FPPDFFramework.initialize()` copies it to internal storage on first launch (a few seconds for ~75 MB). Adding a second copy to your own assets just doubles the download.

**Q: Can I convert specific pages instead of the whole document?**

A: Yes. `PageRange` is 1-based — `PageRange.Pages(listOf(1, 3, 5))` converts pages 1, 3 and 5; `PageRange.First(n)` takes the first n pages.

**Q: Can I use the SDK from plain Java?**

A: Yes — `convertAsync(request, ConvertCallback)` and the `FPPDFDocument.open(...)` accessors are Java-friendly. The `conversionOptions { }` DSL and `suspend convert()` are Kotlin-only conveniences.

**Q: Which STL should my app use?**

A: With the Kotlin API your app has no native code, so the question doesn't apply. With the C++ API: any. The SDK statically links its own libc++, so your app can use `c++_shared`, `c++_static`, or none without conflict.

**Q: Why can't I consume the SDK through Prefab?**

A: Because `libFPPDFFramework.so` is a shared library with a privately, statically linked libc++, which AGP's Prefab rejects for any consumer. Unpack the AAR at build time and link the `.so` + headers directly in CMake — see `FPPDFFramework_Demo_Android_CPP/app/build.gradle.kts` (`unpackFppdfSdk`) and `src/main/cpp/CMakeLists.txt`.

**Q: Does minification (R8/ProGuard) break the SDK?**

A: No extra rules are needed. The AAR ships consumer ProGuard rules (`proguard.txt`) that keep the Kotlin API and its JNI bridge. Just don't strip the native libraries.

**Q: Can I convert images to PDF, or text to Word?**

A: Only through the C++ API (`convertImages2PDF` / `convertText2Word`). The Kotlin layer has no `ConversionFormat` for either yet.

## License Options and Free Trial

### Get a Free Trial License

Ready to evaluate the SDK? [Contact our sales team](https://www.flyingbee.com/contact-us?utm_source=github_guide_conversion_sdk_android&utm_medium=referral&utm_campaign=github_guide_conversion_sdk_android) to receive a 30-day free trial license for the Flyingbee PDF Conversion SDK for Android.

### Get a Commercial License

Flyingbee PDF Conversion SDK is a commercial product requiring a valid license for application release. Redistribution of the SDK binary (`FPPDFFramework.aar` / `libFPPDFFramework.so`) or its public headers to third parties is strictly prohibited.

To obtain a commercial license, please [contact our sales team](https://www.flyingbee.com/contact-us?utm_source=github_guide_conversion_sdk_android&utm_medium=referral&utm_campaign=github_guide_conversion_sdk_android). **Note:** For the Android Conversion SDK, commercial licenses must be bound to your specific Android application IDs (package names). The demo applications in this repository are open source under the [Apache License 2.0](LICENSE); the AAR is not.

## Technical Support

If you encounter technical questions, integration issues, or bugs, please submit a detailed problem report to [Flyingbee Support](mailto:support@flyingbee.com). To help us resolve your issue quickly, please include:

- The Flyingbee PDF Conversion SDK product name and version (shown on the demo Home screen, or `FPPDFFramework.version`).
- Your Android OS version, device model, and Android Studio version.
- A detailed description of the problem, the source PDF, the output format, and the options used.
- Relevant error messages or `adb logcat` output (filter on `FPPDFFrameworkKotlin`).

**Helpful Links:**

- **Home:** [https://www.flyingbee.com](https://www.flyingbee.com/?utm_source=github_guide_conversion_sdk_android&utm_medium=referral&utm_campaign=github_guide_conversion_sdk_android)
- **Support Center:** [https://www.flyingbee.com/support](https://www.flyingbee.com/support?utm_source=github_guide_conversion_sdk_android&utm_medium=referral&utm_campaign=github_guide_conversion_sdk_android)
- **Email:** [support@flyingbee.com](mailto:support@flyingbee.com)

> Copyright 2026 Flyingbee Software, Inc. All rights reserved.
