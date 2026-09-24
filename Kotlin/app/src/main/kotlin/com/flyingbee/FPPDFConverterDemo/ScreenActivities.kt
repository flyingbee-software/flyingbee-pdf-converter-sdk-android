package com.flyingbee.FPPDFConverterDemo

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import com.flyingbee.FPPDFConverterDemo.ui.FPPDFDemoTheme
import com.flyingbee.FPPDFConverterDemo.ui.OcrLanguageScreen
import com.flyingbee.FPPDFConverterDemo.ui.SamplePickerScreen
import com.flyingbee.FPPDFConverterDemo.ui.settings.ElementSettingsScreen
import com.flyingbee.FPPDFConverterDemo.ui.settings.ExcelSettingsScreen
import com.flyingbee.FPPDFConverterDemo.ui.settings.HtmlSettingsScreen
import com.flyingbee.FPPDFConverterDemo.ui.settings.ImageSettingsScreen
import com.flyingbee.FPPDFConverterDemo.ui.settings.SettingsScreen
import com.flyingbee.FPPDFConverterDemo.ui.settings.WordSettingsScreen

// ---------------------------------------------------------------------------
// Multi-Activity navigation (aligned with the Java demo): every screen is its
// own Activity. A child screen slides in via the platform's default Activity
// transition and pops with the system back gesture / toolbar back button,
// exactly like startActivity() + finish() in the Java version. Each Activity
// hosts the unchanged Compose screen and reads the shared process-wide
// ConverterController singleton.
// ---------------------------------------------------------------------------

/** Common plumbing for the sub-screens: edge-to-edge, locale attach, theme. */
abstract class ComposeScreenActivity : AppCompatActivity() {

    protected val controller: ConverterController get() = ConverterController.shared

    /** The screen body, already wrapped in the demo theme by the base class. */
    @Composable
    abstract fun Content(controller: ConverterController)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep the status-bar icons dark over the white top bar (parity with
        // the Java demo's SystemBarStyle.light and MainActivity).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(0, 0),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
        )
        controller.attachUiContext(this)
        setContent {
            FPPDFDemoTheme { Content(controller) }
        }
    }
}

class SamplePickerActivity : ComposeScreenActivity() {
    @Composable
    override fun Content(controller: ConverterController) {
        SamplePickerScreen(controller) { finish() }
    }
}

class OcrLanguageActivity : ComposeScreenActivity() {
    @Composable
    override fun Content(controller: ConverterController) {
        OcrLanguageScreen(controller) { finish() }
    }
}

/** Root settings screen; its onOpen routes map onto the child Activities. */
class SettingsActivity : ComposeScreenActivity() {
    @Composable
    override fun Content(controller: ConverterController) {
        SettingsScreen(
            controller = controller,
            onBack = { finish() },
            onOpen = { route -> openRoute(route) },
        )
    }

    private fun openRoute(route: String) {
        val cls = when (route) {
            "ocr" -> OcrLanguageActivity::class.java
            "settings/word" -> WordSettingsActivity::class.java
            "settings/excel" -> ExcelSettingsActivity::class.java
            "settings/html" -> HtmlSettingsActivity::class.java
            "settings/image" -> ImageSettingsActivity::class.java
            "settings/element" -> ElementSettingsActivity::class.java
            else -> null
        } ?: return
        startActivity(Intent(this, cls))
    }
}

class WordSettingsActivity : ComposeScreenActivity() {
    @Composable
    override fun Content(controller: ConverterController) {
        WordSettingsScreen(controller) { finish() }
    }
}

class ExcelSettingsActivity : ComposeScreenActivity() {
    @Composable
    override fun Content(controller: ConverterController) {
        ExcelSettingsScreen(controller) { finish() }
    }
}

class HtmlSettingsActivity : ComposeScreenActivity() {
    @Composable
    override fun Content(controller: ConverterController) {
        HtmlSettingsScreen(controller) { finish() }
    }
}

class ImageSettingsActivity : ComposeScreenActivity() {
    @Composable
    override fun Content(controller: ConverterController) {
        ImageSettingsScreen(controller) { finish() }
    }
}

class ElementSettingsActivity : ComposeScreenActivity() {
    @Composable
    override fun Content(controller: ConverterController) {
        ElementSettingsScreen(controller) { finish() }
    }
}
