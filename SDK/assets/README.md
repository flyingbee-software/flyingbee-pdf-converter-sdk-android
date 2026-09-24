# Shared SDK Runtime Resources

This folder holds the single copy of `Resources.bundle/` (PDF CMaps, OOXML
templates, Tesseract `tessdata`, fonts.conf) and the `samples/` PDF library
used by **all three demo projects** (`Kotlin/`, `Java/`, `CPP/`). Each demo's
`app/build.gradle.kts` adds it to the main source set:

```kotlin
sourceSets {
    getByName("main") {
        assets.directories.add("../../SDK/assets")
    }
}
```

AGP merges the bundle into the APK's `assets/Resources.bundle/` at build
time, exactly as if it lived under the demo's own `src/main/assets/` — so
`FPPDFFramework.initialize()` (Kotlin/Java demos) and the CPP demo's
`SdkResources.kt` unpack it unchanged on first launch. The same merge puts
every `samples/*.pdf` in the APK's `assets/samples/`, which is what the demos
enumerate through `AssetManager.list("samples")`.

Anything that stays demo-specific (drawables, layouts, strings) belongs in
each project's own `app/src/main/` folder.
