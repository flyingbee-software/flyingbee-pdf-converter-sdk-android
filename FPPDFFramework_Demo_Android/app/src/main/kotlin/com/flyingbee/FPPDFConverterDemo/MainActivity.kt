package com.flyingbee.FPPDFConverterDemo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.flyingbee.FPPDFConverterDemo.ui.HomeScreen
import com.flyingbee.FPPDFConverterDemo.ui.OcrLanguageScreen
import com.flyingbee.FPPDFConverterDemo.ui.PasswordDialog
import com.flyingbee.FPPDFConverterDemo.ui.SamplePickerScreen
import com.flyingbee.FPPDFConverterDemo.ui.settings.ElementSettingsScreen
import com.flyingbee.FPPDFConverterDemo.ui.settings.ExcelSettingsScreen
import com.flyingbee.FPPDFConverterDemo.ui.settings.HtmlSettingsScreen
import com.flyingbee.FPPDFConverterDemo.ui.settings.ImageSettingsScreen
import com.flyingbee.FPPDFConverterDemo.ui.settings.SettingsScreen
import com.flyingbee.FPPDFConverterDemo.ui.settings.WordSettingsScreen
import java.io.File

// ---------------------------------------------------------------------------
// MainActivity : single-activity Compose host. Wires the system file picker,
// "Open in…" incoming intents, Quick Look-style preview and the share sheet
// to the shared ConverterController (Android counterpart of the iOS
// AppDelegate + MainViewController navigation glue).
// ---------------------------------------------------------------------------

class MainActivity : AppCompatActivity() {

    private lateinit var controller: ConverterController

    // SAF document picker limited to PDFs.
    private val pickPdf = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> if (uri != null) controller.importPickedPdf(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            com.flyingbee.FPPDFConverterDemo.ui.FPPDFDemoTheme {
            // viewModel() resolves the controller inside the Compose graph so
            // it survives configuration changes.
            controller = viewModel()
            // Feed the controller this Activity's (locale-aware) configuration;
            // the Application context is not reconfigured by
            // AppCompatDelegate.setApplicationLocales on all API levels.
            controller.attachUiContext(this@MainActivity)

            val navController = rememberNavController()

            // Cold-start "Open in" (VIEW/SEND with a PDF).
            LaunchedEffect(Unit) {
                handleIncomingIntent(intent)
                controller.startup()
            }

            // One-shot requests published by the controller.
            LaunchedEffect(controller.previewRequest) {
                controller.previewRequest?.let {
                    openForPreview(it)
                    controller.consumePreview()
                }
            }
            LaunchedEffect(controller.shareRequest) {
                controller.shareRequest?.let {
                    shareFile(it)
                    controller.consumeShare()
                }
            }

            NavHost(navController = navController, startDestination = "home") {
                composable("home") {
                    HomeScreen(
                        controller = controller,
                        onOpenSettings = { navController.navigate("settings") },
                        onOpenSamples = { navController.navigate("samples") },
                        onOpenOcrLanguages = { navController.navigate("ocr") },
                        onPickPdf = { pickPdf.launch(arrayOf("application/pdf")) },
                    )
                }
                composable("samples") { SamplePickerScreen(controller) { navController.popBackStack() } }
                composable("ocr") { OcrLanguageScreen(controller) { navController.popBackStack() } }
                composable("settings") {
                    SettingsScreen(
                        controller = controller,
                        onBack = { navController.popBackStack() },
                        onOpen = { route -> navController.navigate(route) },
                    )
                }
                composable("settings/word") { WordSettingsScreen(controller) { navController.popBackStack() } }
                composable("settings/excel") { ExcelSettingsScreen(controller) { navController.popBackStack() } }
                composable("settings/html") { HtmlSettingsScreen(controller) { navController.popBackStack() } }
                composable("settings/image") { ImageSettingsScreen(controller) { navController.popBackStack() } }
                composable("settings/element") { ElementSettingsScreen(controller) { navController.popBackStack() } }
            }

            // Encrypted-PDF password prompt (mirrors iOS showPasswordPromptForURL).
            val prompt = controller.passwordPromptFile
            if (prompt != null) {
                PasswordDialog(
                    fileName = prompt.name,
                    onCancel = { controller.clearPasswordPrompt() },
                    onSubmit = { pw ->
                        if (controller.pdfPasswordValid(prompt, pw)) {
                            controller.pdfPassword = pw
                            controller.saveSettings()
                            controller.clearPasswordPrompt()
                        } else {
                            controller.setStatus(getString(R.string.status_wrong_password))
                            controller.clearPasswordPrompt()
                        }
                    },
                )
            }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    /** Accepts a PDF handed over by another app (ACTION_VIEW / ACTION_SEND). */
    private fun handleIncomingIntent(intent: Intent?) {
        val uri: Uri = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> intent.getParcelableExtraCompat(Intent.EXTRA_STREAM)
            else -> null
        } ?: return
        controller.importPickedPdf(uri)
    }

    private fun Intent.getParcelableExtraCompat(name: String): Uri? {
        return if (android.os.Build.VERSION.SDK_INT >= 33)
            getParcelableExtra(name, Uri::class.java)
        else @Suppress("DEPRECATION") getParcelableExtra(name)
    }

    /** "Quick Look" equivalent: open the produced file with a viewer app. */
    private fun openForPreview(file: File) {
        val uri = uriFor(file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeFor(file.name))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (t: Throwable) {
            controller.setStatus(getString(R.string.status_no_preview_app, file.name))
        }
    }

    /** Share sheet equivalent (iOS UIActivityViewController). */
    private fun shareFile(file: File) {
        val uri = uriFor(file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeFor(file.name)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(Intent.createChooser(send, getString(R.string.share_save)))
        } catch (t: Throwable) {
            controller.setStatus(getString(R.string.status_share_unavailable, t.message ?: "unknown"))
        }
    }

    private fun uriFor(file: File): Uri =
        FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

    private fun mimeFor(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "pdf" -> "application/pdf"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "html", "htm" -> "text/html"
        "txt", "csv", "xml" -> "text/plain"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        "bmp" -> "image/bmp"
        "tif", "tiff" -> "image/tiff"
        "zip" -> "application/zip"
        else -> "application/octet-stream"
    }
}
