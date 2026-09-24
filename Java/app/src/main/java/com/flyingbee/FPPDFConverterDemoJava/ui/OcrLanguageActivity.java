package com.flyingbee.FPPDFConverterDemoJava.ui;

import android.widget.LinearLayout;

import com.flyingbee.FPPDFConverterDemoJava.Models.OcrLanguage;
import com.flyingbee.FPPDFConverterDemoJava.R;

/**
 * OcrLanguageActivity : one checkbox per language with the combined
 * "eng+fra" string in the footer (Kotlin OcrLanguageScreen.kt).
 */
public final class OcrLanguageActivity extends BaseScreenActivity {

    @Override
    protected int titleRes() {
        return R.string.ocr_languages;
    }

    @Override
    protected void render() {
        content.removeAllViews();
        String combined = controller.ocrLanguageDisplay();
        LinearLayout card = Rows.section(this, content,
            getString(R.string.ocr_languages_section),
            getString(R.string.ocr_combined, combined.isEmpty() ? "—" : combined));
        for (OcrLanguage lang : controller.ocrLanguages) {
            final String code = lang.code;
            Rows.checkRow(this, card, lang.displayName + "  (" + lang.code + ")",
                controller.ocrSelectedLanguages.contains(code),
                isOn -> controller.toggleOcrLanguage(code, isOn));
        }
        Rows.bodyText(this, content, getString(R.string.ocr_tip), 24, 8);
    }
}
