package com.flyingbee.FPPDFConverterDemoJava.ui.settings;

import android.widget.LinearLayout;

import com.flyingbee.FPPDFConverterDemoJava.Models.ImageFormat;
import com.flyingbee.FPPDFConverterDemoJava.R;
import com.flyingbee.FPPDFConverterDemoJava.ui.BaseScreenActivity;
import com.flyingbee.FPPDFConverterDemoJava.ui.Rows;

/** Image output options — Kotlin ImageSettingsScreen. */
public final class ImageSettingsActivity extends BaseScreenActivity {

    @Override
    protected int titleRes() {
        return R.string.settings_image;
    }

    @Override
    protected void render() {
        content.removeAllViews();

        LinearLayout format = Rows.section(this, content, getString(R.string.settings_image_format_section));
        Rows.valueRow(this, format, getString(R.string.settings_image_format), controller.imageFormat.displayName,
            () -> Rows.optionPicker(this, getString(R.string.settings_image_format), Labels.imageFormats(),
                controller.imageFormat.ordinal(), which -> {
                    controller.imageFormat = ImageFormat.values()[which];
                    controller.commit();
                }));

        LinearLayout rendering = Rows.section(this, content, getString(R.string.settings_image_rendering));
        Rows.valueRow(this, rendering, getString(R.string.settings_image_dpi_label),
            controller.imageOutputDPI + " DPI",
            () -> {
                String[] labels = new String[controller.dpiValues.size()];
                for (int i = 0; i < labels.length; i++) labels[i] = controller.dpiValues.get(i) + " DPI";
                int idx = Math.max(0, controller.dpiValues.indexOf(controller.imageOutputDPI));
                Rows.optionPicker(this, getString(R.string.settings_image_dpi_label), labels, idx, which -> {
                    controller.imageOutputDPI = controller.dpiValues.get(which);
                    controller.commit();
                });
            });
        Rows.valueRow(this, rendering, getString(R.string.settings_image_quality_label),
            qualityAt(controller.imageOutputQualityIndex),
            () -> {
                String[] labels = controller.qualityLabels().toArray(new String[0]);
                Rows.optionPicker(this, getString(R.string.settings_image_quality_label), labels,
                    Labels.clamp(controller.imageOutputQualityIndex, labels.length), which -> {
                        controller.imageOutputQualityIndex = which;
                        controller.commit();
                    });
            });
        Rows.switchRow(this, rendering, getString(R.string.settings_image_antialias), controller.imageAntiAlias,
            isOn -> { controller.imageAntiAlias = isOn; controller.saveSettings(); });

        LinearLayout output = Rows.section(this, content, getString(R.string.settings_image_output));
        Rows.switchRow(this, output, getString(R.string.settings_html_zip), controller.imagePackageZip,
            isOn -> { controller.imagePackageZip = isOn; controller.saveSettings(); });
    }

    private String qualityAt(int index) {
        java.util.List<String> labels = controller.qualityLabels();
        return labels.get(Labels.clamp(index, labels.size()));
    }
}
