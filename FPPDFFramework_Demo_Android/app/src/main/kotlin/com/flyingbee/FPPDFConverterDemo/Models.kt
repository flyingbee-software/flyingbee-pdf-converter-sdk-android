package com.flyingbee.FPPDFConverterDemo

import com.flyingbee.FPPDFConverterDemo.R

// ---------------------------------------------------------------------------
// Models.kt : enums and value objects shared by the controller and the UI.
// Raw values mirror the iOS demo (FPOutputFormat / FPImageFormat /
// FPPageRangeMode / FPThreadMode in ConverterController.h) so the two demos
// stay in lockstep.
// ---------------------------------------------------------------------------

enum class OutputFormat(val key: String, val displayNameRes: Int) {
    Docx("docx", R.string.format_docx),
    Pptx("pptx", R.string.format_pptx),
    Xlsx("xlsx", R.string.format_xlsx),
    Csv("csv", R.string.format_csv),
    Txt("txt", R.string.format_txt),
    Html("html", R.string.format_html),
    Image("image", R.string.format_image),
    Element("element", R.string.format_element);

    companion object {
        fun fromKey(key: String?): OutputFormat =
            entries.firstOrNull { it.key == key } ?: Docx
    }
}

enum class ImageFormat(val displayName: String, val extension: String) {
    JPEG("JPEG", "jpeg"),
    PNG("PNG", "png"),
    BMP("BMP", "bmp"),
    GIF("GIF", "gif"),
    TIFF("TIFF", "tiff"),
    TGA("TGA", "tga"),
    JPEG2000("JPEG2000", "jp2");

    companion object {
        fun fromOrdinal(i: Int): ImageFormat = entries.getOrElse(i) { PNG }
    }
}

enum class PageRangeMode(val displayNameRes: Int) {
    All(R.string.range_all),
    First10(R.string.range_first_10),
    First3(R.string.range_first_3),
    First1(R.string.range_first_1),
    Custom(R.string.range_customize);
}

enum class ThreadMode(val displayNameRes: Int) {
    Auto(R.string.thread_auto),
    Two(R.string.thread_2),
    Five(R.string.thread_5),
    Ten(R.string.thread_10),
    Custom(R.string.thread_customize);
}

data class Ocrlanguage(val code: String, val displayName: String)

data class BundledSample(val assetName: String, val fileName: String, val pages: Int, val sizeText: String)

data class OutputFileItem(val file: java.io.File, val sizeText: String)

object ValueTables {
    val dpiValues = listOf(36, 72, 144, 300, 600, 1200)
    val qualityValues = listOf(0.3f, 0.6f, 0.83f, 0.92f, 1.0f)
    val qualityLabelRes = listOf(R.string.quality_low, R.string.quality_medium, R.string.quality_good, R.string.quality_high, R.string.quality_best)
    val wordDpiValues = listOf(72, 144, 300, 600)
    val ocrDpiValues = listOf(72, 144, 200, 300, 600)

    val ocrLanguages = listOf(
        Ocrlanguage("fra", "French"),
        Ocrlanguage("por", "Portuguese"),
        Ocrlanguage("kor", "Korean"),
        Ocrlanguage("deu", "German"),
        Ocrlanguage("nld", "Nederlands"),
        Ocrlanguage("jpn", "Japanese"),
        Ocrlanguage("ita", "Italian"),
        Ocrlanguage("swe", "Swedish"),
        Ocrlanguage("chi_sim", "Simplified Chinese"),
        Ocrlanguage("spa", "Spanish"),
        Ocrlanguage("pol", "Polski"),
        Ocrlanguage("chi_tra", "Traditional Chinese"),
        Ocrlanguage("rus", "Russian"),
        Ocrlanguage("tur", "Türkiye"),
        Ocrlanguage("ara", "Arabic"),
        Ocrlanguage("ukr", "Ukrainian"),
        Ocrlanguage("ind", "Indonesian"),
        Ocrlanguage("ces", "Czech"),
        Ocrlanguage("eng", "English"),
        Ocrlanguage("vie", "Vietnamese"),
    )

    fun fileSizeText(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var v = bytes.toDouble()
        var i = 0
        while (v >= 1024.0 && i < units.size - 1) { v /= 1024.0; i++ }
        return if (i == 0) "${bytes} B" else String.format("%.1f %s", v, units[i])
    }
}
