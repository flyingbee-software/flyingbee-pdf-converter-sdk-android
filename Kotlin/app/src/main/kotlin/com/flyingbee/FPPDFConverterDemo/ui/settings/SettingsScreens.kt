package com.flyingbee.FPPDFConverterDemo.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Html
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.MaterialTheme
import com.flyingbee.FPPDFConverterDemo.ConverterController
import com.flyingbee.FPPDFConverterDemo.R
import com.flyingbee.FPPDFConverterDemo.ThreadMode
import com.flyingbee.FPPDFConverterDemo.ui.OptionPickerDialog
import com.flyingbee.FPPDFConverterDemo.ui.SectionCard
import com.flyingbee.FPPDFConverterDemo.ui.SwitchRow
import com.flyingbee.FPPDFConverterDemo.ui.TextFieldRow
import com.flyingbee.FPPDFConverterDemo.ui.ValueRow

// ---------------------------------------------------------------------------
// Settings screens : Android translation of SettingsViewController.m and its
// five format sub-pages (Word / Excel / HTML / Image / Element). The iOS
// "Debug — view SDK log" section is intentionally absent: on Android the SDK
// derives its log path from /proc/self/exe, which points at app_process
// inside an app, so no debug log is produced (see README).
// ---------------------------------------------------------------------------

/** Shared scaffold for every settings page. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            content = content,
        )
    }
}

// Localised picker option label lists. Each option in these lists maps 1:1
// to the corresponding index used by the SDK or the controller state.
@Composable
private fun wordOutlineLabels() = listOf(
    stringResource(R.string.picker_none),
    stringResource(R.string.picker_pdf_outline),
    stringResource(R.string.picker_detect_outline),
)

@Composable
private fun excelFormatLabels() = listOf(
    stringResource(R.string.picker_keep_formatting),
    stringResource(R.string.picker_retain_structure),
    stringResource(R.string.picker_special_version),
)

@Composable
private fun excelSeparatorLabels() = listOf(
    stringResource(R.string.picker_auto),
    stringResource(R.string.picker_comma),
    stringResource(R.string.picker_dot),
    stringResource(R.string.picker_blank_comma),
    stringResource(R.string.picker_apostrophe_comma),
)

@Composable
private fun excelOverlapLabels() = listOf(
    stringResource(R.string.picker_auto),
    stringResource(R.string.picker_merge),
    stringResource(R.string.picker_split),
)

@Composable
private fun excelSheetStyleLabels() = listOf(
    stringResource(R.string.settings_excel_style_append_rows),
    stringResource(R.string.settings_excel_style_append_cols),
)

@Composable
private fun htmlLayoutLabels() = listOf(
    stringResource(R.string.settings_html_page_view),
    stringResource(R.string.settings_html_text_flow),
)

@Composable
private fun htmlParagraphLabels() = listOf(
    stringResource(R.string.settings_html_blank_line),
    stringResource(R.string.settings_html_first_indent),
)

@Composable
private fun htmlMergeLabels() = listOf(
    stringResource(R.string.picker_none),
    stringResource(R.string.picker_css_js),
    stringResource(R.string.picker_css_js_small),
    stringResource(R.string.picker_css_js_all),
)

@Composable
private fun htmlNavLabels() = listOf(
    stringResource(R.string.picker_none),
    stringResource(R.string.picker_pdf_outline),
)

// ============================ Root =========================================

@Composable
fun SettingsScreen(
    controller: ConverterController,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    var picker by remember { mutableStateOf<String?>(null) }

    SettingsScaffold(
        title = stringResource(R.string.settings_output),
        onBack = { controller.saveSettings(); onBack() },
    ) {
        SectionCard(
            title = stringResource(R.string.settings_conversion),
            footer = stringResource(R.string.settings_conversion_footer),
        ) {
            ValueRow(
                stringResource(R.string.settings_multi_threads),
                stringResource(controller.threadMode.displayNameRes),
            ) { picker = "threads" }
            if (controller.threadMode == ThreadMode.Custom) {
                TextFieldRow(
                    label = stringResource(R.string.settings_thread_count),
                    value = controller.customThreadCount,
                    placeholder = stringResource(R.string.settings_thread_count_placeholder),
                    numeric = true,
                    onValueChange = { controller.customThreadCount = it; controller.saveSettings() },
                )
            } else {
                SwitchRow(
                    title = stringResource(R.string.settings_preview_after),
                    checked = controller.openAfterConversion,
                    onCheckedChange = { controller.openAfterConversion = it; controller.saveSettings() },
                )
            }
        }

        SectionCard(
            title = stringResource(R.string.settings_general),
            footer = stringResource(R.string.settings_general_footer),
        ) {
            ValueRow(
                stringResource(R.string.settings_image_dpi),
                "${controller.imageDPI} DPI",
            ) { picker = "imageDpi" }
            ValueRow(
                stringResource(R.string.settings_image_quality),
                controller.qualityLabels[controller.imageQualityIndex],
            ) { picker = "imageQuality" }
        }

        SectionCard(
            title = stringResource(R.string.settings_ocr),
            footer = stringResource(R.string.settings_ocr_footer),
        ) {
            SwitchRow(
                title = stringResource(R.string.enable_ocr),
                checked = controller.ocrEnabled,
                onCheckedChange = { controller.ocrEnabled = it; controller.saveSettings() },
                titleColor = androidx.compose.ui.graphics.Color(0xFFD93025),
            )
            ValueRow(
                stringResource(R.string.settings_ocr_resolution),
                "${controller.ocrDPI}",
                enabled = controller.ocrEnabled,
            ) { picker = "ocrDpi" }
            SwitchRow(
                title = stringResource(R.string.settings_ocr_scan),
                checked = controller.ocrImageScan,
                onCheckedChange = { controller.ocrImageScan = it; controller.saveSettings() },
                enabled = controller.ocrEnabled,
            )
            ValueRow(
                stringResource(R.string.languages),
                controller.ocrLanguageDisplay.ifEmpty { stringResource(R.string.not_selected) },
                enabled = controller.ocrEnabled,
                valueColor = androidx.compose.ui.graphics.Color(0xFFD93025),
            ) { onOpen("ocr") }
        }

        SectionCard(
            title = stringResource(R.string.settings_output_formats),
            footer = stringResource(R.string.settings_output_formats_footer),
        ) {
            WordEntry(controller, onOpen)
            ExcelEntry(controller, onOpen)
            HtmlEntry(controller, onOpen)
            ImageEntry(controller, onOpen)
            ElementEntry(controller, onOpen)
        }

    }

    when (picker) {
        "threads" -> OptionPickerDialog(
            stringResource(R.string.settings_multi_threads),
            ThreadMode.entries.map { stringResource(it.displayNameRes) },
            ThreadMode.entries.indexOf(controller.threadMode),
            onSelected = { controller.threadMode = ThreadMode.entries[it]; controller.saveSettings() },
            onDismiss = { picker = null },
        )
        "imageDpi" -> OptionPickerDialog(
            stringResource(R.string.settings_image_dpi),
            controller.dpiValues.map { "$it DPI" },
            controller.dpiValues.indexOf(controller.imageDPI).coerceAtLeast(0),
            onSelected = { controller.imageDPI = controller.dpiValues[it]; controller.saveSettings() },
            onDismiss = { picker = null },
        )
        "imageQuality" -> OptionPickerDialog(
            stringResource(R.string.settings_image_quality),
            controller.qualityLabels,
            controller.imageQualityIndex,
            onSelected = { controller.imageQualityIndex = it; controller.saveSettings() },
            onDismiss = { picker = null },
        )
        "ocrDpi" -> OptionPickerDialog(
            stringResource(R.string.settings_ocr_resolution),
            controller.ocrDpiValues.map { it.toString() },
            controller.ocrDpiValues.indexOf(controller.ocrDPI).coerceAtLeast(0),
            onSelected = { controller.ocrDPI = controller.ocrDpiValues[it]; controller.saveSettings() },
            onDismiss = { picker = null },
        )
    }
}

@Composable
private fun WordEntry(c: ConverterController, onOpen: (String) -> Unit) {
    val labels = wordOutlineLabels()
    ValueRow(
        "Word",
        "DOCX · ${labels[c.wordOutlineType.coerceIn(0, 2)]}",
        icon = Icons.Outlined.Description,
    ) { onOpen("settings/word") }
}

@Composable
private fun ExcelEntry(c: ConverterController, onOpen: (String) -> Unit) {
    val labels = excelFormatLabels()
    ValueRow(
        "Excel",
        "XLSX · CSV · ${labels[c.excelFormatOption.coerceIn(0, 2)]}",
        icon = Icons.Outlined.GridOn,
    ) { onOpen("settings/excel") }
}

@Composable
private fun HtmlEntry(c: ConverterController, onOpen: (String) -> Unit) {
    val labels = htmlLayoutLabels()
    ValueRow(
        "HTML",
        "HTML · ${labels[c.htmlLayoutMode.coerceIn(0, 1)]}",
        icon = Icons.Outlined.Html,
    ) { onOpen("settings/html") }
}

@Composable
private fun ImageEntry(c: ConverterController, onOpen: (String) -> Unit) {
    ValueRow(
        "Image",
        "JPEG · PNG · TIFF … · ${c.imageOutputDPI} DPI",
        icon = Icons.Outlined.Image,
    ) { onOpen("settings/image") }
}

@Composable
private fun ElementEntry(c: ConverterController, onOpen: (String) -> Unit) {
    ValueRow(
        "Element",
        "XML · " + if (c.elementPackageZip) stringResource(R.string.element_zip_package) else stringResource(R.string.element_loose_files),
        icon = Icons.Outlined.Code,
    ) { onOpen("settings/element") }
}

// ---------------------------------------------------------------------------
// Language picker dialog
// ---------------------------------------------------------------------------

// ============================ Word =========================================

@Composable
fun WordSettingsScreen(controller: ConverterController, onBack: () -> Unit) {
    var picker by remember { mutableStateOf<String?>(null) }
    SettingsScaffold(
        title = stringResource(R.string.settings_word),
        onBack = { controller.saveSettings(); onBack() },
    ) {
        SectionCard(title = stringResource(R.string.settings_word_layout)) {
            SwitchRow(
                stringResource(R.string.settings_word_trim),
                controller.wordTrimBlankSpace,
            ) { controller.wordTrimBlankSpace = it; controller.saveSettings() }
            SwitchRow(
                stringResource(R.string.settings_word_merge_paragraphs),
                controller.wordMergeParagraphs,
            ) { controller.wordMergeParagraphs = it; controller.saveSettings() }
        }
        SectionCard(title = stringResource(R.string.settings_word_graphics)) {
            SwitchRow(
                stringResource(R.string.settings_word_shapes_to_image),
                controller.wordShapeToImage,
            ) { controller.wordShapeToImage = it; controller.saveSettings() }
            SwitchRow(
                stringResource(R.string.settings_word_merge_intersect),
                controller.wordMergeIntersectImages,
            ) { controller.wordMergeIntersectImages = it; controller.saveSettings() }
            ValueRow(
                stringResource(R.string.settings_image_dpi),
                "${controller.wordImageDPI} DPI",
            ) { picker = "dpi" }
        }
        SectionCard(title = stringResource(R.string.settings_word_outline)) {
            val labels = wordOutlineLabels()
            ValueRow(
                stringResource(R.string.settings_word_outline),
                labels[controller.wordOutlineType.coerceIn(0, 2)],
            ) { picker = "outline" }
        }
    }
    when (picker) {
        "dpi" -> OptionPickerDialog(
            stringResource(R.string.settings_image_dpi),
            controller.wordDpiValues.map { "$it DPI" },
            controller.wordDpiValues.indexOf(controller.wordImageDPI).coerceAtLeast(0),
            onSelected = { controller.wordImageDPI = controller.wordDpiValues[it]; controller.saveSettings() },
            onDismiss = { picker = null },
        )
        "outline" -> {
            val labels = wordOutlineLabels()
            OptionPickerDialog(
                stringResource(R.string.settings_word_outline),
                labels,
                controller.wordOutlineType,
                onSelected = { controller.wordOutlineType = it; controller.saveSettings() },
                onDismiss = { picker = null },
            )
        }
    }
}

// ============================ Excel ========================================

@Composable
fun ExcelSettingsScreen(controller: ConverterController, onBack: () -> Unit) {
    var picker by remember { mutableStateOf<String?>(null) }
    SettingsScaffold(
        title = stringResource(R.string.settings_excel),
        onBack = { controller.saveSettings(); onBack() },
    ) {
        SectionCard(title = stringResource(R.string.settings_excel_sheet)) {
            SwitchRow(
                stringResource(R.string.settings_excel_all_in_one),
                controller.excelAllInOneSheet,
            ) { controller.excelAllInOneSheet = it; controller.saveSettings() }
            val sheetLabels = excelSheetStyleLabels()
            ValueRow(
                stringResource(R.string.settings_excel_sheet_style),
                sheetLabels[controller.excelAllInOneStyle.coerceIn(0, 1)],
            ) { picker = "style" }
            SwitchRow(
                stringResource(R.string.settings_excel_recognize_numbers),
                controller.excelRecognizeNumber,
            ) { controller.excelRecognizeNumber = it; controller.saveSettings() }
        }
        SectionCard(title = stringResource(R.string.settings_excel_data)) {
            val formatLabels = excelFormatLabels()
            ValueRow(
                stringResource(R.string.settings_excel_format),
                formatLabels[controller.excelFormatOption.coerceIn(0, 2)],
            ) { picker = "format" }
            val sepLabels = excelSeparatorLabels()
            ValueRow(
                stringResource(R.string.settings_excel_thousand),
                sepLabels[controller.excelThousandSeparator.coerceIn(0, 4)],
            ) { picker = "sep" }
            val overlapLabels = excelOverlapLabels()
            ValueRow(
                stringResource(R.string.settings_excel_overlap),
                overlapLabels[controller.excelOverlapText.coerceIn(0, 2)],
            ) { picker = "overlap" }
        }
        SectionCard(title = stringResource(R.string.settings_excel_csv)) {
            SwitchRow(
                stringResource(R.string.settings_excel_csv_zip),
                controller.csvPackageZip,
            ) { controller.csvPackageZip = it; controller.saveSettings() }
        }
    }
    when (picker) {
        "style" -> {
            val labels = excelSheetStyleLabels()
            OptionPickerDialog(
                stringResource(R.string.settings_excel_sheet_style),
                labels,
                controller.excelAllInOneStyle,
                onSelected = { controller.excelAllInOneStyle = it; controller.saveSettings() },
                onDismiss = { picker = null },
            )
        }
        "format" -> {
            val labels = excelFormatLabels()
            OptionPickerDialog(
                stringResource(R.string.settings_excel_format),
                labels,
                controller.excelFormatOption,
                onSelected = { controller.excelFormatOption = it; controller.saveSettings() },
                onDismiss = { picker = null },
            )
        }
        "sep" -> {
            val labels = excelSeparatorLabels()
            OptionPickerDialog(
                stringResource(R.string.settings_excel_thousand),
                labels,
                controller.excelThousandSeparator,
                onSelected = { controller.excelThousandSeparator = it; controller.saveSettings() },
                onDismiss = { picker = null },
            )
        }
        "overlap" -> {
            val labels = excelOverlapLabels()
            OptionPickerDialog(
                stringResource(R.string.settings_excel_overlap),
                labels,
                controller.excelOverlapText,
                onSelected = { controller.excelOverlapText = it; controller.saveSettings() },
                onDismiss = { picker = null },
            )
        }
    }
}

// ============================ HTML =========================================

@Composable
fun HtmlSettingsScreen(controller: ConverterController, onBack: () -> Unit) {
    var picker by remember { mutableStateOf<String?>(null) }
    SettingsScaffold(
        title = stringResource(R.string.settings_html),
        onBack = { controller.saveSettings(); onBack() },
    ) {
        SectionCard(
            title = stringResource(R.string.settings_html_layout),
            footer = stringResource(R.string.settings_html_layout_footer),
        ) {
            val layoutLabels = htmlLayoutLabels()
            ValueRow(
                stringResource(R.string.settings_html_layout_mode),
                layoutLabels[controller.htmlLayoutMode.coerceIn(0, 1)],
            ) { picker = "layout" }
            val paragraphLabels = htmlParagraphLabels()
            ValueRow(
                stringResource(R.string.settings_html_paragraph),
                paragraphLabels[controller.htmlTextFlowParagraph.coerceIn(0, 1)],
                enabled = controller.htmlLayoutMode == 1,
            ) { picker = "paragraph" }
        }
        SectionCard(title = stringResource(R.string.settings_html_output)) {
            val mergeLabels = htmlMergeLabels()
            ValueRow(
                stringResource(R.string.settings_html_merge),
                mergeLabels[controller.htmlMergeResource.coerceIn(0, 3)],
            ) { picker = "merge" }
            val navLabels = htmlNavLabels()
            ValueRow(
                stringResource(R.string.settings_html_nav),
                navLabels[controller.htmlNavigationBar.coerceIn(0, 1)],
            ) { picker = "nav" }
            SwitchRow(
                stringResource(R.string.settings_html_zip),
                controller.htmlPackageZip,
            ) { controller.htmlPackageZip = it; controller.saveSettings() }
        }
    }
    when (picker) {
        "layout" -> {
            val labels = htmlLayoutLabels()
            OptionPickerDialog(
                stringResource(R.string.settings_html_layout_mode),
                labels,
                controller.htmlLayoutMode,
                onSelected = { controller.htmlLayoutMode = it; controller.saveSettings() },
                onDismiss = { picker = null },
            )
        }
        "paragraph" -> {
            val labels = htmlParagraphLabels()
            OptionPickerDialog(
                stringResource(R.string.settings_html_paragraph),
                labels,
                controller.htmlTextFlowParagraph,
                onSelected = { controller.htmlTextFlowParagraph = it; controller.saveSettings() },
                onDismiss = { picker = null },
            )
        }
        "merge" -> {
            val labels = htmlMergeLabels()
            OptionPickerDialog(
                stringResource(R.string.settings_html_merge),
                labels,
                controller.htmlMergeResource,
                onSelected = { controller.htmlMergeResource = it; controller.saveSettings() },
                onDismiss = { picker = null },
            )
        }
        "nav" -> {
            val labels = htmlNavLabels()
            OptionPickerDialog(
                stringResource(R.string.settings_html_nav),
                labels,
                controller.htmlNavigationBar,
                onSelected = { controller.htmlNavigationBar = it; controller.saveSettings() },
                onDismiss = { picker = null },
            )
        }
    }
}

// ============================ Image ========================================

@Composable
fun ImageSettingsScreen(controller: ConverterController, onBack: () -> Unit) {
    var picker by remember { mutableStateOf<String?>(null) }
    SettingsScaffold(
        title = stringResource(R.string.settings_image),
        onBack = { controller.saveSettings(); onBack() },
    ) {
        SectionCard(title = stringResource(R.string.settings_image_format_section)) {
            ValueRow(
                stringResource(R.string.settings_image_format),
                controller.imageFormat.displayName,
            ) { picker = "format" }
        }
        SectionCard(title = stringResource(R.string.settings_image_rendering)) {
            ValueRow(
                stringResource(R.string.settings_image_dpi_label),
                "${controller.imageOutputDPI} DPI",
            ) { picker = "dpi" }
            ValueRow(
                stringResource(R.string.settings_image_quality_label),
                controller.qualityLabels[controller.imageOutputQualityIndex],
            ) { picker = "quality" }
            SwitchRow(
                stringResource(R.string.settings_image_antialias),
                controller.imageAntiAlias,
            ) { controller.imageAntiAlias = it; controller.saveSettings() }
        }
        SectionCard(title = stringResource(R.string.settings_image_output)) {
            SwitchRow(
                stringResource(R.string.settings_html_zip),
                controller.imagePackageZip,
            ) { controller.imagePackageZip = it; controller.saveSettings() }
        }
    }
    when (picker) {
        "format" -> OptionPickerDialog(
            stringResource(R.string.settings_image_format),
            com.flyingbee.FPPDFConverterDemo.ImageFormat.entries.map { it.displayName },
            com.flyingbee.FPPDFConverterDemo.ImageFormat.entries.indexOf(controller.imageFormat),
            onSelected = { controller.imageFormat = com.flyingbee.FPPDFConverterDemo.ImageFormat.entries[it]; controller.saveSettings() },
            onDismiss = { picker = null },
        )
        "dpi" -> OptionPickerDialog(
            stringResource(R.string.settings_image_dpi_label),
            controller.dpiValues.map { "$it DPI" },
            controller.dpiValues.indexOf(controller.imageOutputDPI).coerceAtLeast(0),
            onSelected = { controller.imageOutputDPI = controller.dpiValues[it]; controller.saveSettings() },
            onDismiss = { picker = null },
        )
        "quality" -> OptionPickerDialog(
            stringResource(R.string.settings_image_quality_label),
            controller.qualityLabels,
            controller.imageOutputQualityIndex,
            onSelected = { controller.imageOutputQualityIndex = it; controller.saveSettings() },
            onDismiss = { picker = null },
        )
    }
}

// ============================ Element ======================================

@Composable
fun ElementSettingsScreen(controller: ConverterController, onBack: () -> Unit) {
    var picker by remember { mutableStateOf<String?>(null) }
    SettingsScaffold(
        title = stringResource(R.string.settings_element),
        onBack = { controller.saveSettings(); onBack() },
    ) {
        SectionCard(title = stringResource(R.string.settings_element_section)) {
            ValueRow(
                stringResource(R.string.settings_image_quality_label),
                controller.qualityLabels[controller.elementQualityIndex],
            ) { picker = "quality" }
            SwitchRow(
                stringResource(R.string.settings_html_zip),
                controller.elementPackageZip,
            ) { controller.elementPackageZip = it; controller.saveSettings() }
        }
    }
    when (picker) {
        "quality" -> OptionPickerDialog(
            stringResource(R.string.settings_image_quality_label),
            controller.qualityLabels,
            controller.elementQualityIndex,
            onSelected = { controller.elementQualityIndex = it; controller.saveSettings() },
            onDismiss = { picker = null },
        )
    }
}
