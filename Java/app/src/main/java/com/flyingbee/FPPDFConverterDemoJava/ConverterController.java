package com.flyingbee.FPPDFConverterDemoJava;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;

import com.flyingbee.FPPDFFramework.Java.ConversionFormat;
import com.flyingbee.FPPDFFramework.Java.ConversionOptions;
import com.flyingbee.FPPDFFramework.Java.ConversionRequest;
import com.flyingbee.FPPDFFramework.Java.ConversionResult;
import com.flyingbee.FPPDFFramework.Java.ConvertCallback;
import com.flyingbee.FPPDFFramework.Java.FPPDFConverter;
import com.flyingbee.FPPDFFramework.Java.FPPDFDocument;
import com.flyingbee.FPPDFFramework.Java.FPPDFFramework;
import com.flyingbee.FPPDFFramework.Java.OutlineType;
import com.flyingbee.FPPDFFramework.Java.PageRange;
import com.flyingbee.FPPDFFramework.Java.ProgressUpdate;
import com.flyingbee.FPPDFFramework.Java.HtmlLayoutMode;
import com.flyingbee.FPPDFFramework.Java.HtmlMergeResource;
import com.flyingbee.FPPDFFramework.Java.HtmlNavigationBar;
import com.flyingbee.FPPDFFramework.Java.HtmlTextFlowParagraph;
import com.flyingbee.FPPDFFramework.Java.ExcelFormatOption;
import com.flyingbee.FPPDFFramework.Java.ExcelThousandSeparator;
import com.flyingbee.FPPDFFramework.Java.ExcelOverlapText;
import com.flyingbee.FPPDFFramework.Java.OcrEngineMode;

import com.flyingbee.FPPDFConverterDemoJava.Models.BundledSample;
import com.flyingbee.FPPDFConverterDemoJava.Models.ImageFormat;
import com.flyingbee.FPPDFConverterDemoJava.Models.OcrLanguage;
import com.flyingbee.FPPDFConverterDemoJava.Models.OutputFileItem;
import com.flyingbee.FPPDFConverterDemoJava.Models.OutputFormat;
import com.flyingbee.FPPDFConverterDemoJava.Models.PageRangeMode;
import com.flyingbee.FPPDFConverterDemoJava.Models.ThreadMode;
import com.flyingbee.FPPDFConverterDemoJava.Models.ValueTables;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ConverterController : the single source of truth for the demo (Java
 * counterpart of the Kotlin demo's ConverterController). Holds the selected
 * input, all 38 persisted settings (same keys as the iOS UserDefaults schema),
 * drives the SDK's Java API and notifies the visible screen when state changes.
 *
 * It is a process-wide singleton owned by the application context: every screen
 * reads and writes the same instance, mirroring the shared Compose ViewModel.
 * Instead of Compose snapshot state, a single {@link StateListener} callback
 * asks the foreground Activity to re-render from the fields below.
 */
public final class ConverterController {

    /** Re-invoked on the main thread whenever any observable state changes. */
    public interface StateListener {
        void onStateChanged();
    }

    private static ConverterController sInstance;

    public static ConverterController get() {
        if (sInstance == null) {
            throw new IllegalStateException("Call init(app) first");
        }
        return sInstance;
    }

    public static synchronized void init(Context appContext) {
        if (sInstance == null) sInstance = new ConverterController(appContext.getApplicationContext());
    }

    // -----------------------------------------------------------------------
    private final Context appContext;
    private final SharedPreferences prefs;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private StateListener listener;
    private boolean didRunStartup = false;

    private FPPDFConverter converter;

    private ConverterController(Context app) {
        this.appContext = app;
        this.prefs = app.getSharedPreferences("fpdf_settings", Context.MODE_PRIVATE);
        statusMessage = localize(R.string.status_ready);
        loadSettings();
        loadLicenseInfo();
    }

    /** The visible screen registers itself so it can be told to re-render. */
    public void setListener(StateListener l) { this.listener = l; }

    /** Detach only if the given listener is still the active one (screen teardown). */
    public void clearListener(StateListener l) { if (this.listener == l) this.listener = null; }

    /** Bump the listener on the main thread. */
    public void notifyChanged() {
        StateListener l = listener;
        if (l == null) return;
        if (Looper.myLooper() == Looper.getMainLooper()) l.onStateChanged();
        else mainHandler.post(l::onStateChanged);
    }

    /** Persist all settings and refresh the UI (the common "edit done" path). */
    public void commit() {
        saveSettings();
        notifyChanged();
    }

    // --- Localisation helper ----------------------------------------------
    private Integer lastStatusResId;
    private Object[] lastStatusArgs = new Object[0];

    private String localize(int resId, Object... args) {
        lastStatusResId = resId;
        lastStatusArgs = args;
        return args.length == 0
            ? appContext.getString(resId)
            : appContext.getString(resId, args);
    }

    /** Application versionName, shown in the About footer. */
    public String getAppVersionName() {
        try {
            android.content.pm.PackageInfo pi =
                appContext.getPackageManager().getPackageInfo(appContext.getPackageName(), 0);
            return pi.versionName != null ? pi.versionName : "1.0";
        } catch (Throwable t) {
            return "1.0";
        }
    }

    // --- Paths ---------------------------------------------------------------
    private File inputsDir() {
        File d = new File(appContext.getFilesDir(), "FPPDFInputs");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    public File outputDir() {
        File base = appContext.getExternalFilesDir(null);
        File d = new File(base != null ? base : appContext.getFilesDir(), "FPPDFOutput");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    // --- Shared value tables -------------------------------------------------
    public final List<Integer> dpiValues = ValueTables.dpiValues;
    public final List<Float> qualityValues = ValueTables.qualityValues;
    public final List<Integer> wordDpiValues = ValueTables.wordDpiValues;
    public final List<Integer> ocrDpiValues = ValueTables.ocrDpiValues;
    public final List<OcrLanguage> ocrLanguages = ValueTables.ocrLanguages;

    /** Localised quality labels, recomputed on every access (locale aware). */
    public List<String> qualityLabels() {
        List<String> out = new ArrayList<>();
        for (int res : ValueTables.qualityLabelRes) out.add(appContext.getString(res));
        return out;
    }

    // --- Source / status -----------------------------------------------------
    public File selectedFile = null;
    public String sourceName = "";
    public String pdfPassword = "";
    public boolean isConverting = false;
    public float progressValue = 0f;
    public String statusMessage = "";

    public String licenseOrganization = "";
    public String licenseExpiredDate = "";
    public boolean licenseExpired = false;
    public String sdkVersionText = "";

    public File lastOutputFile = null;
    public String lastOutputSizeText = "";
    public List<OutputFileItem> outputFiles = new ArrayList<>();

    public List<BundledSample> samples = new ArrayList<>();

    // --- Format --------------------------------------------------------------
    public OutputFormat outputFormat = OutputFormat.Docx;
    public ImageFormat imageFormat = ImageFormat.PNG;

    // --- General settings ----------------------------------------------------
    public boolean openAfterConversion = true;
    public PageRangeMode pageRangeMode = PageRangeMode.All;
    public String customPageRange = "1";
    public ThreadMode threadMode = ThreadMode.Auto;
    public String customThreadCount = "3";
    public int imageDPI = 144;
    public int imageQualityIndex = 3;

    // --- Word ----------------------------------------------------------------
    public boolean wordTrimBlankSpace = true;
    public boolean wordMergeParagraphs = false;
    public boolean wordShapeToImage = true;
    public boolean wordMergeIntersectImages = true;
    public int wordImageDPI = 144;
    public int wordOutlineType = 1;

    // --- Excel ---------------------------------------------------------------
    public boolean excelAllInOneSheet = false;
    public boolean excelRecognizeNumber = true;
    public int excelAllInOneStyle = 0;
    public int excelFormatOption = 0;
    public int excelThousandSeparator = 0;
    public int excelOverlapText = 0;
    public boolean csvPackageZip = false;

    // --- HTML ----------------------------------------------------------------
    public int htmlLayoutMode = 0;
    public int htmlMergeResource = 2;
    public int htmlNavigationBar = 1;
    public int htmlTextFlowParagraph = 0;
    public boolean htmlPackageZip = false;

    // --- Image output --------------------------------------------------------
    public int imageOutputDPI = 144;
    public int imageOutputQualityIndex = 3;
    public boolean imagePackageZip = false;
    public boolean imageAntiAlias = true;

    // --- Element output ------------------------------------------------------
    public int elementQualityIndex = 3;
    public boolean elementPackageZip = false;

    // --- OCR -----------------------------------------------------------------
    public boolean ocrEnabled = false;
    public boolean ocrImageScan = false;
    public int ocrDPI = 300;
    public String ocrLanguageText = "eng";
    public Set<String> ocrSelectedLanguages = new LinkedHashSet<>();

    // --- One-shot UI requests ------------------------------------------------
    public File passwordPromptFile = null;
    public File previewRequest = null;
    public File shareRequest = null;

    // === Lifecycle ===========================================================

    public void startup() {
        if (lastStatusResId != null) statusMessage = localize(lastStatusResId, lastStatusArgs);
        if (didRunStartup) { notifyChanged(); return; }
        didRunStartup = true;
        ioExecutor.execute(() -> {
            try {
                FPPDFFramework.initialize(appContext);
                if (converter == null) converter = new FPPDFConverter();
            } catch (Throwable t) {
                mainHandler.post(() -> {
                    statusMessage = localize(R.string.status_sdk_load_failed,
                        t.getMessage() != null ? t.getMessage() : "unknown");
                    notifyChanged();
                });
                return;
            }
            mainHandler.post(() -> {
                loadSampleLibrary();
                loadLicenseInfo();
                if (sourceName.isEmpty()) statusMessage = localize(R.string.status_ready_pick);
                notifyChanged();
            });
        });
    }

    public void shutdown() {
        if (converter != null) {
            converter.cancel();
            converter.close();
            converter = null;
        }
    }

    private void loadLicenseInfo() {
        try {
            FPPDFFramework.LicenseInfo license = FPPDFFramework.getLicense();
            licenseOrganization = license.organization;
            licenseExpiredDate = license.expiredDate;
            licenseExpired = license.isExpired;
            sdkVersionText = FPPDFFramework.getVersion() + " (" + FPPDFFramework.getReleaseDate() + ")";
        } catch (Throwable t) {
            statusMessage = localize(R.string.status_sdk_load_failed,
                t.getMessage() != null ? t.getMessage() : "unknown");
        }
    }

    // === PDF helpers =========================================================

    public int pageCountOf(File file) {
        if (!FPPDFFramework.isInitialized()) return 0;
        String pw = pdfPassword.isEmpty() ? null : pdfPassword;
        FPPDFDocument doc = FPPDFDocument.open(file, pw, pw);
        try {
            return doc.isOpen() ? doc.getPageCount() : 0;
        } finally {
            doc.close();
        }
    }

    public boolean pdfRequiresPassword(File file) {
        if (!FPPDFFramework.isInitialized()) return false;
        FPPDFDocument doc = FPPDFDocument.open(file);
        try {
            return doc.isEncrypted();
        } finally {
            doc.close();
        }
    }

    public boolean pdfPasswordValid(File file, String password) {
        if (!pdfRequiresPassword(file)) return true;
        if (password.isEmpty()) return false;
        FPPDFDocument doc = FPPDFDocument.open(file, password, password);
        try {
            return doc.isOpen();
        } finally {
            doc.close();
        }
    }

    public void requestPasswordPrompt(File file) {
        passwordPromptFile = file;
        notifyChanged();
    }

    public void clearPasswordPrompt() {
        passwordPromptFile = null;
        notifyChanged();
    }

    // === Input selection =====================================================

    public void importPickedPdf(Uri uri) {
        ioExecutor.execute(() -> {
            try {
                String name = queryDisplayName(uri);
                if (name == null) name = "imported.pdf";
                File dest = uniqueDestinationFor(name);
                InputStream input = appContext.getContentResolver().openInputStream(uri);
                if (input == null) throw new IllegalStateException("Cannot open " + uri);
                try {
                    OutputStream output = new FileOutputStream(dest);
                    try { copy(input, output); } finally { output.close(); }
                } finally {
                    input.close();
                }
                final File f = dest;
                mainHandler.post(() -> {
                    selectSourceFile(f);
                    statusMessage = localize(R.string.status_selected, f.getName());
                    notifyChanged();
                });
            } catch (Throwable t) {
                mainHandler.post(() -> {
                    statusMessage = localize(R.string.status_import_failed,
                        t.getMessage() != null ? t.getMessage() : "unknown");
                    notifyChanged();
                });
            }
        });
    }

    private static void copy(InputStream in, OutputStream out) throws java.io.IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
    }

    private String queryDisplayName(Uri uri) {
        ContentResolver cr = appContext.getContentResolver();
        android.database.Cursor c = cr.query(uri, null, null, null, null);
        if (c != null) {
            try {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0 && c.moveToFirst()) return c.getString(idx);
            } finally {
                c.close();
            }
        }
        String last = uri.getLastPathSegment();
        if (last != null) {
            int slash = last.lastIndexOf('/');
            return slash >= 0 ? last.substring(slash + 1) : last;
        }
        return null;
    }

    private File uniqueDestinationFor(String filename) {
        int dot = filename.lastIndexOf('.');
        String base = dot > 0 ? filename.substring(0, dot) : filename;
        String ext = dot > 0 ? filename.substring(dot + 1) : "";
        File candidate = new File(inputsDir(), filename);
        int i = 1;
        while (candidate.exists()) {
            String name = ext.isEmpty() ? base + "-" + i : base + "-" + i + "." + ext;
            candidate = new File(inputsDir(), name);
            i++;
        }
        return candidate;
    }

    private void selectSourceFile(File file) {
        selectedFile = file;
        sourceName = file.getName();
        lastOutputFile = null;
        lastOutputSizeText = "";
        outputFiles = new ArrayList<>();
        checkPdfPassword(file);
    }

    private void checkPdfPassword(File file) {
        if (!pdfRequiresPassword(file)) { passwordPromptFile = null; return; }
        if (!pdfPassword.isEmpty() && pdfPasswordValid(file, pdfPassword)) return;
        passwordPromptFile = file;
    }

    // === Sample library ======================================================

    private void loadSampleLibrary() {
        ioExecutor.execute(() -> {
            try {
                String[] all = appContext.getAssets().list("samples");
                List<String> names = new ArrayList<>();
                if (all != null) for (String s : all) if (s.endsWith(".pdf")) names.add(s);
                // Collections.sort() instead of List#sort(): the Java 8 default
                // method is missing from some ROMs' core-libart.jar and throws
                // NoSuchMethodError at runtime (seen on API 23).
                Collections.sort(names);
                final List<BundledSample> list = new ArrayList<>();
                for (String asset : names) {
                    File dest = new File(inputsDir(), asset.substring(asset.lastIndexOf('/') + 1));
                    if (!dest.exists() || dest.length() == 0L) {
                        InputStream input = appContext.getAssets().open("samples/" + asset);
                        try {
                            OutputStream output = new FileOutputStream(dest);
                            try { copy(input, output); } finally { output.close(); }
                        } finally {
                            input.close();
                        }
                    }
                    int pages = Math.max(1, pageCountOf(dest));
                    String size = ValueTables.fileSizeText(dest.length());
                    list.add(new BundledSample(asset, dest.getName(), pages, size));
                }
                mainHandler.post(() -> { samples = list; notifyChanged(); });
            } catch (Throwable t) {
                mainHandler.post(() -> {
                    statusMessage = localize(R.string.status_sample_unavailable,
                        t.getMessage() != null ? t.getMessage() : "unknown");
                    notifyChanged();
                });
            }
        });
    }

    public void prepareSample(BundledSample sample) {
        File file = new File(inputsDir(), sample.fileName);
        if (!file.exists()) return;
        selectSourceFile(file);
        statusMessage = localize(R.string.status_selected_sample, file.getName());
        notifyChanged();
    }

    public int selectedPageCount() {
        return selectedFile != null ? pageCountOf(selectedFile) : 0;
    }

    public String selectedFileSizeText() {
        return selectedFile != null ? ValueTables.fileSizeText(selectedFile.length()) : "";
    }

    // === Settings persistence ===============================================

    private void loadSettings() {
        pdfPassword = prefs.getString("sourePassword", "");
        outputFormat = OutputFormat.fromKey(prefs.getString("outputFormat", null));
        imageFormat = ImageFormat.fromOrdinal(prefs.getInt("imageFormat", ImageFormat.PNG.ordinal()));
        openAfterConversion = prefs.getBoolean("settings_openAfterConversion", true);
        pageRangeMode = PageRangeMode.fromOrdinal(prefs.getInt("settings_pageRangeSegment", 0));
        String pr = prefs.getString("settings_pageRange", "");
        customPageRange = (pr == null || pr.isEmpty()) ? "1" : pr;
        threadMode = ThreadMode.fromOrdinal(prefs.getInt("settings_multiThreadSegment", 0));
        String mt = prefs.getString("settings_multiThread", "");
        customThreadCount = (mt == null || mt.isEmpty()) ? "3" : mt;
        imageDPI = prefs.getInt("settings_imageDPI", 144);
        imageQualityIndex = prefs.getInt("settings_imageQuality", 3);
        wordTrimBlankSpace = prefs.getBoolean("docx_trimBlankSpace", true);
        wordMergeParagraphs = prefs.getBoolean("docx_mergeParagraph", false);
        wordShapeToImage = prefs.getBoolean("docx_enableShapToImage", true);
        wordMergeIntersectImages = prefs.getBoolean("docx_enableMergeImages", true);
        wordImageDPI = prefs.getInt("docx_imageDPI", 144);
        wordOutlineType = prefs.getInt("docx_outline", 1);
        excelAllInOneSheet = prefs.getBoolean("xlsx_allInOneSheet", false);
        excelRecognizeNumber = prefs.getBoolean("xlsx_recognizeNumber", true);
        excelAllInOneStyle = prefs.getInt("xlsx_AIOStyle", 0);
        excelFormatOption = prefs.getInt("xlsx_outputFormat", 0);
        excelThousandSeparator = prefs.getInt("xlsx_ThousandSeparator", 0);
        excelOverlapText = prefs.getInt("xlsx_OverlapText", 0);
        csvPackageZip = prefs.getBoolean("xlsx_csv_isPackageZip", false);
        htmlLayoutMode = prefs.getInt("html_layoutMode", 0);
        htmlMergeResource = prefs.getInt("html_mergeResource", 2);
        htmlNavigationBar = prefs.getInt("html_navigationBar", 1);
        htmlTextFlowParagraph = prefs.getInt("html_textFlowParagraph", 0);
        htmlPackageZip = prefs.getBoolean("html_isPackageZip", false);
        imageOutputDPI = prefs.getInt("image_imageDPI", 144);
        imageOutputQualityIndex = prefs.getInt("image_imageQuality", 3);
        imagePackageZip = prefs.getBoolean("image_isPackageZip", false);
        imageAntiAlias = prefs.getBoolean("image_isAntiAlias", true);
        elementQualityIndex = prefs.getInt("element_imageQuality", 3);
        elementPackageZip = prefs.getBoolean("element_isPackageZip", false);
        ocrEnabled = prefs.getBoolean("settings_ocr_isEnableOCR", false);
        ocrImageScan = prefs.getBoolean("settings_ocr_isEnableImageScan", false);
        ocrDPI = prefs.getInt("settings_ocr_imageDPI", 300);
        String ocrLangs = prefs.getString("settings_ocr_languages", "");
        ocrLanguageText = (ocrLangs == null || ocrLangs.isEmpty()) ? "eng" : ocrLangs;
        ocrSelectedLanguages = new LinkedHashSet<>();
        for (String p : ocrLanguageText.split("\\+")) {
            String t = p.trim();
            if (!t.isEmpty()) ocrSelectedLanguages.add(t);
        }
    }

    public void saveSettings() {
        prefs.edit()
            .putString("sourePassword", pdfPassword)
            .putString("outputFormat", outputFormat.key)
            .putInt("imageFormat", imageFormat.ordinal())
            .putBoolean("settings_openAfterConversion", openAfterConversion)
            .putInt("settings_pageRangeSegment", pageRangeMode.ordinal())
            .putString("settings_pageRange", customPageRange)
            .putInt("settings_multiThreadSegment", threadMode.ordinal())
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
            .apply();
    }

    /** Language codes joined in the canonical ValueTables order (iOS parity). */
    public String ocrLanguageDisplay() {
        StringBuilder sb = new StringBuilder();
        for (OcrLanguage l : ocrLanguages) {
            if (ocrSelectedLanguages.contains(l.code)) {
                if (sb.length() > 0) sb.append('+');
                sb.append(l.code);
            }
        }
        return sb.toString();
    }

    public void toggleOcrLanguage(String code, boolean isOn) {
        Set<String> set = new LinkedHashSet<>(ocrSelectedLanguages);
        if (isOn) set.add(code); else set.remove(code);
        ocrSelectedLanguages = set;
        StringBuilder sb = new StringBuilder();
        for (OcrLanguage l : ocrLanguages) {
            if (set.contains(l.code)) {
                if (sb.length() > 0) sb.append('+');
                sb.append(l.code);
            }
        }
        ocrLanguageText = sb.toString();
        prefs.edit().putString("settings_ocr_languages", ocrLanguageText).apply();
        notifyChanged();
    }

    // === Conversion ==========================================================

    public void startConversion() {
        if (isConverting) return;
        final File source = selectedFile;
        if (source == null) {
            statusMessage = localize(R.string.status_pick_first);
            notifyChanged();
            return;
        }
        final FPPDFConverter conv = converter;
        if (conv == null || !FPPDFFramework.isInitialized()) {
            statusMessage = localize(R.string.status_sdk_load_failed, "SDK not initialized");
            notifyChanged();
            return;
        }
        saveSettings();
        isConverting = true;
        progressValue = 0f;
        statusMessage = localize(R.string.status_prepare);

        String destType = outputFormat == OutputFormat.Image ? imageFormat.extension : outputFormat.key;
        outputFiles = new ArrayList<>();
        statusMessage = localize(R.string.status_converting_to, destType);
        notifyChanged();

        final ConversionRequest request = buildConversionRequest(source);
        conv.convertAsync(request, new ConvertCallback() {
            @Override
            public void onProgress(ProgressUpdate update) {
                if (isConverting) {
                    mainHandler.post(() -> {
                        statusMessage = localize(R.string.status_page_progress,
                            update.getPage(), update.getTotalPages());
                        progressValue = update.getTotalPages() > 0
                            ? (float) update.getPage() / update.getTotalPages() : 0f;
                        notifyChanged();
                    });
                }
            }

            @Override
            public void onWillSaveDoc() {
                mainHandler.post(() -> {
                    statusMessage = localize(R.string.status_saving);
                    notifyChanged();
                });
            }

            @Override
            public void onComplete(ConversionResult result) {
                mainHandler.post(() -> {
                    if (result instanceof ConversionResult.Success) {
                        ConversionResult.Success s = (ConversionResult.Success) result;
                        handleCompletion(s.getOutput(),
                            String.format(Locale.US, "%.1f", s.getElapsedSeconds()), true, null);
                    } else if (result instanceof ConversionResult.Failure) {
                        handleCompletion(null, "0.0", false, ((ConversionResult.Failure) result).getMessage());
                    } else {
                        isConverting = false;
                        progressValue = 0f;
                        statusMessage = localize(R.string.status_cancelled);
                    }
                    notifyChanged();
                });
            }
        });
    }

    public void stopConversion() {
        if (converter != null) converter.cancel();
        isConverting = false;
        progressValue = 0f;
        statusMessage = localize(R.string.status_cancelled);
        notifyChanged();
    }

    /** Maps the demo's persisted settings onto an SDK ConversionRequest. */
    private ConversionRequest buildConversionRequest(File source) {
        int pageCount = Math.max(1, pageCountOf(source));
        int threadCount;
        switch (threadMode) {
            case Two: threadCount = 2; break;
            case Five: threadCount = 5; break;
            case Ten: threadCount = 10; break;
            case Custom:
                Integer parsed = parseIntOrNull(customThreadCount);
                threadCount = parsed != null ? clamp(parsed, 0, 20) : 0;
                break;
            default: threadCount = 0;
        }
        ConversionFormat conversionFormat;
        switch (outputFormat) {
            case Docx: conversionFormat = ConversionFormat.WORD; break;
            case Pptx: conversionFormat = ConversionFormat.POWERPOINT; break;
            case Xlsx: conversionFormat = ConversionFormat.EXCEL; break;
            case Csv: conversionFormat = ConversionFormat.CSV; break;
            case Txt: conversionFormat = ConversionFormat.TEXT; break;
            case Html: conversionFormat = ConversionFormat.HTML; break;
            case Image: conversionFormat = ConversionFormat.IMAGE; break;
            default: conversionFormat = ConversionFormat.ELEMENT; break;
        }

        ConversionOptions options = new ConversionOptions();
        options.setParseAnnotations(true)
            .setThreadMax(threadCount)
            .setImageDpi(imageDPI)
            .setImageQuality(safeQuality(imageQualityIndex))
            .setEnableOcr(ocrEnabled);

        options.getWord()
            .setTrimBlankSpace(wordTrimBlankSpace)
            .setMergeParagraphs(wordMergeParagraphs)
            .setOutlineType(OutlineType.values()[clamp(wordOutlineType, 0, OutlineType.values().length - 1)])
            .setShapeToImage(wordShapeToImage)
            .setMergeIntersectImages(wordMergeIntersectImages);

        options.getHtml()
            .setLayoutMode(HtmlLayoutMode.values()[clamp(htmlLayoutMode, 0, HtmlLayoutMode.values().length - 1)])
            .setMergeResource(HtmlMergeResource.values()[clamp(htmlMergeResource, 0, HtmlMergeResource.values().length - 1)])
            .setNavigationBar(HtmlNavigationBar.values()[clamp(htmlNavigationBar, 0, HtmlNavigationBar.values().length - 1)])
            .setTextFlowParagraph(HtmlTextFlowParagraph.values()[clamp(htmlTextFlowParagraph, 0, HtmlTextFlowParagraph.values().length - 1)])
            .setPackageAsZip(htmlPackageZip);

        options.getExcel()
            .setFormatOption(ExcelFormatOption.values()[clamp(excelFormatOption, 0, ExcelFormatOption.values().length - 1)])
            .setThousandSeparator(ExcelThousandSeparator.values()[clamp(excelThousandSeparator, 0, ExcelThousandSeparator.values().length - 1)])
            .setAllInOneSheet(excelAllInOneSheet)
            .setAllInOneSheetAddToRow(excelAllInOneStyle == 0)
            .setRecognizeNumber(excelRecognizeNumber)
            .setOverlapText(ExcelOverlapText.values()[clamp(excelOverlapText, 0, ExcelOverlapText.values().length - 1)])
            .setCsvPackageZip(csvPackageZip);

        options.getImage()
            .setFormat(com.flyingbee.FPPDFFramework.Java.ImageFormat.values()[clamp(imageFormat.ordinal(), 0, com.flyingbee.FPPDFFramework.Java.ImageFormat.values().length - 1)])
            .setDpi(imageOutputDPI)
            .setQuality(safeQuality(imageOutputQualityIndex))
            .setPackageAsZip(imagePackageZip)
            .setAntiAlias(imageAntiAlias);

        options.getElement()
            .setQuality(safeQuality(elementQualityIndex))
            .setPackageAsZip(elementPackageZip);

        options.getOcr()
            .setLanguage(ocrLanguageText.isEmpty() ? "eng" : ocrLanguageText)
            .setEngineMode(OcrEngineMode.LSTM_ONLY)
            .setResizeDpi(ocrDPI)
            .setMinConfidence(10.0f)
            .setEnableImageScan(ocrImageScan);

        List<Integer> pages = buildPageIndexes(pageCount);

        ConversionRequest.Builder builder = new ConversionRequest.Builder(source, outputDir(), conversionFormat)
            .pages(PageRange.pages(pages))
            .options(options);
        if (!pdfPassword.isEmpty()) builder.password(pdfPassword);
        return builder.build();
    }

    private float safeQuality(int index) {
        return index >= 0 && index < qualityValues.size() ? qualityValues.get(index) : 0.83f;
    }

    public List<Integer> buildPageIndexes(int pageCount) {
        List<Integer> out = new ArrayList<>();
        switch (pageRangeMode) {
            case All:
                for (int i = 1; i <= Math.max(1, pageCount); i++) out.add(i);
                break;
            case First10:
                for (int i = 1; i <= Math.min(10, pageCount); i++) out.add(i);
                break;
            case First3:
                for (int i = 1; i <= Math.min(3, pageCount); i++) out.add(i);
                break;
            case First1:
                out.add(1);
                break;
            case Custom:
                for (String part : customPageRange.split(",")) {
                    String piece = part.trim();
                    if (piece.contains("-")) {
                        String[] bounds = piece.split("-");
                        if (bounds.length == 2) {
                            Integer first = parseIntOrNull(bounds[0].trim());
                            Integer last = parseIntOrNull(bounds[1].trim());
                            if (first == null || last == null) continue;
                            if (last >= first) {
                                for (int v = first; v <= last; v++) if (v >= 1 && v <= pageCount) out.add(v);
                            } else {
                                for (int v = first; v >= last; v--) if (v >= 1 && v <= pageCount) out.add(v);
                            }
                        }
                    } else {
                        Integer v = parseIntOrNull(piece);
                        if (v != null && v >= 1 && v <= pageCount) out.add(v);
                    }
                }
                if (out.isEmpty()) out.add(1);
                break;
        }
        return out;
    }

    /** Finalises the UI state after a conversion settles. */
    private void handleCompletion(File dest, String elapsedText, boolean success, String errorInfo) {
        isConverting = false;
        progressValue = 0f;

        if (!success) {
            lastOutputFile = null;
            outputFiles = new ArrayList<>();
            statusMessage = localize(R.string.status_failed, errorInfo != null ? errorInfo : "Unknown error");
            return;
        }
        if (dest == null || !dest.exists()) {
            lastOutputFile = null;
            outputFiles = new ArrayList<>();
            statusMessage = localize(R.string.status_no_output);
            return;
        }

        if (dest.isFile()) {
            lastOutputFile = dest;
            lastOutputSizeText = ValueTables.fileSizeText(dest.length());
            outputFiles = new ArrayList<>();
            outputFiles.add(new OutputFileItem(dest, lastOutputSizeText));
            statusMessage = localize(R.string.status_success_file,
                outputFormat.key, elapsedText, dest.getName(), lastOutputSizeText);
            if (openAfterConversion) previewRequest = dest;
        } else if (dest.isDirectory()) {
            lastOutputFile = dest;
            List<File> files = new ArrayList<>();
            collectFiles(dest, files);
            Collections.sort(files, new Comparator<File>() {
                @Override public int compare(File a, File b) { return a.getName().compareTo(b.getName()); }
            });
            long total = 0;
            List<OutputFileItem> items = new ArrayList<>();
            for (File f : files) {
                total += f.length();
                items.add(new OutputFileItem(f, ValueTables.fileSizeText(f.length())));
            }
            lastOutputSizeText = ValueTables.fileSizeText(total);
            outputFiles = items;
            statusMessage = localize(R.string.status_success_folder,
                outputFormat.key, elapsedText, lastOutputSizeText, files.size(), dest.getName());
            if (openAfterConversion && !files.isEmpty()) previewRequest = files.get(0);
        } else {
            lastOutputFile = null;
            outputFiles = new ArrayList<>();
            statusMessage = localize(R.string.status_no_output);
        }
    }

    private static void collectFiles(File dir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File c : children) {
            if (c.isDirectory()) collectFiles(c, out);
            else out.add(c);
        }
    }

    // === UI actions ==========================================================

    public void setStatus(String msg) {
        lastStatusResId = null;
        statusMessage = msg;
        notifyChanged();
    }

    public void consumePreview() { previewRequest = null; }
    public void consumeShare() { shareRequest = null; }

    public void previewOne(File file) { previewRequest = file; notifyChanged(); }

    public void previewSource() {
        if (selectedFile != null) { previewRequest = selectedFile; notifyChanged(); }
    }

    /** Share the produced artifacts. A folder output shares its first file. */
    public void shareAllOutput() {
        if (outputFiles.size() == 1) shareRequest = outputFiles.get(0).file;
        else shareRequest = !outputFiles.isEmpty() ? outputFiles.get(0).file : lastOutputFile;
        notifyChanged();
    }

    public void openUrl(String url) {
        try {
            android.content.Intent intent = new android.content.Intent(
                android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url));
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            appContext.startActivity(intent);
        } catch (Throwable t) {
            statusMessage = localize(R.string.status_cannot_open, url);
            notifyChanged();
        }
    }

    // --- small helpers ------------------------------------------------------
    private static Integer parseIntOrNull(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
