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
// SamplePickerScreen : Android translation of SamplePickerViewController —
// one row per bundled PDF with "N pages · size" subtitle; tapping selects
// the sample and returns to the home screen.
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SamplePickerScreen(controller: ConverterController, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sample_pdfs)) },
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
            SectionCard(title = stringResource(R.string.bundled_samples_label)) {
                if (controller.samples.isEmpty()) {
                    Text(
                        stringResource(R.string.no_bundled_samples),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    controller.samples.forEach { sample ->
                        val pageSuffix = if (sample.pages == 1) stringResource(R.string.page_single_suffix) else stringResource(R.string.page_plural_suffix)
                        ValueRow(
                            title = sample.fileName,
                            value = stringResource(R.string.page_count_with_pages, sample.pages, pageSuffix, sample.sizeText),
                            enabled = !controller.isConverting,
                            onClick = {
                                controller.prepareSample(sample)
                                onBack()
                            },
                        )
                    }
                }
            }
        }
    }
}
