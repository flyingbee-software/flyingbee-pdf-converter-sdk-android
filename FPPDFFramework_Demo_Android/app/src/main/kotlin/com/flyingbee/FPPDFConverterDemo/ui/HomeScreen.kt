package com.flyingbee.FPPDFConverterDemo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flyingbee.FPPDFConverterDemo.ConverterController
import com.flyingbee.FPPDFConverterDemo.OutputFormat
import com.flyingbee.FPPDFConverterDemo.PageRangeMode
import com.flyingbee.FPPDFConverterDemo.R

// ---------------------------------------------------------------------------
// HomeScreen : the Android translation of MainViewController.m — grouped
// sections (Input / Output / Action / Result / SDK License / About) plus the
// two-line navigation title and the version footer block.
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    controller: ConverterController,
    onOpenSettings: () -> Unit,
    onOpenSamples: () -> Unit,
    onOpenOcrLanguages: () -> Unit,
    onPickPdf: () -> Unit,
) {
    var showFormatPicker by remember { mutableStateOf(false) }
    var showRangePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.app_name),
                                fontWeight = FontWeight.Bold,
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(
                                    text = stringResource(R.string.nav_title_kotlin_pill),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Text(
                            controller.sdkVersionText.ifEmpty { " " },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_output))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            // ================= Input =================
            SectionCard(title = stringResource(R.string.section_input)) {
                if (controller.selectedFile != null) {
                    val pages = controller.selectedPageCount()
                    val pageSuffix = if (pages == 1) stringResource(R.string.page_single_suffix) else stringResource(R.string.page_plural_suffix)
                    SelectedFileRow(
                        name = controller.sourceName,
                        meta = stringResource(R.string.page_count_with_pages, pages, pageSuffix, controller.selectedFileSizeText()),
                        onClick = { controller.previewSource() },
                    )
                } else {
                    Row(
                        Modifier.fillMaxWidth().height(64.dp).clickable(onClick = onPickPdf).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Description, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.no_file_selected),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                stringResource(R.string.no_file_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.Filled.PlayArrow, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                ValueRow(
                    stringResource(R.string.bundled_samples),
                    "",
                    icon = Icons.Outlined.Book,
                    onClick = onOpenSamples,
                )
                ValueRow(
                    stringResource(R.string.import_pdf),
                    "",
                    icon = Icons.Filled.Folder,
                    onClick = onPickPdf,
                )
            }

            // ================= Output =================
            SectionCard(title = stringResource(R.string.section_output)) {
                ValueRow(
                    title = stringResource(R.string.output_format),
                    value = stringResource(controller.outputFormat.displayNameRes),
                    icon = Icons.Outlined.Article,
                    onClick = { showFormatPicker = true },
                )
                ValueRow(
                    title = stringResource(R.string.page_range),
                    value = stringResource(controller.pageRangeMode.displayNameRes),
                    icon = Icons.Outlined.FormatListNumbered,
                    onClick = { showRangePicker = true },
                )
                if (controller.pageRangeMode == PageRangeMode.Custom) {
                    TextFieldRow(
                        label = stringResource(R.string.pages),
                        value = controller.customPageRange,
                        placeholder = stringResource(R.string.pages_placeholder),
                        onValueChange = { controller.customPageRange = it; controller.saveSettings() },
                    )
                }
                SwitchRow(
                    title = stringResource(R.string.enable_ocr),
                    checked = controller.ocrEnabled,
                    onCheckedChange = { controller.ocrEnabled = it; controller.saveSettings() },
                    icon = Icons.Outlined.PhotoCamera,
                    titleColor = Color(0xFFD93025),
                )
                ValueRow(
                    title = stringResource(R.string.languages),
                    value = controller.ocrLanguageDisplay.ifEmpty { stringResource(R.string.not_selected) },
                    icon = Icons.Outlined.Language,
                    enabled = controller.ocrEnabled,
                    onClick = onOpenOcrLanguages,
                )
            }

            // ================= Action =================
            SectionCard(title = stringResource(R.string.section_action)) {
                ActionRow(controller)
                StartStopRow(controller)
            }

            // ================= Result =================
            if (controller.outputFiles.isNotEmpty()) {
                SectionCard(title = stringResource(R.string.section_result)) {
                    controller.outputFiles.forEach { item ->
                        ValueRow(
                            title = item.file.name,
                            value = item.sizeText,
                            onClick = { controller.previewOne(item.file) },
                        )
                    }
                    ValueRow(
                        title = stringResource(R.string.share_save),
                        value = "",
                        icon = Icons.Filled.Share,
                        onClick = { controller.shareAllOutput() },
                    )
                }
            }

            // ================= SDK License =================
            SectionCard(title = stringResource(R.string.section_license)) {
                ValueRow(
                    stringResource(R.string.organization),
                    controller.licenseOrganization.ifEmpty { "—" },
                )
                ValueRow(
                    stringResource(R.string.expired_date),
                    controller.licenseExpiredDate.ifEmpty { "—" },
                )
                ValueRow(
                    stringResource(R.string.expired),
                    if (controller.licenseExpired) stringResource(R.string.yes) else stringResource(R.string.no),
                    valueColor = if (controller.licenseExpired) Color(0xFFD93025) else MaterialTheme.colorScheme.onSurface,
                )
            }

            // ================= About =================
            SectionCard(title = stringResource(R.string.section_about)) {
                ValueRow(
                    stringResource(R.string.contact_us),
                    "",
                    onClick = { controller.openUrl("https://www.flyingbee.com/contact-us") },
                )
                ValueRow(
                    stringResource(R.string.product_url),
                    "",
                    onClick = { controller.openUrl("https://www.flyingbee.com/pdf-sdk") },
                )
                ValueRow(
                    stringResource(R.string.github),
                    "",
                    onClick = { controller.openUrl("https://github.com/flyingbee-software/flyingbee-pdf-converter-sdk-ios") },
                )
                Text(
                    stringResource(R.string.about_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            // Footer version block (iOS footerInfoView)
            Column(
                Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.demo_title),
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.version_label, controller.appVersionName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.sdk_version_label, controller.sdkVersionText),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.copyright),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
            }
        }
    }

    if (showFormatPicker) {
        OptionPickerDialog(
            title = stringResource(R.string.output_format),
            options = OutputFormat.entries.map { stringResource(it.displayNameRes) },
            selectedIndex = OutputFormat.entries.indexOf(controller.outputFormat),
            onSelected = { controller.outputFormat = OutputFormat.entries[it]; controller.saveSettings() },
            onDismiss = { showFormatPicker = false },
        )
    }
    if (showRangePicker) {
        OptionPickerDialog(
            title = stringResource(R.string.page_range),
            options = PageRangeMode.entries.map { stringResource(it.displayNameRes) },
            selectedIndex = PageRangeMode.entries.indexOf(controller.pageRangeMode),
            onSelected = { controller.pageRangeMode = PageRangeMode.entries[it]; controller.saveSettings() },
            onDismiss = { showRangePicker = false },
        )
    }
}

// --- Input: selected-file card -------------------------------------------------

@Composable
private fun SelectedFileRow(name: String, meta: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(76.dp).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Card(
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.size(width = 44.dp, height = 56.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Description, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.tap_to_preview),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// --- Action: status + progress + Start/Stop ------------------------------------

@Composable
private fun ActionRow(controller: ConverterController) {
    val status = controller.statusMessage
    val color = when {
        status.startsWith("✅") -> Color(0xFF1B8E3C)
        status.startsWith("❌") || status.startsWith("⚠️") -> Color(0xFFE8710A)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        if (controller.isConverting) {
            LinearProgressIndicator(
                progress = { controller.progressValue.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp),
            )
            Spacer(Modifier.height(10.dp))
        }
        Text(status, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@Composable
private fun StartStopRow(controller: ConverterController) {
    val converting = controller.isConverting
    val canStart = controller.selectedFile != null
    Button(
        onClick = { if (converting) controller.stopConversion() else controller.startConversion() },
        enabled = converting || canStart,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (converting) Color(0xFFCC3333) else Color(0xFF2E9E4F),
            contentColor = Color.White,
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(52.dp),
    ) {
        Icon(if (converting) Icons.Filled.Stop else Icons.Filled.PlayArrow, null, Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (converting) stringResource(R.string.stop) else stringResource(R.string.start_conversion),
            fontWeight = FontWeight.SemiBold,
        )
    }
}
