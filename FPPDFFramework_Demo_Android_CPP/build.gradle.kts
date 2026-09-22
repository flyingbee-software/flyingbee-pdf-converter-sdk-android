// Top-level build file. Plugins are applied by the modules that use them.
// Note: since AGP 9.0 Kotlin support is built into the Android Gradle plugin;
// the standalone org.jetbrains.kotlin.android plugin must NOT be applied.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
