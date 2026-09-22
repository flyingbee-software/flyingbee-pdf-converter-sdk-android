package com.flyingbee.FPPDFConverterDemoCpp

import android.content.Context
import java.io.File

// ---------------------------------------------------------------------------
// SdkResources : ships the SDK's Resources.bundle (cmaps, OOXML templates,
// OCR tessdata) as app assets and unpacks it into filesDir on first launch.
//
// On Windows/macOS the SDK finds Resources.bundle next to the DLL / inside the
// app bundle; an Android app has no executable folder to hang relative paths
// off, so the bundle must be materialised in app-private storage and handed to
// the SDK through FPPDF2AllConverter::SetResourceRootFolder (see
// FPPDFFramework_jni.cpp).
// ---------------------------------------------------------------------------

object SdkResources {

    private const val ASSET_ROOT = "Resources.bundle"
    private const val STAMP = ".fpdf-resources-v1"

    /**
     * Copies assets/Resources.bundle into <filesDir>/Resources.bundle once and
     * returns filesDir (the folder that must be passed to
     * FPPDFNative.setResourceRootFolder - the SDK appends "Resources.bundle/..."
     * to it itself).  Safe to call from the main thread; it only does work on
     * the very first launch after install or after the asset layout changes.
     */
    fun ensureUnpacked(context: Context): File {
        val root = context.filesDir
        val bundle = File(root, ASSET_ROOT)
        val stamp = File(root, STAMP)
        val version = "1" // bump when the bundled Resources.bundle changes

        if (bundle.isDirectory && stamp.exists() && stamp.readText() == version) {
            return root
        }

        bundle.deleteRecursively()
        unpackAsset(context.assets, ASSET_ROOT, bundle)
        stamp.writeText(version)
        return root
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
