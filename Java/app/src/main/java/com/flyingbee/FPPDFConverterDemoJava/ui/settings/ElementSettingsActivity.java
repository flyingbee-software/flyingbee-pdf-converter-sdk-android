package com.flyingbee.FPPDFConverterDemoJava.ui.settings;

import android.widget.LinearLayout;

import com.flyingbee.FPPDFConverterDemoJava.R;
import com.flyingbee.FPPDFConverterDemoJava.ui.BaseScreenActivity;
import com.flyingbee.FPPDFConverterDemoJava.ui.Rows;

/** Element (XML) output options — Kotlin ElementSettingsScreen. */
public final class ElementSettingsActivity extends BaseScreenActivity {

    @Override
    protected int titleRes() {
        return R.string.settings_element;
    }

    @Override
    protected void render() {
        content.removeAllViews();

        LinearLayout card = Rows.section(this, content, getString(R.string.settings_element_section));
        Rows.valueRow(this, card, getString(R.string.settings_image_quality_label), quality(),
            () -> {
                String[] labels = controller.qualityLabels().toArray(new String[0]);
                Rows.optionPicker(this, getString(R.string.settings_image_quality_label), labels,
                    Labels.clamp(controller.elementQualityIndex, labels.length), which -> {
                        controller.elementQualityIndex = which;
                        controller.commit();
                    });
            });
        Rows.switchRow(this, card, getString(R.string.settings_html_zip), controller.elementPackageZip,
            isOn -> { controller.elementPackageZip = isOn; controller.saveSettings(); });
    }

    private String quality() {
        java.util.List<String> labels = controller.qualityLabels();
        return labels.get(Labels.clamp(controller.elementQualityIndex, labels.size()));
    }
}
