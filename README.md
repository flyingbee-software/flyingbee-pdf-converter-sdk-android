# PDF to Word, Excel & PowerPoint Conversion SDK for Android | Flyingbee PDF Conversion SDK

Flyingbee PDF Conversion SDK for Android is a high-performance, developer-friendly library that converts PDF to Word, Excel, PowerPoint, HTML, CSV, plain text, images, and structured XML elements on Android devices — preserving original layouts, text, and images. Whether you need a robust Android PDF conversion SDK or a comprehensive mobile PDF library to convert PDF to editable Word documents in your Android app, this SDK delivers enterprise-grade accuracy and formatting fidelity.

This repository ships **three ready-to-run demo projects** that cover all integration paths of the same SDK:

| Project | Integration surface | Native code in the app |
| :--- | :--- | :--- |
| [`Kotlin`](Kotlin/) | **Kotlin API** (`com.flyingbee.FPPDFFramework.*`) — coroutines, `Flow` progress, options DSL | **None** — no NDK, no CMake, no JNI of your own |
| [`Java`](Java/) | **Java API** (`com.flyingbee.FPPDFFramework.Java.*`) — `convertAsync` + `ConvertCallback`, XML layouts | **None** — no NDK, no CMake, no JNI of your own |
| [`CPP`](CPP/) | **C++ API** (`FPPDFDocument` / `FPPDF2AllConverter` / `FPPDFOptions`) via prefab headers | Yes — the demo's own JNI bridge + CMake link |

All three link the same `FPPDFFramework-10.3.6.aar`. Start with the Kotlin demo unless you specifically need a pure-Java or native integration.

## 🕹️ Try the Free Online Web Demo

Experience the full power of our PDF conversion SDK before integrating it into your project. Our web demo is always powered by the latest version of the Flyingbee SDK, allowing you to test PDF to MS Office (.docx, .xlsx, .pptx) conversion and OCR capabilities instantly.

[🚀 Launch the Free Web Demo](https://www.flyingbee.com/pdf-converter/?utm_source=github_readme_conversion_sdk_android&utm_medium=referral&utm_campaign=github_readme_conversion_sdk_android)

## Key Features

- **PDF to Word (.docx)** — high layout fidelity, outline generation, shape handling, paragraph merging options.
- **PDF to Excel (.xlsx) / CSV** — table extraction with formatting, thousand-separator and overlap-text strategies.
- **PDF to PowerPoint (.pptx)** — presentation reconstruction.
- **PDF to HTML** — page-view or text-flow layout, resource merging, navigation bar, ZIP packaging.
- **PDF to Image** — PNG, JPEG, BMP, GIF, TIFF, TGA, JPEG2000 with configurable DPI and quality.
- **PDF to Element (XML)** — page content as structured XML with embedded images.
- **PDF to Plain Text (.txt)**.
- **Built-in OCR (Tesseract)** — convert scanned PDFs into searchable, editable text entirely on-device.
- **Images to PDF** — merge images into one PDF with custom page size, margins and orientation *(C++ API only)*.
- **Text to Word** — plain text to formatted Word with configurable font, size and columns *(C++ API only)*.
- **Progress Tracking** — page-by-page progress callbacks with a live progress bar.
- **Background Conversion** — asynchronous conversion that keeps the UI responsive; cancellable at any time.
- **Password Protection** — open and convert encrypted PDFs (the demo prompts for the password).
- **Page Selection** — convert all pages, the first N pages, or a custom page range.
- **Sample Library** — seven bundled sample PDFs covering every conversion path (Kotlin / Java demos).
- **Per-format option screens** — the full `ConversionOptions` tree is editable in the UI and persisted across launches (Kotlin / Java demos).

> **Note:** Images → PDF and Text → Word are exposed by the C++ API (`convertImages2PDF` / `convertText2Word`). They are not yet surfaced by the Kotlin layer.

## Table of Contents

- [Three Demo Projects — Which One Should You Open?](#three-demo-projects--which-one-should-you-open)
- [Requirements](#requirements)
- [System Requirements](#system-requirements)
- [How to Run the Demos](#how-to-run-the-demos)
  - [Kotlin Demo (Kotlin)](#kotlin-demo-kotlin)
  - [Java Demo (Java)](#java-demo-java)
  - [C++ Demo (CPP)](#c-demo-cpp)
- [Repository Layout](#repository-layout)
- [Inside the SDK Package (FPPDFFramework.aar)](#inside-the-sdk-package-fppdfframeworkaar)
- [Integrating the SDK into Your Own App](#integrating-the-sdk-into-your-own-app)
- [API Reference](#api-reference)
- [Platform Notes and Limitations](#platform-notes-and-limitations)
- [Frequently Asked Questions (FAQ)](#frequently-asked-questions-faq)
- [License Options and Free Trial](#license-options-and-free-trial)
- [Technical Support](#technical-support)

> 📖 For the full options reference, a complete conversion workflow, and an FAQ, see the **[SDK Integration Guide](FPPDFFramework%20Android%20SDK%20Integration%20Guide.md)**. Release notes are in **[CHANGELOG.md](CHANGELOG.md)**.

## Three Demo Projects — Which One Should You Open?

| | `Kotlin` | `Java` | `CPP` |
| :--- | :--- | :--- | :--- |
| **API used** | Kotlin layer shipped in the AAR (`com.flyingbee.FPPDFFramework.*`) | Java layer shipped in the AAR (`com.flyingbee.FPPDFFramework.Java.*`) | Public C++ API (`FPPDFFramework.h`, `FPPDFOptions.h`, `FPPDFOptionsTitles.h`) |
| **Package** | `com.flyingbee.FPPDFConverterDemo` | `com.flyingbee.FPPDFConverterDemoJava` | `com.flyingbee.FPPDFConverterDemoCpp` |
| **UI toolkit** | Jetpack Compose (Material 3) | XML layouts + Material Components | Jetpack Compose (Material 3) |
| **Your own native code** | None | None | `app/src/main/cpp/FPPDFFramework_jni.cpp` + `CMakeLists.txt` |
| **NDK / CMake required** | No | No | Yes (NDK 28.2.13676358, CMake 3.22.1) |
| **Formats exercised** | Word, PowerPoint, Excel, CSV, HTML, Image, Element, Text | Word, PowerPoint, Excel, CSV, HTML, Image, Element, Text | DOCX and PNG (+ an OCR path that side-buffers to `<base>_ocr.docx`) |
| **Options UI** | Full per-format settings screens, persisted | Full per-format settings screens, persisted | Minimal fixed options |
| **Sample PDFs** | 7 bundled samples | 7 bundled samples | 1 bundled sample |
| **Best for** | Evaluating the SDK, and 99% of production Kotlin apps | 99% of production Java apps (no coroutines needed) | Apps that already run native code and want the raw C++ entry points |

All three projects are independent Gradle builds — each has its own `settings.gradle.kts`, Gradle wrapper and `app/libs/flyingbee/` drop-in folder for the AAR. The Kotlin and Java demos share the same screens, formats and option trees; pick whichever language your app uses.

## Requirements

| Item | Kotlin demo | Java demo | C++ demo |
| :--- | :--- | :--- | :--- |
| Android OS (device/emulator) | 6.0 (API 23) or later | 6.0 (API 23) or later | 6.0 (API 23) or later |
| Supported ABIs | `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86` | same | same |
| Android Studio | 2026.1.4+ (**Quail 4** or newer) | same | same |
| Android Gradle Plugin | 9.4.0 | 9.4.0 | 9.4.0 |
| Gradle | 9.7.1 (wrapper included) | 9.7.1 (wrapper included) | 9.7.1 (wrapper included) |
| Android NDK | **Not required** | **Not required** | **Required** — 28.2.13676358 |
| CMake | — | — | 3.22.1 |
| SDK Build Tools | 36.0.0 or later | 36.0.0 or later | 36.0.0 or later |
| compileSdk / targetSdk | 37 | 37 | 37 |
| JDK | 17 or later (the project pins 25 in `gradle/gradle-daemon-jvm.properties`; Gradle auto-downloads it on first sync, so the first build needs network access) | same | same |
| Language / UI | Kotlin + Jetpack Compose (Material 3) | Java + XML layouts (Material Components) | Kotlin + Jetpack Compose (Material 3) |

## System Requirements

This section describes what the SDK requires at **runtime on the end user's device or emulator**.

| Item | Requirement |
| :--- | :--- |
| Minimum Android OS | **6.0 (API 23)** — `minSdk = 23` |
| Tested Android OS | 6.0 (API 23) through 16 (API 36/37), phones, tablets and emulators |
| CPU architectures | `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86` (one `libFPPDFFramework.so` per ABI, plus a small per-ABI Kotlin-layer bridge `libFPPDFFrameworkKotlin.so`) |
| Native runtime | The AAR ships a Kotlin API layer (`com.flyingbee.FPPDFFramework.*`) on top of the C++ core. The core carries a private, statically linked libc++, so your app's own STL choice (`c++_shared` / `c++_static` / none) is irrelevant unless you write native code |
| Storage (app data) | ~75 MB for the unpacked `Resources.bundle` (PDF CMaps, OOXML templates, Tesseract `tessdata`) — **you ship this yourself** in `app/src/main/assets/Resources.bundle/`; `FPPDFFramework.initialize()` unpacks it to internal storage on first launch — plus the ~17 MB per-ABI native library inside the APK |
| Memory | Conversion is multi-threaded. Measured on an arm64 emulator converting a 4-page sample with OCR at the default settings: peak ~290 MB PSS during page rendering/OCR start, ~150 MB PSS steady state. Keep `threadMax` low (2–3) on low-RAM devices |
| Network | None. All processing is fully offline |
| Runtime permissions | None required for internal-storage I/O (the SDK reads the input PDF and writes output under the app's own private directories). No camera, network or external-storage permission is used |
| Device features | None (no telephony, GPS or camera requirement). OCR quality depends on render DPI, not on hardware |

Notes:

- **Why API 23.** The build toolchain (NDK r28) supports down to API 21, and no third-party component in the SDK strictly requires API 22+; API 23 was chosen as the floor because it is the first version with runtime permissions and marks the practical baseline of current Play-store-targeting apps. If you genuinely need Android 5.0–5.1 (API 21–22) coverage, contact support — the SDK can be rebuilt with `minSdk = 21` after a symbol-level verification pass.
- **64-bit requirement.** Google Play requires 64-bit (`arm64-v8a`) for all apps; the AAR ships it, so nothing extra is needed on your side.
- **Writing your own JNI bridge (optional).** If you drive the C++ API natively instead of the Kotlin layer, worker threads that call into the JVM must detach before exiting (see *Platform Notes and Limitations*); the `CPP` project's `FPPDFFramework_jni.cpp` shows the correct RAII pattern.

## How to Run the Demos

> ⚠️ **Windows users — short path required.** Before you begin, **unzip or clone the project into a short, ASCII-only path without spaces** (e.g. `E:\FPPDFConverter\` or `D:\Projects\FPPDF\`). Windows has a 260-character `MAX_PATH` limit, and Android Gradle builds (especially when NDK / CMake / `.cxx` native chains kick in) generate deeply nested intermediate directories that easily exceed it. Chinese characters, spaces and special characters in the path also break `make` / `clang` argument parsing. If your path is long or non-ASCII you will see cryptic "file not found" or "path too long" errors during Gradle sync or compile — moving the project to a short, clean path fixes 80% of them. This matters most for the **C++ demo**, which builds a CMake/NDK chain.

All three projects need one thing dropped in before the first build:

```
Kotlin/app/libs/flyingbee/FPPDFFramework-10.3.6.aar
Java/app/libs/flyingbee/FPPDFFramework-10.3.6.aar
CPP/app/libs/flyingbee/FPPDFFramework-10.3.6.aar
```

The runtime resources are **already in the repository**: a single `Resources.bundle/` lives at the repo root in [`shared-assets/`](shared-assets/), and all three demos pull it into their APK via `assets.srcDirs(...)` — no per-demo copy is needed.

See `app/libs/flyingbee/README.md` in each project.

### Kotlin Demo (Kotlin)

1. Open the folder in **Android Studio** (`File -> Open...`). The IDE will prompt to create `local.properties`; make sure `sdk.dir` points to your Android SDK (Android Studio writes this automatically).
2. Wait for Gradle sync to finish (the first sync downloads Compose/AndroidX dependencies; the SDK itself is bundled locally, no credentials needed).
3. Select a device or emulator (any ABI works) and press **Run**.

Or from the command line:

```bash
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The debug APK contains all four ABIs (~86 MB). For your own releases, use an ABI split or per-ABI App Bundles to reduce size.

#### Using the Kotlin demo

- **Input** — pick a PDF from the bundled samples or from any document provider (Files, Drive, email attachments...). You can also open a PDF directly from another app via *Share -> FPPDFFrameworkDemo*. Encrypted PDFs prompt for the password.
- **Output** — choose the target format (Word / PowerPoint / Excel / CSV / HTML / Image / Element / Text), the destination folder, and the page range.
- **Settings** — per-format option screens mirror the full `ConversionOptions` structure; values persist across app launches.
- **Result** — conversion progress is shown page-by-page; when finished you can preview, share, or reveal the output files.

### Java Demo (Java)

The Java demo is the pure-Java counterpart of the Kotlin demo: identical screens, the same output formats, the same per-format option screens and the same sample-PDF library — built with Java + XML layouts + Material Components, driving the SDK through its Java API (`convertAsync(request, callback)`), so no coroutines are needed.

1. Open the folder in **Android Studio** (`File -> Open...`) and wait for Gradle sync to finish.
2. Select a device or emulator (any ABI works) and press **Run**.

Or from the command line:

```bash
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### C++ Demo (CPP)

1. Install the **NDK (28.2.13676358)** and **CMake (3.22.1)** via *Android Studio → Settings → Android SDK → SDK Tools*.
2. Open the folder in Android Studio and let Gradle sync finish.
3. Press **Run**. The build unpacks the AAR into `app/build/fppdf-sdk` (Gradle task `unpackFppdfSdk`), then CMake links `libFPPDFFramework.so` + the three public headers into the app's own `libFPPDFFramework_jni.so` bridge.

```bash
./gradlew :app:assembleDebug
```

Why the native link is manual instead of Prefab: `libFPPDFFramework.so` is a *shared* library with a privately, statically linked libc++ (self-contained delivery, no `libc++_shared.so` to ship). AGP's Prefab integration categorically rejects that combination for any consumer, so the demo unpacks the AAR at build time and links the imported `.so` + headers directly. The public API only crosses the boundary in C types and PODs, so two independent libc++ copies can never interact.

All three demos share the single `Resources.bundle` in the repo-root `shared-assets/` folder (wired in through each demo's `assets.srcDirs`) and unpack it on launch (Kotlin / Java demos via `FPPDFFramework.initialize()`, CPP demo via its own `SdkResources.kt`). The C++ demo additionally shows the correct JNI thread attach/detach pattern (`FPPDFFramework_jni.cpp`) and how to link the `.so` + headers manually instead of using Prefab.

> The C++ demo packages only `libFPPDFFramework.so` — it excludes the SDK's Kotlin bridge `libFPPDFFrameworkKotlin.so`, since it talks to the C++ API directly.

## Repository Layout

```
flyingbee-pdf-converter-sdk-android/
├── README.md                              this file
├── CHANGELOG.md                           release notes
├── FPPDFFramework Android SDK Integration Guide.md
│                                          full options reference + workflows
├── LICENSE                                Apache 2.0 (demo source only)
├── shared-assets/                         single copy of the SDK runtime
│   └── Resources.bundle/                  resources (CMaps, OOXML templates,
│                                          tessdata, fonts.conf) — pulled into
│                                          all three APKs via assets.srcDirs
├── Kotlin/                                Kotlin API demo — no native code
│   ├── app/
│   │   ├── libs/flyingbee/                drop FPPDFFramework-10.3.6.aar here
│   │   ├── src/main/kotlin/com/flyingbee/FPPDFConverterDemo/
│   │   │   ├── DemoApplication.kt         process-wide SDK init + controller
│   │   │   ├── ConverterController.kt     state, options persistence,
│   │   │   │                              conversion via the Kotlin SDK API
│   │   │   ├── Models.kt                  enums / value tables shared with the UI
│   │   │   ├── MainActivity.kt            Compose host, file picking, share/open intents
│   │   │   ├── ScreenActivities.kt        per-screen Activities (multi-Activity nav)
│   │   │   └── ui/                        Home, Settings (+5 sub-screens),
│   │   │                                  sample & OCR pickers
│   │   └── src/main/assets/samples/       seven demo PDFs
│   └── build.gradle.kts / settings.gradle.kts / gradle/
├── Java/                                  Java API demo — no native code
│   ├── app/
│   │   ├── libs/flyingbee/                drop FPPDFFramework-10.3.6.aar here
│   │   ├── src/main/java/com/flyingbee/FPPDFConverterDemoJava/
│   │   │   ├── DemoApplication.java       process-wide SDK init + controller
│   │   │   ├── ConverterController.java   state, options persistence, conversion
│   │   │   │                              via convertAsync + ConvertCallback
│   │   │   ├── Models.java                enums / value tables shared with the UI
│   │   │   ├── MainActivity.java          Home screen
│   │   │   └── ui/                        BaseScreenActivity, sample & OCR
│   │   │                                  pickers, settings/ (6 Activities)
│   │   ├── src/main/res/layout/           XML layouts for every screen
│   │   └── src/main/assets/samples/       seven demo PDFs
│   └── build.gradle.kts / settings.gradle.kts / gradle/
└── CPP/                                   C++ API demo — own JNI bridge
    ├── app/
    │   ├── libs/flyingbee/                drop FPPDFFramework-10.3.6.aar here
    │   ├── src/main/cpp/
    │   │   ├── CMakeLists.txt             links libFPPDFFramework.so + public headers
    │   │   └── FPPDFFramework_jni.cpp     JNI bridge (attach/detach RAII guard)
    │   ├── src/main/kotlin/com/flyingbee/FPPDFConverterDemoCpp/
    │   │   ├── FPPDFNative.kt             1:1 Kotlin view of the JNI exports
    │   │   ├── SdkResources.kt            manual Resources.bundle unpack +
    │   │   │                              SetResourceRootFolder
    │   │   ├── ConverterController.kt     conversion flow via the C++ API
    │   │   └── MainActivity.kt            Compose host
    │   └── src/main/assets/samples/       one demo PDF
    └── build.gradle.kts / settings.gradle.kts / gradle/
```

There is **no `src/main/cpp/` and no NDK configuration** in `Kotlin/` or `Java/`: the SDK's Kotlin/Java layers (shipped inside the AAR) are their only integration surface. The C++ API integration path lives in `CPP/`.

## Inside the SDK Package (FPPDFFramework.aar)

```
FPPDFFramework.aar
├── classes.jar                            the Kotlin API layer:
│     com.flyingbee.FPPDFFramework.*       FPPDFFramework / FPPDFDocument /
│                                          FPPDFConverter / ConversionOptions / ...
├── jni/<abi>/libFPPDFFramework.so         self-contained native core, one per ABI
│                                          (arm64-v8a / armeabi-v7a / x86_64 / x86);
│                                          statically links its own libc++, strips all
│                                          private symbols, and exports only the public
│                                          C++ API via a linker version script
├── jni/<abi>/libFPPDFFrameworkKotlin.so   the JNI bridge the Kotlin layer uses
│                                          (small; excluded from your APK if you use
│                                          only the C++ API, as the CPP demo shows)
├── prefab/modules/FPPDFFramework/include/ the 3 public C++ headers (for native use):
│     FPPDFFramework.h                     converter + document API, SetResourceRootFolder
│     FPPDFOptions.h                       all option structs (word/excel/image/ocr/...)
│     FPPDFOptionsTitles.h                 human-readable option titles (for settings UIs)
└── proguard.txt                           consumer rules for the Kotlin API
```

The AAR is consumed as a plain file dependency (see step 1 below). AGP merges the Kotlin classes and the native libraries (`jni/<abi>/*.so`) into your APK automatically. **Resources.bundle is not inside the AAR** — you place it in `app/src/main/assets/Resources.bundle/` and AGP merges that alongside.

## Integrating the SDK into Your Own App

The Kotlin demo consumes the SDK exactly as a customer app should. Four pieces are involved:

### 1. Depend on the AAR

Copy `app/libs/flyingbee/FPPDFFramework-10.3.6.aar` into your own app (e.g. `app/libs/`), then declare it as a file dependency:

```kotlin
dependencies {
    implementation(files("libs/FPPDFFramework-10.3.6.aar"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
```

No `settings.gradle.kts` repository changes are needed — this is a plain file dependency. AGP automatically merges the Kotlin classes and `jni/<abi>/*.so` from the AAR into your APK.

### 2. Ship Resources.bundle

Copy the **entire `Resources.bundle/` folder** from the SDK drop (or from this repository's [`shared-assets/Resources.bundle/`](shared-assets/)) into your app at `app/src/main/assets/Resources.bundle/`. This is required because the AAR no longer carries the runtime resources.

### 3. Initialize the SDK once

```kotlin
// Call once (any thread; it does file I/O on first launch, so prefer a
// background thread).  Unpacks Resources.bundle from your app's assets into
// app-private storage and points the native SDK at it.  Idempotent and
// thread-safe.  Throws FPPDFFrameworkException if Resources.bundle is missing.
FPPDFFramework.initialize(applicationContext)
```

Before this call, `FPPDFDocument` / `FPPDFConverter` throw. `initialize()` also verifies that the Kotlin layer and the native core come from the same SDK build (ABI check) and throws `FPPDFFrameworkException` otherwise.

### 4. Convert

```kotlin
val converter = FPPDFConverter()

val result = converter.convert(
    ConversionRequest(
        pdfFile = File(sourcePath),
        password = null,                    // or the user/owner password
        pages = PageRange.All,              // or PageRange.First(n) / PageRange.Pages(listOf(1,3,5))
        outputDir = File(getExternalFilesDir(null), "FPPDFOutput"),
        format = ConversionFormat.WORD,
        options = conversionOptions {
            threadMax = 2
            enableOCR = true
            word { outlineType = OutlineType.PDF_OUTLINE }
            ocr { language = "eng+chi_sim" }
        },
    )
)

when (result) {
    is ConversionResult.Success -> show(result.output)      // produced file/folder
    is ConversionResult.Failure -> showError(result.message)
    ConversionResult.Cancelled  -> showCancelled()
}
```

`convert()` is a `suspend` function that serialises requests on one converter instance (a `Mutex`), so the UI stays responsive and cancel/restart races are impossible. Progress and the "saving document" event are exposed as `Flow`s:

```kotlin
launch { converter.progress.collect { ui.update(it.page, it.totalPages) } }
val job = launch { /* ... converter.convert(request) ... */ }

// Cancel cooperatively; the in-flight convert() completes with Cancelled:
converter.cancel()
```

For Java (non-coroutine) callers, `convertAsync(request, callback)` delivers the same events through a `ConvertCallback`.

> **Resources and the resource root.** On Windows/macOS/Linux the SDK resolves `Resources.bundle` relative to the executable; on Android you must ship it in `app/src/main/assets/Resources.bundle/` and `initialize()` unpacks it to internal storage. If you drive the C++ API natively (CPP demo), you do the unpack + `FPPDF2AllConverter::SetResourceRootFolder(...)` sequence yourself — the demo's `SdkResources.kt` shows the code.

## API Reference

The public Kotlin API lives in the package `com.flyingbee.FPPDFFramework`.

### FPPDFFramework (process entry point)

```kotlin
object FPPDFFramework {
    fun initialize(context: Context)        // once, before any other API
    val isInitialized: Boolean
    val version: String                     // e.g. "10.3.6"
    val releaseDate: String
    val license: LicenseInfo                // organization / expiredDate / isExpired
}
```

### FPPDFDocument (inspection)

```kotlin
val doc = FPPDFDocument.open(file, ownerPassword = null, userPassword = null)
doc.isOpen; doc.isEncrypted; doc.hasPassword; doc.pageCount
doc.close()
```

### FPPDFConverter (conversion)

```kotlin
class FPPDFConverter : Closeable {
    val progress: Flow<ProgressUpdate>      // per-page (1-based)
    val willSaveDoc: Flow<Unit>             // once per conversion, at save phase
    suspend fun convert(request: ConversionRequest): ConversionResult
    fun convertAsync(request: ConversionRequest, callback: ConvertCallback): Job
    fun cancel(): Boolean
    override fun close()
}
```

`ConversionRequest` carries the source `pdfFile`, optional `password`, `pages` (`PageRange.All` / `First(n)` / `Pages(1-based list)`), `outputDir`, `format` (`ConversionFormat.WORD/POWERPOINT/EXCEL/CSV/TEXT/HTML/IMAGE/ELEMENT`) and `options`. The output file/folder name is derived from the source name and format; the produced path is returned as `ConversionResult.Success.output`.

### ConversionOptions (behavior)

General: `parseAnnotations`, `threadMax` (2–3 recommended on phones), `imageDPI`, `imageQuality`, `enableOCR`, `outputToFolder`. Sub-option groups: `word` (outline/shape/paragraph handling), `html` (layout/merge/navbar/zip), `excel` (formatting/separator/overlap/zip), `image` (format/DPI/quality/zip/anti-alias), `element`, `ocr` (language/engineMode/resizeDPI/minConfidence). Every field mirrors the native `FPPDFOptions` tree; the demo's Settings screens set each one.

### C++ API

The same core is also exposed as a C++ API (`FPPDFDocument`, `FPPDF2AllConverter`, `FPPDFOptions`) through the three public headers under `prefab/modules/FPPDFFramework/include/`. Use it only if you need to call the SDK from native code; the `CPP` project is the complete integration example (manual CMake link + JNI bridge).

```cpp
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

## Platform Notes and Limitations

- **Resources.** The SDK's runtime resources (`Resources.bundle`: PDF CMaps, OOXML templates, Tesseract `tessdata`) are **not** inside the AAR. You ship them in `app/src/main/assets/Resources.bundle/` and `FPPDFFramework.initialize()` copies them to internal storage on first launch (a few seconds for ~75 MB). Omitting this folder causes an `FPPDFFrameworkException` at `initialize()`.
- **Coroutines.** The Kotlin API's `convert()` is a `suspend` function and `progress`/`willSaveDoc` are `Flow`s, so your app needs `kotlinx-coroutines-android`. Java callers without coroutines can use `convertAsync(request, callback)`.
- **JNI worker threads (only if you write native code).** The SDK runs conversions on its own pthreads and reports progress by calling its delegate from those threads. If you write your own JNI bridge to the C++ API and attach such a thread to the JVM (`AttachCurrentThread`) to invoke Kotlin/Java code, it must call `DetachCurrentThread` again before the thread exits — on Android below 11 (API 30) ART aborts the whole process otherwise. The safest pattern, used by the CPP demo's `FPPDFFramework_jni.cpp`, is a small RAII guard that attaches for the duration of each callback and detaches on scope exit. The bundled Kotlin layer already handles this for you.
- **OCR on Android.** With the resources wired up by `initialize()`, OCR works out of the box: the bundled `tessdata` is found through the resource root. Enable "OCR recognition" on the Home screen and pick languages in Settings.
- **PDF → Image / Element output.** Image (PNG/JPEG/BMP/GIF/TIFF/TGA) and XML-element conversions are supported and verified on Android. Word, PowerPoint, Excel, CSV, HTML and TXT conversions are fully functional.
- **Images → PDF / Text → Word.** Available only through the C++ API; the Kotlin layer does not expose them yet.
- **Debug logging.** The SDK's optional file logging is anchored to the executable folder and is not available in the Android app form; the demo therefore has no "Debug log" section (the iOS demo does).
- **Trial license watermarking.** With the bundled evaluation license, output documents may carry trial restrictions; the Home screen shows the licensed organization and expiry date reported by the SDK (`FPPDFFramework.license`).
- **APK size.** Each ABI carries one ~17 MB `libFPPDFFramework.so` (plus a small Kotlin-bridge `.so`); your own `Resources.bundle` in assets adds ~75 MB. Use ABI splits / App Bundles and ship only the resource sub-folders you need in production.
- **Permissions.** The demos need no runtime permissions: they use the Storage Access Framework (document picker) and a `FileProvider` for sharing.

## Frequently Asked Questions (FAQ)

### This repository contains three demo projects — which one should I start with?

`Kotlin`. It uses the Kotlin API shipped inside the AAR, needs no NDK/CMake/JNI in your app, and exercises every output format plus the full options tree. If your app is pure Java, open `Java` instead — same screens and formats, built with XML layouts and `convertAsync` callbacks, no coroutines required. Open `CPP` only if you intend to call the C++ API from your own native code — it exists to show the manual CMake link, the resource-root setup and the JNI attach/detach pattern.

### Can I convert scanned PDFs to editable Word documents on Android?

Yes. The SDK includes a built-in OCR module (Tesseract) that recognizes text in scanned documents and converts them into fully editable Word (.docx) files while preserving the original layout — entirely on-device, with no network or cloud services required.

### What Android versions are supported?

Android 6.0 (API 23) and later, on `arm64-v8a`, `armeabi-v7a`, `x86_64` and `x86` devices and emulators. See [System Requirements](#system-requirements) for the full list.

### Which output formats are supported on Android?

Word (.docx), PowerPoint (.pptx), Excel (.xlsx), CSV, HTML, plain text, XML elements, and images (JPEG, PNG, BMP, TIFF, JPEG2000, GIF, TGA). Images → PDF and Text → Word are available through the C++ API only. (RTF is not available on Android.)

### Does the SDK require any permissions?

No runtime permissions are needed when you read input PDFs and write output files inside your app's own storage (as the demo does). The SDK never accesses the network, camera or location.

### How does the license work on Android?

A commercial license is bound to your app's **applicationId (package name)** and an expiry date, and is compiled into the SDK build we deliver to you. The running app's package name is verified at conversion time; if it does not match, conversions are rejected with `Invalid Lib Key.` You can check the license state at runtime via `FPPDFFramework.license` (`organization`, `expiredDate`, `isExpired`).

### Is there a free trial?

Yes — a 30-day evaluation license is available. [Contact our sales team](https://www.flyingbee.com/contact-us?utm_source=github_readme_conversion_sdk_android&utm_medium=referral&utm_campaign=github_readme_conversion_sdk_android) with your applicationId and use case.

### Can I convert only specific pages?

Yes. Pass `PageRange.Pages(listOf(1, 3, 5))` to `ConversionRequest.pages` — page numbers are **1-based**. Use `PageRange.All` for the whole document or `PageRange.First(n)` for the first n pages.

### Can conversions run in the background while the UI stays responsive?

Yes. `FPPDFConverter.convert()` is a `suspend` function that runs the conversion on the SDK's own worker threads and suspends until it completes; collect `converter.progress` for page-by-page updates. No threading setup is required.

### Can I cancel a conversion in progress?

Yes — call `converter.cancel()`. Cancellation is cooperative: the worker currently processing a page (which may take a moment with OCR enabled) finishes that page, then the conversion stops and the in-flight `convert()` returns `ConversionResult.Cancelled`. The converter serialises requests internally, so you can start the next conversion as soon as the previous `convert()` returns.

### How much memory does a conversion use?

On a typical 4-page document with OCR enabled, peak memory is around 290 MB PSS during page rendering, settling to ~150 MB PSS steady state (measured on arm64). On low-RAM devices, keep `threadMax` at 1–2.

### What is the recommended thread count?

`threadMax = 2–3` on phones and tablets. `0` lets the SDK auto-detect from the CPU core count. Higher values rarely help on mobile and increase memory pressure.

### How do I speed up OCR?

Use `ocr { resizeDPI = 300; engineMode = OcrEngineMode.LSTM_ONLY }`, and enable OCR only for the documents that need it — text-layer PDFs convert much faster with OCR off.

### Which C++ STL should my app use?

Any (`c++_shared`, `c++_static`, or none) — and with the Kotlin API you don't choose one at all, because your app contains no native code. The SDK statically links a private copy of libc++ inside `libFPPDFFramework.so`, so there is no STL conflict with your app.

### Why can't I consume the SDK through Prefab?

Because `libFPPDFFramework.so` is a shared library with a privately, statically linked libc++, which AGP's Prefab rejects for any consumer. The C++ demo works around it by unpacking the AAR at build time and linking the `.so` + headers directly in CMake — see `CPP/app/build.gradle.kts` and `src/main/cpp/CMakeLists.txt`.

### Can I use the SDK from Java/Kotlin directly?

Yes. The AAR ships a first-class Kotlin API (`com.flyingbee.FPPDFFramework.*`) with coroutines and `Flow`s — see [Integrating the SDK into Your Own App](#integrating-the-sdk-into-your-own-app). From Java, use the Java API layer (`com.flyingbee.FPPDFFramework.Java.*`) with `convertAsync(request, callback)` — the `Java` demo is the complete example. If you need the raw C++ API instead, the `CPP` project is a complete, Apache-licensed example.

### How large is the SDK in my APK?

~17 MB per ABI (stripped `libFPPDFFramework.so` + a small Kotlin-bridge `.so`) from the AAR, plus the ~75 MB `Resources.bundle` you ship in your app's assets (unpacked to internal storage on first launch). Use Android App Bundles / ABI splits so each device only downloads one ABI.

### Does the demo source code have the same license as the SDK?

No. All three demo projects are open source under the [Apache License 2.0](LICENSE); the `FPPDFFramework.aar` binary is a commercial product. See [License](#license).

## License Options and Free Trial

### Get a Free Trial License

Ready to evaluate the SDK? [Contact our sales team](https://www.flyingbee.com/contact-us?utm_source=github_readme_conversion_sdk_android&utm_medium=referral&utm_campaign=github_readme_conversion_sdk_android) to receive a 30-day free trial license for the Flyingbee PDF Conversion SDK for Android.

### Get a Commercial License

Flyingbee PDF Conversion SDK is a commercial product requiring a valid license for application release. To obtain a commercial license, please [contact our sales team](https://www.flyingbee.com/contact-us?utm_source=github_readme_conversion_sdk_android&utm_medium=referral&utm_campaign=github_readme_conversion_sdk_android). 

**Note:** For the Android Conversion SDK, commercial licenses must be bound to your specific Android application IDs (package names).

## Technical Support

Thank you for choosing Flyingbee PDF Conversion SDK. If you encounter technical questions, integration issues, or bugs, please submit a detailed problem report to [Flyingbee Support](mailto:support@flyingbee.com). To help us resolve your issue quickly, please include:

- The Flyingbee PDF Conversion SDK product name and version (shown on the demo Home screen).
- Your Android OS version, device model, and Android Studio version.
- A detailed description of the problem, the source PDF, the output format, and the options used.
- Relevant error messages or `adb logcat` output (filter on `FPPDFFrameworkKotlin`).

**Helpful Links:**

- **Home:** [https://www.flyingbee.com](https://www.flyingbee.com/?utm_source=github_readme_conversion_sdk_android&utm_medium=referral&utm_campaign=github_readme_conversion_sdk_android)
- **Support Center:** [https://www.flyingbee.com/support](https://www.flyingbee.com/support?utm_source=github_readme_conversion_sdk_android&utm_medium=referral&utm_campaign=github_readme_conversion_sdk_android)
- **Email:** [support@flyingbee.com](mailto:support@flyingbee.com)

## License

This package contains two components under different licenses:

- **The demo application source code** in this repository (`Kotlin/`, `Java/` and `CPP/` — the Compose / XML-layout apps and the Gradle project files) is open source and licensed under the [Apache License 2.0](LICENSE). You are free to copy, modify and reuse it in your own projects.
- **The `FPPDFFramework.aar` binary** (the Kotlin API classes, the self-contained `libFPPDFFramework.so` and its public headers) is a **commercial product**. It is *not* covered by the Apache license above and requires a valid Flyingbee license to ship in a released app. See [License Options and Free Trial](#license-options-and-free-trial).

## Acknowledgements

This SDK uses the following open-source projects. We gratefully acknowledge their authors and contributors.

- **Tesseract OCR** — Apache License 2.0 — Copyright © 2006–2026 Google Inc. and contributors.
- **Leptonica** — BSD 2-Clause License — Copyright © 2001–2026 Dan Bloomberg.
- **FreeType** — FreeType Project License (FTL).
- **zlib** — zlib License — Copyright © 1995–2026 Jean-loup Gailly and Mark Adler.
- **AGG (Anti-Grain Geometry)** — MPL 1.1 / BSD-2-Clause — Copyright © 2002–2006 Maxim Shemanarev.
- **libpng** — zlib License — Copyright © 1998–2026 Glenn Randers-Pehrson and contributors.
- **libjpeg-turbo** — BSD License + IJG License — Copyright © 2009–2026 D. R. Commander and contributors.
- **stb_image** — Public Domain / MIT License — By Sean Barrett and contributors.
- **mujs** — ISC License — Copyright © 2013–2026 Artifex Software, Inc.
- **expat** — MIT License — Copyright © 1998–2000 Thai Open Source Software Center Ltd and Clark Cooper.
- **b64** — MIT License — Copyright © 2004–2008 René Nyffenegger and contributors.
- **OpenJPEG** — BSD 2-Clause License — Copyright © 2002–2026 UCL, Belgium and contributors.
- **fontconfig** — MIT License — Copyright © 2001, 2003 Keith Packard.
- **uchardet** — MPL 1.1 (used under MPL 1.1) — Copyright © 2009–2026 Mozilla Foundation and contributors.
- **utf8rewind** — MIT License — Copyright © 2015–2026 Quinten Lansu.
- **PDFium** — BSD 3-Clause / Apache License 2.0 — Copyright © 2014 The PDFium Authors.

> Copyright 2026 Flyingbee Software, Inc. All rights reserved.
