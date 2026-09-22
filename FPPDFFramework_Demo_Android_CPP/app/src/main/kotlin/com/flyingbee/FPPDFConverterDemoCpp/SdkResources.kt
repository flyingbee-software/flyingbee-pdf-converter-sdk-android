package com.flyingbee.FPPDFConverterDemoCpp

import android.content.Context
import java.io.File

// ---------------------------------------------------------------------------
// SdkResources : the SDK's Resources.bundle (cmaps, OOXML templates,
// OCR tessdata) is NOT baked into the AAR anymore.  The host app must place
// it in src/main/assets/Resources.bundle/ — AGP merges that into the APK and
// we read it from there via AssetManager, then unpack it into filesDir
// because the native SDK needs a real folder (no executable-relative path).
// ---------------------------------------------------------------------------

object SdkResources {

    private const val ASSET_ROOT = "Resources.bundle"
    private const val STAMP = ".fpdf-resources-v1"

    /**
     * Copies assets/Resources.bundle into <filesDir>/Resources.bundle once and
     * returns filesDir (the folder that must be passed to
     * FPPDFNative.setResourceRootFolder — the SDK appends "Resources.bundle/..."
     * to it itself).  Safe to call from the main thread; it only does work on
     * the very first launch after install or after the asset layout changes.
     *
     * Throws [IllegalStateException] if Resources.bundle is missing from assets
     * or empty — the host app must ship it in src/main/assets/Resources.bundle/.
     */
    fun ensureUnpacked(context: Context): File {
        val root = context.filesDir
        val bundle = File(root, ASSET_ROOT)
        val stamp = File(root, STAMP)
        val version = "1" // bump when the bundled Resources.bundle changes

        if (bundle.isDirectory && stamp.exists() && stamp.readText() == version && bundleHasFiles(bundle)) {
            return root
        }

        bundle.deleteRecursively()
        unpackAsset(context.assets, ASSET_ROOT, bundle)

        // Validate: if the bundle ended up empty, the host app forgot to ship it.
        if (!bundle.isDirectory || !bundleHasFiles(bundle)) {
            bundle.deleteRecursively()
            throw IllegalStateException(
                "Resources.bundle not found in assets or empty. " +
                    "Resources.bundle is no longer bundled inside the AAR — " +
                    "copy it from FPPDFFramework/Resources.bundle/ into your app's " +
                    "src/main/assets/Resources.bundle/."
            )
        }
        stamp.writeText(version)
        return root
    }

    /** True if the bundle directory exists and contains at least one file. */
    private fun bundleHasFiles(bundleDir: File): Boolean {
        if (!bundleDir.isDirectory) return false
        return bundleDir.walkTopDown().any { it.isFile }
    }

    private fun unpackAsset(assets: android.content.res.AssetManager, path: String, dest: File) {
        val children = assets.list(path) ?: return
        dest.mkdirs()
        for (child in children) {
            val childPath = "$path/$child"
            val sub = assets.list(childPath)
            if (sub != null && sub.isNotEmpty()) {
                unpackAsset(assets, childPath, File(dest, child))
            } else {
                File(dest, child).outputStream().use { out ->
                    assets.open(childPath).use { it.copyTo(out) }
                }
            }
        }
    }
}
