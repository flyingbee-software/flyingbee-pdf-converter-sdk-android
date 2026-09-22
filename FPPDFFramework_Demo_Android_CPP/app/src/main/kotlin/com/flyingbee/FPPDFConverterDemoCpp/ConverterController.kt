package com.flyingbee.FPPDFConverterDemoCpp

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import java.io.File
import java.util.concurrent.Executors

// ---------------------------------------------------------------------------
// ConverterController : minimal controller for the C++ API demo. Unlike the
// pure-Kotlin demo, everything below the UI goes through the app's own JNI
// bridge (FPPDFNative -> cpp/FPPDFFramework_jni.cpp -> libFPPDFFramework.so).
// ---------------------------------------------------------------------------

class ConverterController(app: Application) : AndroidViewModel(app), FPPDFDelegate {

    private val appContext: Context get() = getApplication()

    var statusMessage: String by mutableStateOf("")
        private set
    var isConverting: Boolean by mutableStateOf(false)
        private set
    var sdkVersionText: String by mutableStateOf("")
        private set
    var licenseText: String by mutableStateOf("")
        private set
    var samplePageCount: Int by mutableStateOf(0)
        private set

    // Files produced by the last successful conversion, flattened for the
    // result list. Folder outputs (e.g. PNG, one image per page) enumerate
    // every contained file; single-file outputs (e.g. DOCX) list one item.
    var outputFiles: List<OutputFileItem> by mutableStateOf(emptyList())
        private set

    // One-shot preview request, consumed by MainActivity once the viewer
    // intent has been launched.
    var previewRequest: File? by mutableStateOf(null)
        private set

    val outputDir: File by lazy {
        File(appContext.getExternalFilesDir(null) ?: appContext.filesDir, "FPPDFOutput")
            .apply { mkdirs() }
    }

    /** Human-readable label for the output folder shown in the UI. */
    fun outputDirLabel(): String = outputDir.absolutePath

    /** Asks the UI to open [file] in an external viewer app. */
    fun previewOne(file: File) { previewRequest = file }

    fun consumePreview() { previewRequest = null }

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val bridge = DelegateBridge(this)
    private var nativeHandle: Long = 0L
    private var didRunStartup = false

    // Output file of the running conversion (for the success message).
    @Volatile private var expectedOutput: File? = null

    // Monotonic clock timestamp taken right before convertPdf, read back in
    // didEndConversion to report the conversion time (same methodology as the
    // other demos, so the three demos can be compared fairly).
    @Volatile private var conversionStartMs = 0L

    // === Lifecycle ===========================================================

    fun startup() {
        if (didRunStartup) return
        didRunStartup = true
        executor.execute {
            try {
                // The SDK resolves Resources.bundle (cmaps, OOXML templates,
                // tessdata) relative to a root folder; an Android app has no
                // executable folder, so unpack the bundled resources (merged
                // into the APK from the AAR's assets/ by AGP) and point the
                // SDK at them before the converter is created.
                val resourceRoot = SdkResources.ensureUnpacked(appContext)
                FPPDFNative.setResourceRootFolder(resourceRoot.absolutePath)
                if (nativeHandle == 0L) {
                    nativeHandle = FPPDFNative.createConverter(bridge)
                }
                val version = FPPDFNative.sdkVersion()
                val release = FPPDFNative.releaseDate()
                val org = FPPDFNative.licenseOrganization()
                val expiry = FPPDFNative.licenseExpiredDate()
                val pages = pageCountOf(sampleFile())
                mainHandler.post {
                    sdkVersionText = "$version ($release)"
                    licenseText = "$org / $expiry"
                    samplePageCount = pages
                    statusMessage = "Ready. The bundled sample has $pages pages."
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    statusMessage = "SDK load failed: ${t.message ?: "unknown"}"
                }
            }
        }
    }

    override fun onCleared() {
        if (nativeHandle != 0L) {
            FPPDFNative.destroyConverter(nativeHandle)
            nativeHandle = 0L
        }
        executor.shutdown()
        super.onCleared()
    }

    // === Sample =================================================================

    /** The bundled sample PDF, copied out of assets/ on first launch. */
    private fun sampleFile(): File {
        val dest = File(appContext.filesDir, "samples/FPPDFSample.pdf")
        if (!dest.exists() || dest.length() == 0L) {
            dest.parentFile?.mkdirs()
            appContext.assets.open("samples/FPPDFSample.pdf").use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return dest
    }

    private fun pageCountOf(file: File): Int {
        val h = FPPDFNative.openDocument(file.absolutePath, null, null)
        return try {
            if (FPPDFNative.documentIsOpen(h)) FPPDFNative.documentPageCount(h) else 0
        } finally {
            FPPDFNative.closeDocument(h)
        }
    }

    // === Conversion ===============================================================

    /**
     * [destDocType] is the native strOutputFormat ("docx", "png", ...).
     * [useOcr] runs the conversion through the OCR pipeline (Tesseract) so
     * scanned / image-only pages produce editable text; the OCR result is
     * written to a separate <base>_ocr.<ext> file so it does not clobber the
     * normal-conversion output.
     */
    fun convertSample(destDocType: String, useOcr: Boolean = false) {
        if (isConverting) return
        isConverting = true
        outputFiles = emptyList()
        statusMessage = if (useOcr) "Converting with OCR…" else "Converting…"
        executor.execute {
            val source = sampleFile()
            val pageCount = pageCountOf(source).coerceAtLeast(1)
            val pageIndexes = (1..pageCount).toList().toIntArray()
            val folderTypes = setOf(
                "csv", "htm", "html", "element", "elements", "image", "images",
                "jpg", "jpeg", "png", "bmp", "gif", "tif", "tiff", "tga", "jp2",
            )
            val base = source.nameWithoutExtension
            val namedBase = if (useOcr) "${base}_ocr" else base
            val filename = if (destDocType in folderTypes) "${namedBase}_$destDocType"
            else "$namedBase.$destDocType"
            val destFile = File(outputDir, filename)
            if (destFile.exists()) {
                if (destFile.isDirectory) destFile.deleteRecursively() else destFile.delete()
            }

            val optionsHandle = FPPDFNative.newOptions()
            try {
                FPPDFNative.setOptionsGeneral(optionsHandle, 1, 0, 300, 0.92f, useOcr)
                FPPDFNative.setOptionsImage(optionsHandle, 1, 300, 0.92f, false, true)
                if (useOcr) {
                    // Same OCR defaults as the native OptionsBag / Kotlin demo:
                    // English tessdata, LSTM-only engine, 300 DPI render,
                    // 10% minimum confidence, no whole-image scan.
                    FPPDFNative.setOptionsOcr(
                        optionsHandle,
                        "eng",
                        1,
                        300,
                        10.0f,
                        false,
                    )
                }
                expectedOutput = destFile
                conversionStartMs = SystemClock.elapsedRealtime()
                val started = FPPDFNative.convertPdf(
                    nativeHandle,
                    source.absolutePath,
                    "",
                    pageIndexes,
                    destDocType,
                    destFile.absolutePath,
                    optionsHandle,
                    true,
                )
                if (!started) {
                    mainHandler.post {
                        isConverting = false
                        statusMessage = "SDK rejected the start request."
                    }
                }
                // Completion arrives via didEndConversion on the SDK worker
                // thread; the FPPDFDelegate callbacks finalise the UI state.
            } finally {
                FPPDFNative.freeOptions(optionsHandle)
            }
        }
    }

    fun cancelConversion() {
        if (nativeHandle != 0L) FPPDFNative.cancelConversion(nativeHandle)
    }

    /** Flattens the conversion result into the rows shown in the result list:
     *  a single file for file outputs, every contained file (sorted by name)
     *  for folder outputs. */
    private fun collectOutput(dest: File): List<OutputFileItem> {
        val files = if (dest.isFile) sequenceOf(dest)
        else dest.walkTopDown().asSequence().filter { it.isFile }
        return files.sortedBy { it.name }
            .map { OutputFileItem(it, fileSizeText(it.length())) }
            .toList()
    }

    private fun fileSizeText(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> "%.1f MB".format(mb)
            kb >= 1.0 -> "%.1f KB".format(kb)
            else -> "$bytes B"
        }
    }

    // === FPPDFDelegate (called on SDK worker threads) ==============================

    override fun didStartConversion(result: Boolean, errorInfo: String?) {
        if (!result) {
            mainHandler.post {
                isConverting = false
                statusMessage = "SDK rejected the conversion: ${errorInfo ?: "unknown"}"
            }
        }
    }

    override fun didEndConversion(result: Boolean, errorInfo: String?) {
        mainHandler.post {
            isConverting = false
            if (result) {
                val dest = expectedOutput
                val elapsedSec = (SystemClock.elapsedRealtime() - conversionStartMs) / 1000.0
                if (dest != null && dest.exists()) {
                    outputFiles = collectOutput(dest)
                    statusMessage = "Success: ${dest.name} (elapsed %.1f s)".format(elapsedSec)
                } else {
                    outputFiles = emptyList()
                    statusMessage = "Finished, but no output found."
                }
            } else {
                outputFiles = emptyList()
                statusMessage = "Failed: ${errorInfo ?: "unknown"}"
            }
        }
    }

    override fun didEndPageIndex(toPageIndex: Int, toTotalPages: Int, result: Boolean, errorInfo: String?) {
        mainHandler.post {
            if (isConverting) statusMessage = "Converting… $toPageIndex / $toTotalPages"
        }
    }

    override fun willSaveDoc() {
        mainHandler.post { statusMessage = "Saving…" }
    }

    override fun catchException() {
        mainHandler.post {
            isConverting = false
            statusMessage = "Exception caught inside the SDK."
        }
    }
}

/** One row in the conversion result list: the produced file plus its
 *  human-readable size for display. */
data class OutputFileItem(val file: File, val sizeText: String)
