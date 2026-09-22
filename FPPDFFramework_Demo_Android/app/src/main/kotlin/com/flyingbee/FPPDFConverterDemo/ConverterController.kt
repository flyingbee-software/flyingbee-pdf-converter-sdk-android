package com.flyingbee.FPPDFConverterDemo

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.flyingbee.FPPDFFramework.ConversionFormat
import com.flyingbee.FPPDFFramework.ConversionOptions
import com.flyingbee.FPPDFFramework.ConversionRequest
import com.flyingbee.FPPDFFramework.ConversionResult
import com.flyingbee.FPPDFFramework.FPPDFConverter
import com.flyingbee.FPPDFFramework.FPPDFDocument
import com.flyingbee.FPPDFFramework.FPPDFFramework
import com.flyingbee.FPPDFFramework.ImageFormat as SdkImageFormat
import com.flyingbee.FPPDFFramework.OutlineType
import com.flyingbee.FPPDFFramework.PageRange
import com.flyingbee.FPPDFConverterDemo.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

// ---------------------------------------------------------------------------
// ConverterController : the single source of truth for the demo (Android
// counterpart of ConverterController.m in the iOS demo). Holds the selected
// input, all 38 persisted settings (same keys as the iOS UserDefaults schema),
// drives the SDK's Kotlin API and publishes Compose-observable state.
//
// This demo is a pure Kotlin consumer: it talks to the SDK exclusively through
// the com.flyingbee.FPPDFFramework public API (FPPDFFramework / FPPDFDocument /
// FPPDFConverter). No JNI, no native code, no NDK - see app/build.gradle.kts.
//
// It is an AndroidViewModel so the default factory can construct it with the
// Application context - no custom ViewModelProvider.Factory needed.
// ---------------------------------------------------------------------------

class ConverterController(app: Application) : AndroidViewModel(app) {

    private val appContext: Context get() = getApplication()

    /** Context used for string lookups. The Application context is NOT
     *  reconfigured by AppCompatDelegate.setApplicationLocales on every API
     *  level, so MainActivity attaches its own (locale-aware) configuration
     *  here; only a detached configuration context is kept - never the
     *  Activity itself - to avoid leaking it through the ViewModel. */
    private var uiContext: Context = app

    fun attachUiContext(activity: Context) {
        uiContext = activity.applicationContext
            .createConfigurationContext(activity.resources.configuration)
    }

    /** Localisation helper - see module header about statusMessage.
     *  Records the resource so the message can be re-localised after a
     *  in-app language switch (the ViewModel survives the Activity
     *  recreation, so a raw String would stay in the previous language). */
    private var lastStatusResId: Int? = null
    private var lastStatusArgs: Array<out Any> = emptyArray()

    private fun localize(resId: Int, vararg args: Any): String {
        lastStatusResId = resId
        lastStatusArgs = args
        return uiContext.getString(resId, *args)
    }

    /** Application versionName, shown in the About footer (iOS AppInfo). */
    val appVersionName: String
        get() = try {
            val pi = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            pi.versionName ?: "1.0"
        } catch (t: Throwable) {
            "1.0"
        }

    // --- Paths (iOS: Documents/FPPDFInputs / FPPDFOutput) --------------------
    private val inputsDir: File by lazy {
        File(appContext.filesDir, "FPPDFInputs").apply { mkdirs() }
    }
    val outputDir: File by lazy {
        File(appContext.getExternalFilesDir(null) ?: appContext.filesDir, "FPPDFOutput")
            .apply { mkdirs() }
    }

    // --- Shared value tables ---------------------------------------------------
    val dpiValues = ValueTables.dpiValues
    val qualityValues = ValueTables.qualityValues
    val wordDpiValues = ValueTables.wordDpiValues
    val ocrDpiValues = ValueTables.ocrDpiValues
    val ocrLanguages = ValueTables.ocrLanguages

    /** Localised quality labels. Recomputed on every access: the ViewModel
     *  survives the Activity recreation triggered by a locale change, so a
     *  `by lazy` cache would keep showing the previous language. */
    val qualityLabels: List<String>
        get() = ValueTables.qualityLabelRes.map { uiContext.getString(it) }

    // --- Source / status -----------------------------------------------------------
    var selectedFile: File? by mutableStateOf(null)
        private set
    var sourceName: String by mutableStateOf("")
        private set
    var pdfPassword: String by mutableStateOf("")
    var isConverting: Boolean by mutableStateOf(false)
        private set
    var progressValue: Float by mutableStateOf(0f)
        private set
    var statusMessage: String by mutableStateOf(localize(R.string.status_ready))
        private set

    var licenseOrganization: String by mutableStateOf("")
        private set
    var licenseExpiredDate: String by mutableStateOf("")
        private set
    var licenseExpired: Boolean by mutableStateOf(false)
        private set
    var sdkVersionText: String by mutableStateOf("")
        private set

    var lastOutputFile: File? by mutableStateOf(null)
        private set
    var lastOutputSizeText: String by mutableStateOf("")
        private set
    var outputFiles: List<OutputFileItem> by mutableStateOf(emptyList())
        private set

    var samples: List<BundledSample> by mutableStateOf(emptyList())
        private set

    // --- Format ----------------------------------------------------------------------
    var outputFormat: OutputFormat by mutableStateOf(OutputFormat.Docx)
    var imageFormat: ImageFormat by mutableStateOf(ImageFormat.PNG)

    // --- General settings ----------------------------------------------------------------
    var openAfterConversion: Boolean by mutableStateOf(true)
    var pageRangeMode: PageRangeMode by mutableStateOf(PageRangeMode.All)
    var customPageRange: String by mutableStateOf("1")
    var threadMode: ThreadMode by mutableStateOf(ThreadMode.Auto)
    var customThreadCount: String by mutableStateOf("3")
    var imageDPI: Int by mutableStateOf(144)
    var imageQualityIndex: Int by mutableStateOf(3)

    // --- Word ------------------------------------------------------------------------------
    var wordTrimBlankSpace: Boolean by mutableStateOf(true)
    var wordMergeParagraphs: Boolean by mutableStateOf(false)
    var wordShapeToImage: Boolean by mutableStateOf(true)
    var wordMergeIntersectImages: Boolean by mutableStateOf(true)
    var wordImageDPI: Int by mutableStateOf(144)
    var wordOutlineType: Int by mutableStateOf(1)

    // --- Excel ------------------------------------------------------------------------------
    var excelAllInOneSheet: Boolean by mutableStateOf(false)
    var excelRecognizeNumber: Boolean by mutableStateOf(true)
    var excelAllInOneStyle: Int by mutableStateOf(0)
    var excelFormatOption: Int by mutableStateOf(0)
    var excelThousandSeparator: Int by mutableStateOf(0)
    var excelOverlapText: Int by mutableStateOf(0)
    var csvPackageZip: Boolean by mutableStateOf(false)

    // --- HTML -------------------------------------------------------------------------------
    var htmlLayoutMode: Int by mutableStateOf(0)
    var htmlMergeResource: Int by mutableStateOf(2)
    var htmlNavigationBar: Int by mutableStateOf(1)
    var htmlTextFlowParagraph: Int by mutableStateOf(0)
    var htmlPackageZip: Boolean by mutableStateOf(false)

    // --- Image output --------------------------------------------------------------------------
    var imageOutputDPI: Int by mutableStateOf(144)
    var imageOutputQualityIndex: Int by mutableStateOf(3)
    var imagePackageZip: Boolean by mutableStateOf(false)
    var imageAntiAlias: Boolean by mutableStateOf(true)

    // --- Element output ---------------------------------------------------------------------------
    var elementQualityIndex: Int by mutableStateOf(3)
    var elementPackageZip: Boolean by mutableStateOf(false)

    // --- OCR ----------------------------------------------------------------------------------------
    var ocrEnabled: Boolean by mutableStateOf(false)
    var ocrImageScan: Boolean by mutableStateOf(false)
    var ocrDPI: Int by mutableStateOf(300)
    var ocrLanguageText: String by mutableStateOf("eng")
    var ocrSelectedLanguages: Set<String> by mutableStateOf(emptySet())

    // --- One-shot UI requests (delegate callbacks on iOS) --------------------------------------------
    var passwordPromptFile: File? by mutableStateOf(null)
    var previewRequest: File? by mutableStateOf(null)
    var shareRequest: File? by mutableStateOf(null)

    // --- SDK state ------------------------------------------------------------------------------------
    private val prefs = appContext.getSharedPreferences("fpdf_settings", Context.MODE_PRIVATE)
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var converter: FPPDFConverter? = null
    private var conversionJob: Job? = null
    private var didRunStartup = false

    init {
        loadSettings()
        loadLicenseInfo()
    }

    // === Lifecycle ===============================================================================

    fun startup() {
        // The ViewModel survives the Activity recreation triggered by an
        // in-app language switch; re-render the status line from its recorded
        // resource so it follows the new locale.
        lastStatusResId?.let { statusMessage = localize(it, *lastStatusArgs) }
        if (didRunStartup) return
        didRunStartup = true
        // FPPDFFramework.initialize unpacks the Resources.bundle that ships in
        // the AAR's assets/ (cmaps, OOXML templates, tessdata) into app-private
        // storage and points the native SDK at it. It does file I/O on first
        // launch, so run it off the main thread; everything downstream (license
        // info, sample page counts, conversions) waits for it to finish.
        executor.execute {
            try {
                FPPDFFramework.initialize(appContext)
                if (converter == null) converter = FPPDFConverter()
            } catch (t: Throwable) {
                mainHandler.post {
                    statusMessage = localize(R.string.status_sdk_load_failed, t.message ?: "unknown")
                }
                return@execute
            }
            mainHandler.post {
                loadSampleLibrary()
                loadLicenseInfo()
                if (sourceName.isEmpty()) {
                    statusMessage = localize(R.string.status_ready_pick)
                }
            }
        }
    }

    override fun onCleared() {
        // A running conversion holds the native exporter; close() joins the SDK
        // worker threads, so cancel first and let the job settle before the
        // scope is torn down.
        converter?.cancel()
        conversionJob?.cancel()
        converter?.close()
        converter = null
        scope.cancel()
        executor.shutdown()
        super.onCleared()
    }

    private fun loadLicenseInfo() {
        try {
            val license = FPPDFFramework.license
            licenseOrganization = license.organization
            licenseExpiredDate = license.expiredDate
            licenseExpired = license.isExpired
            sdkVersionText = "${FPPDFFramework.version} (${FPPDFFramework.releaseDate})"
        } catch (t: Throwable) {
            // Native library failed to load - surface it instead of crashing.
            statusMessage = localize(R.string.status_sdk_load_failed, t.message ?: "unknown")
        }
    }

    // === PDF helpers ================================================================================

    fun pageCountOf(file: File): Int {
        if (!FPPDFFramework.isInitialized) return 0
        val doc = FPPDFDocument.open(file, pdfPassword.ifEmpty { null }, pdfPassword.ifEmpty { null })
        return try {
            if (doc.isOpen) doc.pageCount else 0
        } finally {
            doc.close()
        }
    }

    fun pdfRequiresPassword(file: File): Boolean {
        if (!FPPDFFramework.isInitialized) return false
        val doc = FPPDFDocument.open(file)
        return try {
            doc.isEncrypted
        } finally {
            doc.close()
        }
    }

    fun pdfPasswordValid(file: File, password: String): Boolean {
        if (!pdfRequiresPassword(file)) return true
        if (password.isEmpty()) return false
        val doc = FPPDFDocument.open(file, password, password)
        return try {
            doc.isOpen
        } finally {
            doc.close()
        }
    }

    fun requestPasswordPrompt(file: File) {
        passwordPromptFile = file
    }

    fun clearPasswordPrompt() {
        passwordPromptFile = null
    }

    // === Input selection ===============================================================================

    fun importPickedPdf(uri: Uri) {
        executor.execute {
            try {
                val name = queryDisplayName(uri) ?: "imported.pdf"
                val dest = uniqueDestinationFor(name)
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IllegalStateException("Cannot open $uri")
                mainHandler.post {
                    selectSourceFile(dest)
                    statusMessage = localize(R.string.status_selected, dest.name)
                }
            } catch (t: Throwable) {
                mainHandler.post { statusMessage = localize(R.string.status_import_failed, t.message ?: "unknown") }
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        appContext.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) return c.getString(idx)
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun uniqueDestinationFor(filename: String): File {
        val base = filename.substringBeforeLast('.')
        val ext = filename.substringAfterLast('.', "")
        var candidate = File(inputsDir, filename)
        var i = 1
        while (candidate.exists()) {
            val name = if (ext.isEmpty()) "$base-$i" else "$base-$i.$ext"
            candidate = File(inputsDir, name)
            i++
        }
        return candidate
    }

    private fun selectSourceFile(file: File) {
        selectedFile = file
        sourceName = file.name
        lastOutputFile = null
        lastOutputSizeText = ""
        outputFiles = emptyList()
        checkPdfPassword(file)
    }

    private fun checkPdfPassword(file: File) {
        if (!pdfRequiresPassword(file)) {
            passwordPromptFile = null
            return
        }
        if (pdfPassword.isNotEmpty() && pdfPasswordValid(file, pdfPassword)) return
        requestPasswordPrompt(file)
    }

    // === Sample library =================================================================================

    private fun loadSampleLibrary() {
        executor.execute {
            try {
                val names = appContext.assets.list("samples")?.filter { it.endsWith(".pdf") }?.sorted() ?: emptyList()
                val list = names.map { asset ->
                    val dest = File(inputsDir, asset.substringAfterLast('/'))
                    if (!dest.exists() || dest.length() == 0L) {
                        appContext.assets.open("samples/$asset").use { input ->
                            dest.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                    val pages = pageCountOf(dest).coerceAtLeast(1)
                    val size = ValueTables.fileSizeText(dest.length())
                    BundledSample(asset, dest.name, pages, size)
                }
                mainHandler.post { samples = list }
            } catch (t: Throwable) {
                mainHandler.post { statusMessage = localize(R.string.status_sample_unavailable, t.message ?: "unknown") }
            }
        }
    }

    fun prepareSample(sample: BundledSample) {
        val file = File(inputsDir, sample.fileName)
        if (!file.exists()) return
        selectSourceFile(file)
        statusMessage = localize(R.string.status_selected_sample, file.name)
    }

    fun selectedPageCount(): Int = selectedFile?.let { pageCountOf(it) } ?: 0

    fun selectedFileSizeText(): String =
        selectedFile?.let { ValueTables.fileSizeText(it.length()) } ?: ""

    // === Settings persistence ============================================================================
    // Same 38 keys as the iOS UserDefaults schema (ConverterController.m),
    // so behaviour and defaults stay identical across platforms.

    private fun loadSettings() {
        pdfPassword = prefs.getString("sourePassword", "") ?: ""
        outputFormat = OutputFormat.fromKey(prefs.getString("outputFormat", null))
        imageFormat = ImageFormat.fromOrdinal(prefs.getInt("imageFormat", ImageFormat.PNG.ordinal))
        openAfterConversion = prefs.getBoolean("settings_openAfterConversion", true)
        pageRangeMode = PageRangeMode.entries.getOrElse(prefs.getInt("settings_pageRangeSegment", 0)) { PageRangeMode.All }
        customPageRange = prefs.getString("settings_pageRange", "")?.ifEmpty { "1" } ?: "1"
        threadMode = ThreadMode.entries.getOrElse(prefs.getInt("settings_multiThreadSegment", 0)) { ThreadMode.Auto }
        customThreadCount = prefs.getString("settings_multiThread", "")?.ifEmpty { "3" } ?: "3"
        imageDPI = prefs.getInt("settings_imageDPI", 144)
        imageQualityIndex = prefs.getInt("settings_imageQuality", 3)
        wordTrimBlankSpace = prefs.getBoolean("docx_trimBlankSpace", true)
        wordMergeParagraphs = prefs.getBoolean("docx_mergeParagraph", false)
        wordShapeToImage = prefs.getBoolean("docx_enableShapToImage", true)
        wordMergeIntersectImages = prefs.getBoolean("docx_enableMergeImages", true)
        wordImageDPI = prefs.getInt("docx_imageDPI", 144)
        wordOutlineType = prefs.getInt("docx_outline", 1)
        excelAllInOneSheet = prefs.getBoolean("xlsx_allInOneSheet", false)
        excelRecognizeNumber = prefs.getBoolean("xlsx_recognizeNumber", true)
        excelAllInOneStyle = prefs.getInt("xlsx_AIOStyle", 0)
        excelFormatOption = prefs.getInt("xlsx_outputFormat", 0)
        excelThousandSeparator = prefs.getInt("xlsx_ThousandSeparator", 0)
        excelOverlapText = prefs.getInt("xlsx_OverlapText", 0)
        csvPackageZip = prefs.getBoolean("xlsx_csv_isPackageZip", false)
        htmlLayoutMode = prefs.getInt("html_layoutMode", 0)
        htmlMergeResource = prefs.getInt("html_mergeResource", 2)
        htmlNavigationBar = prefs.getInt("html_navigationBar", 1)
        htmlTextFlowParagraph = prefs.getInt("html_textFlowParagraph", 0)
        htmlPackageZip = prefs.getBoolean("html_isPackageZip", false)
        imageOutputDPI = prefs.getInt("image_imageDPI", 144)
        imageOutputQualityIndex = prefs.getInt("image_imageQuality", 3)
        imagePackageZip = prefs.getBoolean("image_isPackageZip", false)
        imageAntiAlias = prefs.getBoolean("image_isAntiAlias", true)
        elementQualityIndex = prefs.getInt("element_imageQuality", 3)
        elementPackageZip = prefs.getBoolean("element_isPackageZip", false)
        ocrEnabled = prefs.getBoolean("settings_ocr_isEnableOCR", false)
        ocrImageScan = prefs.getBoolean("settings_ocr_isEnableImageScan", false)
        ocrDPI = prefs.getInt("settings_ocr_imageDPI", 300)
        ocrLanguageText = prefs.getString("settings_ocr_languages", "")?.ifEmpty { "eng" } ?: "eng"
        ocrSelectedLanguages = ocrLanguageText.split('+').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    fun saveSettings() {
        prefs.edit()
            .putString("sourePassword", pdfPassword)
            .putString("outputFormat", outputFormat.key)
            .putInt("imageFormat", imageFormat.ordinal)
            .putBoolean("settings_openAfterConversion", openAfterConversion)
            .putInt("settings_pageRangeSegment", pageRangeMode.ordinal)
            .putString("settings_pageRange", customPageRange)
            .putInt("settings_multiThreadSegment", threadMode.ordinal)
            .putString("settings_multiThread", customThreadCount)
            .putInt("settings_imageDPI", imageDPI)
            .putInt("settings_imageQuality", imageQualityIndex)
            .putBoolean("docx_trimBlankSpace", wordTrimBlankSpace)
            .putBoolean("docx_mergeParagraph", wordMergeParagraphs)
            .putBoolean("docx_enableShapToImage", wordShapeToImage)
            .putBoolean("docx_enableMergeImages", wordMergeIntersectImages)
            .putInt("docx_imageDPI", wordImageDPI)
            .putInt("docx_outline", wordOutlineType)
            .putBoolean("xlsx_allInOneSheet", excelAllInOneSheet)
            .putBoolean("xlsx_recognizeNumber", excelRecognizeNumber)
            .putInt("xlsx_AIOStyle", excelAllInOneStyle)
            .putInt("xlsx_outputFormat", excelFormatOption)
            .putInt("xlsx_ThousandSeparator", excelThousandSeparator)
            .putInt("xlsx_OverlapText", excelOverlapText)
            .putBoolean("xlsx_csv_isPackageZip", csvPackageZip)
            .putInt("html_layoutMode", htmlLayoutMode)
            .putInt("html_mergeResource", htmlMergeResource)
            .putInt("html_navigationBar", htmlNavigationBar)
            .putInt("html_textFlowParagraph", htmlTextFlowParagraph)
            .putBoolean("html_isPackageZip", htmlPackageZip)
            .putInt("image_imageDPI", imageOutputDPI)
            .putInt("image_imageQuality", imageOutputQualityIndex)
            .putBoolean("image_isPackageZip", imagePackageZip)
            .putBoolean("image_isAntiAlias", imageAntiAlias)
            .putInt("element_imageQuality", elementQualityIndex)
            .putBoolean("element_isPackageZip", elementPackageZip)
            .putBoolean("settings_ocr_isEnableOCR", ocrEnabled)
            .putBoolean("settings_ocr_isEnableImageScan", ocrImageScan)
            .putInt("settings_ocr_imageDPI", ocrDPI)
            .putString("settings_ocr_languages", ocrLanguageText)
            .apply()
    }

    /** Language codes joined in the canonical ValueTables order (iOS parity). */
    val ocrLanguageDisplay: String
        get() = ocrLanguages.filter { ocrSelectedLanguages.contains(it.code) }.joinToString("+") { it.code }

    fun toggleOcrLanguage(code: String, isOn: Boolean) {
        val set = ocrSelectedLanguages.toMutableSet()
        if (isOn) set.add(code) else set.remove(code)
        ocrSelectedLanguages = set
        ocrLanguageText = ocrLanguages.filter { set.contains(it.code) }.joinToString("+") { it.code }
        prefs.edit().putString("settings_ocr_languages", ocrLanguageText).apply()
    }

    // === Conversion =================================================================================================

    fun startConversion() {
        if (isConverting) return
        val source = selectedFile
        if (source == null) {
            statusMessage = localize(R.string.status_pick_first)
            return
        }
        val conv = converter
        if (conv == null || !FPPDFFramework.isInitialized) {
            statusMessage = localize(R.string.status_sdk_load_failed, "SDK not initialized")
            return
        }
        saveSettings()
        isConverting = true
        progressValue = 0f
        statusMessage = localize(R.string.status_prepare)

        val destType = if (outputFormat == OutputFormat.Image) imageFormat.extension else outputFormat.key
        outputFiles = emptyList()
        statusMessage = localize(R.string.status_converting_to, destType)

        conversionJob = scope.launch {
            // Opening the document to resolve the page range touches native
            // code; keep it off the main thread together with the conversion.
            val result = kotlinx.coroutines.withContext(Dispatchers.IO) {
                val request = buildConversionRequest(source)
                // Progress / will-save streams from the shared converter
                // instance; both are cancelled as soon as this conversion
                // settles.
                val progressJob = launch {
                    conv.progress.collect { update ->
                        if (isConverting) {
                            statusMessage = localize(R.string.status_page_progress, update.page, update.totalPages)
                            progressValue =
                                if (update.totalPages > 0) update.page.toFloat() / update.totalPages else 0f
                        }
                    }
                }
                val willSaveJob = launch {
                    conv.willSaveDoc.collect { statusMessage = localize(R.string.status_saving) }
                }
                try {
                    conv.convert(request)
                } finally {
                    progressJob.cancel()
                    willSaveJob.cancel()
                }
            }
            when (result) {
                is ConversionResult.Success -> handleCompletion(
                    result.output,
                    // iOS parity: the elapsed time is formatted as %.1f only -
                    // the file name is interpolated, never fed through a
                    // format string ('%' safe).
                    String.format(java.util.Locale.US, "%.1f", result.elapsedSeconds),
                    true,
                    null,
                )
                is ConversionResult.Failure -> handleCompletion(null, "0.0", false, result.message)
                ConversionResult.Cancelled -> {
                    isConverting = false
                    progressValue = 0f
                    statusMessage = localize(R.string.status_cancelled)
                }
            }
        }
    }

    fun stopConversion() {
        converter?.cancel()
        // The UI leaves the converting state immediately; the native guard in
        // FPPDF2AllConverterImpl::convertPDFItem rejects a restart while the
        // previous conversion's worker threads (slow OCR pages) still own the
        // exporter, so a too-early Start is a safe no-op ("SDK rejected the
        // start request") instead of a use-after-free. FPPDFConverter serialises
        // conversions on its own Mutex, so convert() only returns once the
        // workers have been joined.
        isConverting = false
        progressValue = 0f
        statusMessage = localize(R.string.status_cancelled)
    }

    /** Maps the demo's persisted settings onto an SDK [ConversionRequest]. */
    private fun buildConversionRequest(source: File): ConversionRequest {
        val pageCount = pageCountOf(source).coerceAtLeast(1)
        val threadCount = when (threadMode) {
            ThreadMode.Auto -> 0
            ThreadMode.Two -> 2
            ThreadMode.Five -> 5
            ThreadMode.Ten -> 10
            ThreadMode.Custom -> customThreadCount.toIntOrNull()?.coerceIn(0, 20) ?: 0
        }
        val conversionFormat = when (outputFormat) {
            OutputFormat.Docx -> ConversionFormat.WORD
            OutputFormat.Pptx -> ConversionFormat.POWERPOINT
            OutputFormat.Xlsx -> ConversionFormat.EXCEL
            OutputFormat.Csv -> ConversionFormat.CSV
            OutputFormat.Txt -> ConversionFormat.TEXT
            OutputFormat.Html -> ConversionFormat.HTML
            OutputFormat.Image -> ConversionFormat.IMAGE
            OutputFormat.Element -> ConversionFormat.ELEMENT
        }
        val options = ConversionOptions().apply {
            parseAnnotations = true
            threadMax = threadCount
            imageDPI = this@ConverterController.imageDPI
            imageQuality = qualityValues.getOrElse(imageQualityIndex) { 0.83f }
            enableOCR = ocrEnabled

            word {
                trimBlankSpace = wordTrimBlankSpace
                mergeParagraphs = wordMergeParagraphs
                outlineType = OutlineType.entries.getOrElse(wordOutlineType) { OutlineType.PDF_OUTLINE }
                shapeToImage = wordShapeToImage
                mergeIntersectImages = wordMergeIntersectImages
            }
            html {
                layoutMode = com.flyingbee.FPPDFFramework.HtmlLayoutMode.entries.getOrElse(htmlLayoutMode) { com.flyingbee.FPPDFFramework.HtmlLayoutMode.EXACT_PAGE }
                mergeResource = com.flyingbee.FPPDFFramework.HtmlMergeResource.entries.getOrElse(htmlMergeResource) { com.flyingbee.FPPDFFramework.HtmlMergeResource.CSS_JS_SMALL_IMAGES }
                navigationBar = com.flyingbee.FPPDFFramework.HtmlNavigationBar.entries.getOrElse(htmlNavigationBar) { com.flyingbee.FPPDFFramework.HtmlNavigationBar.PDF_VIEWER }
                textFlowParagraph = com.flyingbee.FPPDFFramework.HtmlTextFlowParagraph.entries.getOrElse(htmlTextFlowParagraph) { com.flyingbee.FPPDFFramework.HtmlTextFlowParagraph.LINE_BREAK }
                packageAsZip = htmlPackageZip
            }
            excel {
                formatOption = com.flyingbee.FPPDFFramework.ExcelFormatOption.entries.getOrElse(excelFormatOption) { com.flyingbee.FPPDFFramework.ExcelFormatOption.KEEP_ORIGINAL_FORMATTING }
                thousandSeparator = com.flyingbee.FPPDFFramework.ExcelThousandSeparator.entries.getOrElse(excelThousandSeparator) { com.flyingbee.FPPDFFramework.ExcelThousandSeparator.AUTO }
                allInOneSheet = excelAllInOneSheet
                allInOneSheetAddToRow = excelAllInOneStyle == 0
                recognizeNumber = excelRecognizeNumber
                overlapText = com.flyingbee.FPPDFFramework.ExcelOverlapText.entries.getOrElse(excelOverlapText) { com.flyingbee.FPPDFFramework.ExcelOverlapText.AUTO }
                csvPackageZip = this@ConverterController.csvPackageZip
            }
            image {
                // The demo's ImageFormat enum keeps the same order as the SDK's.
                format = SdkImageFormat.entries.getOrElse(imageFormat.ordinal) { SdkImageFormat.PNG }
                dpi = imageOutputDPI
                quality = qualityValues.getOrElse(imageOutputQualityIndex) { 0.83f }
                packageAsZip = imagePackageZip
                antiAlias = imageAntiAlias
            }
            element {
                quality = qualityValues.getOrElse(elementQualityIndex) { 0.83f }
                packageAsZip = elementPackageZip
            }
            ocr {
                language = ocrLanguageText.ifEmpty { "eng" }
                engineMode = com.flyingbee.FPPDFFramework.OcrEngineMode.LSTM_ONLY
                resizeDPI = ocrDPI
                minConfidence = 10.0f
                enableImageScan = ocrImageScan
            }
        }
        return ConversionRequest(
            pdfFile = source,
            password = pdfPassword.ifEmpty { null },
            pages = PageRange.Pages(buildPageIndexes(pageCount)),
            outputDir = outputDir,
            format = conversionFormat,
            options = options,
        )
    }

    fun buildPageIndexes(pageCount: Int): List<Int> = when (pageRangeMode) {
        PageRangeMode.All -> (1..pageCount.coerceAtLeast(1)).toList()
        PageRangeMode.First10 -> (1..minOf(10, pageCount)).toList()
        PageRangeMode.First3 -> (1..minOf(3, pageCount)).toList()
        PageRangeMode.First1 -> listOf(1)
        PageRangeMode.Custom -> {
            val out = mutableListOf<Int>()
            for (part in customPageRange.split(',')) {
                val piece = part.trim()
                if (piece.contains('-')) {
                    val bounds = piece.split('-')
                    if (bounds.size == 2) {
                        val first = bounds[0].trim().toIntOrNull() ?: continue
                        val last = bounds[1].trim().toIntOrNull() ?: continue
                        if (last >= first) {
                            for (v in first..last) if (v in 1..pageCount) out.add(v)
                        } else {
                            for (v in first downTo last) if (v in 1..pageCount) out.add(v)
                        }
                    }
                } else {
                    val v = piece.toIntOrNull()
                    if (v != null && v in 1..pageCount) out.add(v)
                }
            }
            out.ifEmpty { listOf(1) }
        }
    }

    /** Finalises the UI state after a conversion settles. [dest] is the output
     *  file/folder reported by the SDK (null on failure). */
    private fun handleCompletion(dest: File?, elapsedText: String, success: Boolean, errorInfo: String?) {
        isConverting = false
        progressValue = 0f

        if (!success) {
            lastOutputFile = null
            outputFiles = emptyList()
            statusMessage = localize(R.string.status_failed, errorInfo ?: "Unknown error")
            return
        }
        if (dest == null || !dest.exists()) {
            lastOutputFile = null
            outputFiles = emptyList()
            statusMessage = localize(R.string.status_no_output)
            return
        }

        when {
            dest.isFile -> {
                lastOutputFile = dest
                lastOutputSizeText = ValueTables.fileSizeText(dest.length())
                outputFiles = listOf(OutputFileItem(dest, lastOutputSizeText))
                statusMessage = localize(
                    R.string.status_success_file,
                    outputFormat.key, elapsedText, dest.name, lastOutputSizeText,
                )
                if (openAfterConversion) previewRequest = dest
            }
            dest.isDirectory -> {
                lastOutputFile = dest
                val files = dest.walkTopDown().filter { it.isFile }.sortedBy { it.name }.toList()
                val total = files.sumOf { it.length() }
                lastOutputSizeText = ValueTables.fileSizeText(total)
                outputFiles = files.map { OutputFileItem(it, ValueTables.fileSizeText(it.length())) }
                statusMessage = localize(
                    R.string.status_success_folder,
                    outputFormat.key, elapsedText, lastOutputSizeText, files.size, dest.name,
                )
                if (openAfterConversion) files.firstOrNull()?.let { previewRequest = it }
            }
            else -> {
                lastOutputFile = null
                outputFiles = emptyList()
                statusMessage = localize(R.string.status_no_output)
            }
        }
    }

    // === UI actions ==============================================================================================

    fun setStatus(msg: String) {
        // Raw text (e.g. built by the Activity): drop the resource record so a
        // later locale switch does not resurrect a stale message.
        lastStatusResId = null
        statusMessage = msg
    }

    fun consumePreview() { previewRequest = null }
    fun consumeShare() { shareRequest = null }

    fun previewOne(file: File) { previewRequest = file }

    fun previewSource() { selectedFile?.let { previewRequest = it } }

    /** Share the produced artifacts. Android FileProvider cannot share a
     *  directory itself, so a folder output shares its first file. */
    fun shareAllOutput() {
        shareRequest = if (outputFiles.size == 1) outputFiles.first().file
            else outputFiles.firstOrNull()?.file ?: lastOutputFile
    }

    fun openUrl(url: String) {
        try {
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(url),
            )
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
        } catch (t: Throwable) {
            statusMessage = localize(R.string.status_cannot_open, url)
        }
    }
}
