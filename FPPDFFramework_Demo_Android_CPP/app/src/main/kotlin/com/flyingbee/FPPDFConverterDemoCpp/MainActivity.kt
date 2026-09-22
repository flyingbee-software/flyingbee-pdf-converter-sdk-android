package com.flyingbee.FPPDFConverterDemoCpp

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File

// ---------------------------------------------------------------------------
// MainActivity : single-activity Compose host for the minimal C++ API demo.
// The whole point of this project is the native integration path (see
// app/build.gradle.kts and app/src/main/cpp/); the UI stays as small as
// possible: convert the bundled sample to DOCX or PNG and show the status.
// ---------------------------------------------------------------------------

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val controller: ConverterController = viewModel()
                    LaunchedEffect(Unit) { controller.startup() }

                    // Fires when the user taps a result file (or a future
                    // auto-open request); one-shot, consumed immediately.
                    LaunchedEffect(controller.previewRequest) {
                        controller.previewRequest?.let {
                            openForPreview(it)
                            controller.consumePreview()
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            // Edge-to-edge is enforced on Android 15+ (targetSdk
                            // 35+): keep all content clear of the status bar /
                            // display cutout at the top and the navigation bar
                            // at the bottom. This is the single inset handling
                            // point; the 24.dp below is pure content padding.
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("FPPDFFramework - C++ API Demo", style = MaterialTheme.typography.headlineSmall)
                        Text(controller.sdkVersionText, style = MaterialTheme.typography.bodyMedium)
                        Text("License: ${controller.licenseText}", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Sample: FPPDFSample.pdf (${controller.samplePageCount} pages)",
                            style = MaterialTheme.typography.bodyMedium,
                        )

                        Spacer(Modifier.height(8.dp))

                        // Stack the actions vertically: the labels ("Convert
                        // sample PDF to DOCX/PNG") are too long to share one
                        // row on narrow screens, so each button gets its own
                        // full-width line instead of truncating the text.
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Button(
                                onClick = { controller.convertSample("docx") },
                                enabled = !controller.isConverting,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(getString(R.string.convert_docx)) }
                            // OCR path for scanned / image-only PDFs; sits
                            // directly under the normal Word conversion button.
                            Button(
                                onClick = { controller.convertSample("docx", useOcr = true) },
                                enabled = !controller.isConverting,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(getString(R.string.convert_docx_ocr)) }
                            OutlinedButton(
                                onClick = { controller.convertSample("png") },
                                enabled = !controller.isConverting,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(getString(R.string.convert_png)) }
                        }
                        if (controller.isConverting) {
                            OutlinedButton(onClick = { controller.cancelConversion() }) {
                                Text(getString(R.string.cancel))
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            controller.statusMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            getString(R.string.output_dir, controller.outputDirLabel()),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )

                        // Result files: shown only after a successful
                        // conversion. Each row opens the file in an external
                        // viewer app (the Android equivalent of tapping a
                        // file in iOS Files / Quick Look).
                        if (controller.outputFiles.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider()
                            Text(
                                getString(R.string.output_files),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            controller.outputFiles.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { controller.previewOne(item.file) }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Text(
                                        item.file.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        item.sizeText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** Opens [file] in a viewer app via a FileProvider content Uri. Plain
     *  file:// Uris are banned since Android 7, and the read grant lets the
     *  viewer access our app-private output. */
    private fun openForPreview(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeFor(file.name))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (t: Throwable) {
            // No app handles this type, or the path is outside the
            // FileProvider configuration.
            Toast.makeText(
                this,
                getString(R.string.no_preview_app, file.name),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun mimeFor(name: String): String =
        when (name.substringAfterLast('.', "").lowercase()) {
            "pdf" -> "application/pdf"
            "docx" ->
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            else -> "*/*"
        }
}
