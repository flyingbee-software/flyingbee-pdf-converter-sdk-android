package com.flyingbee.FPPDFConverterDemoJava.ui;

import android.widget.LinearLayout;

import com.flyingbee.FPPDFConverterDemoJava.Models.BundledSample;
import com.flyingbee.FPPDFConverterDemoJava.R;

/**
 * SamplePickerActivity : one row per bundled PDF with an "N pages · size"
 * subtitle; tapping selects the sample and returns to Home (Kotlin
 * SamplePickerScreen.kt).
 */
public final class SamplePickerActivity extends BaseScreenActivity {

    @Override
    protected int titleRes() {
        return R.string.sample_pdfs;
    }

    @Override
    protected void render() {
        content.removeAllViews();
        LinearLayout card = Rows.section(this, content, getString(R.string.bundled_samples_label));
        if (controller.samples.isEmpty()) {
            Rows.bodyText(this, card, getString(R.string.no_bundled_samples), 16, 16);
        } else {
            for (BundledSample sample : controller.samples) {
                String suffix = getString(sample.pages == 1
                    ? R.string.page_single_suffix : R.string.page_plural_suffix);
                final BundledSample s = sample;
                Rows.valueRow(this, card, s.fileName,
                    getString(R.string.page_count_with_pages, s.pages, suffix, s.sizeText),
                    0, !controller.isConverting, 0,
                    () -> {
                        controller.prepareSample(s);
                        finish();
                    });
            }
        }
    }
}
