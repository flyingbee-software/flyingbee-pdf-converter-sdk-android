package com.flyingbee.FPPDFConverterDemoJava;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Enums and value objects shared by the controller and the UI. Java
 * counterpart of the Kotlin demo's Models.kt. Raw values mirror the iOS demo
 * so the three demos stay in lockstep.
 */
public final class Models {

    private Models() {}

    // --- OutputFormat -------------------------------------------------------
    public enum OutputFormat {
        Docx("docx", R.string.format_docx),
        Pptx("pptx", R.string.format_pptx),
        Xlsx("xlsx", R.string.format_xlsx),
        Csv("csv", R.string.format_csv),
        Txt("txt", R.string.format_txt),
        Html("html", R.string.format_html),
        Image("image", R.string.format_image),
        Element("element", R.string.format_element);

        public final String key;
        public final int displayNameRes;

        OutputFormat(String key, int res) {
            this.key = key;
            this.displayNameRes = res;
        }

        public static OutputFormat fromKey(String key) {
            for (OutputFormat f : values()) if (f.key.equals(key)) return f;
            return Docx;
        }
    }

    // --- ImageFormat (demo-local; same order as the SDK's) ------------------
    public enum ImageFormat {
        JPEG("JPEG", "jpeg"),
        PNG("PNG", "png"),
        BMP("BMP", "bmp"),
        GIF("GIF", "gif"),
        TIFF("TIFF", "tiff"),
        TGA("TGA", "tga"),
        JPEG2000("JPEG2000", "jp2");

        public final String displayName;
        public final String extension;

        ImageFormat(String displayName, String extension) {
            this.displayName = displayName;
            this.extension = extension;
        }

        public static ImageFormat fromOrdinal(int i) {
            ImageFormat[] v = values();
            return (i >= 0 && i < v.length) ? v[i] : PNG;
        }
    }

    // --- PageRangeMode ------------------------------------------------------
    public enum PageRangeMode {
        All(R.string.range_all),
        First10(R.string.range_first_10),
        First3(R.string.range_first_3),
        First1(R.string.range_first_1),
        Custom(R.string.range_customize);

        public final int displayNameRes;

        PageRangeMode(int res) { this.displayNameRes = res; }

        public static PageRangeMode fromOrdinal(int i) {
            PageRangeMode[] v = values();
            return (i >= 0 && i < v.length) ? v[i] : All;
        }
    }

    // --- ThreadMode ---------------------------------------------------------
    public enum ThreadMode {
        Auto(R.string.thread_auto),
        Two(R.string.thread_2),
        Five(R.string.thread_5),
        Ten(R.string.thread_10),
        Custom(R.string.thread_customize);

        public final int displayNameRes;

        ThreadMode(int res) { this.displayNameRes = res; }

        public static ThreadMode fromOrdinal(int i) {
            ThreadMode[] v = values();
            return (i >= 0 && i < v.length) ? v[i] : Auto;
        }
    }

    // --- Value objects ------------------------------------------------------
    public static final class OcrLanguage {
        public final String code;
        public final String displayName;

        public OcrLanguage(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
    }

    public static final class BundledSample {
        public final String assetName;
        public final String fileName;
        public final int pages;
        public final String sizeText;

        public BundledSample(String assetName, String fileName, int pages, String sizeText) {
            this.assetName = assetName;
            this.fileName = fileName;
            this.pages = pages;
            this.sizeText = sizeText;
        }
    }

    public static final class OutputFileItem {
        public final File file;
        public final String sizeText;

        public OutputFileItem(File file, String sizeText) {
            this.file = file;
            this.sizeText = sizeText;
        }
    }

    // --- Shared value tables ------------------------------------------------
    public static final class ValueTables {
        private ValueTables() {}

        public static final List<Integer> dpiValues = Arrays.asList(36, 72, 144, 300, 600, 1200);
        public static final List<Float> qualityValues = Arrays.asList(0.3f, 0.6f, 0.83f, 0.92f, 1.0f);
        public static final int[] qualityLabelRes = {
            R.string.quality_low, R.string.quality_medium, R.string.quality_good,
            R.string.quality_high, R.string.quality_best,
        };
        public static final List<Integer> wordDpiValues = Arrays.asList(72, 144, 300, 600);
        public static final List<Integer> ocrDpiValues = Arrays.asList(72, 144, 200, 300, 600);

        public static final List<OcrLanguage> ocrLanguages = Arrays.asList(
            new OcrLanguage("fra", "French"),
            new OcrLanguage("por", "Portuguese"),
            new OcrLanguage("kor", "Korean"),
            new OcrLanguage("deu", "German"),
            new OcrLanguage("nld", "Nederlands"),
            new OcrLanguage("jpn", "Japanese"),
            new OcrLanguage("ita", "Italian"),
            new OcrLanguage("swe", "Swedish"),
            new OcrLanguage("chi_sim", "Simplified Chinese"),
            new OcrLanguage("spa", "Spanish"),
            new OcrLanguage("pol", "Polski"),
            new OcrLanguage("chi_tra", "Traditional Chinese"),
            new OcrLanguage("rus", "Russian"),
            new OcrLanguage("tur", "Türkiye"),
            new OcrLanguage("ara", "Arabic"),
            new OcrLanguage("ukr", "Ukrainian"),
            new OcrLanguage("ind", "Indonesian"),
            new OcrLanguage("ces", "Czech"),
            new OcrLanguage("eng", "English"),
            new OcrLanguage("vie", "Vietnamese")
        );

        public static String fileSizeText(long bytes) {
            if (bytes <= 0) return "0 B";
            String[] units = {"B", "KB", "MB", "GB", "TB"};
            double v = bytes;
            int i = 0;
            while (v >= 1024.0 && i < units.length - 1) { v /= 1024.0; i++; }
            if (i == 0) return bytes + " B";
            return String.format(Locale.US, "%.1f %s", v, units[i]);
        }
    }
}
