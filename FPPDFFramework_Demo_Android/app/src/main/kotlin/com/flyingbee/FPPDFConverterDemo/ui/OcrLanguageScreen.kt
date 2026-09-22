package com.flyingbee.FPPDFConverterDemo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flyingbee.FPPDFConverterDemo.ConverterController
import com.flyingbee.FPPDFConverterDemo.R

// ---------------------------------------------------------------------------
// OcrLanguageScreen : Android translation of OCRLanguagePickerViewController —
// one switch per language, with the combined "eng+fra" string in the footer.
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrLanguageScreen(controller: ConverterController, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ocr_languages)) },
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
                .verticalScroll(rememberScrollState()),
        ) {
            SectionCard(
                title = stringResource(R.string.ocr_languages_section),
                footer = stringResource(
                    R.string.ocr_combined,
                    controller.ocrLanguageDisplay.ifEmpty { "—" },
                ),
            ) {
                controller.ocrLanguages.forEach { lang ->
                    MultiCheckRow(
                        label = "${lang.displayName}  (${lang.code})",
                        checked = controller.ocrSelectedLanguages.contains(lang.code),
                        onCheckedChange = { controller.toggleOcrLanguage(lang.code, it) },
                    )
                }
            }
            Text(
                stringResource(R.string.ocr_tip),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }
    }
}
