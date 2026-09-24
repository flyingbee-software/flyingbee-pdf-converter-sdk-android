package com.flyingbee.FPPDFConverterDemoJava.ui.settings;

import android.content.Intent;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.flyingbee.FPPDFConverterDemoJava.Models.ThreadMode;
import com.flyingbee.FPPDFConverterDemoJava.R;
import com.flyingbee.FPPDFConverterDemoJava.ui.OcrLanguageActivity;
import com.flyingbee.FPPDFConverterDemoJava.ui.BaseScreenActivity;
import com.flyingbee.FPPDFConverterDemoJava.ui.Rows;

/**
 * SettingsActivity : the root output-settings screen (Kotlin SettingsScreen).
 * Conversion / General / OCR cards plus five per-format entry rows that open
 * their own sub-Activities.
 */
public final class SettingsActivity extends BaseScreenActivity {

    @Override
    protected int titleRes() {
        return R.string.settings_output;
    }

    @Override
    protected void render() {
        content.removeAllViews();

        // --- Conversion ---
        LinearLayout conv = Rows.section(this, content,
            getString(R.string.settings_conversion), getString(R.string.settings_conversion_footer));
        Rows.valueRow(this, conv, getString(R.string.settings_multi_threads),
            getString(controller.threadMode.displayNameRes), 0, true, 0, () -> {
                String[] labels = new String[ThreadMode.values().length];
                for (int i = 0; i < labels.length; i++) labels[i] = getString(ThreadMode.values()[i].displayNameRes);
                Rows.optionPicker(this, getString(R.string.settings_multi_threads), labels,
                    controller.threadMode.ordinal(), which -> {
                        controller.threadMode = ThreadMode.values()[which];
                        controller.commit();
                    });
            });
        if (controller.threadMode == ThreadMode.Custom) {
            Rows.textFieldRow(this, conv, getString(R.string.settings_thread_count),
                controller.customThreadCount, getString(R.string.settings_thread_count_placeholder), true,
                text -> { controller.customThreadCount = text; controller.saveSettings(); });
        } else {
            Rows.switchRow(this, conv, getString(R.string.settings_preview_after), controller.openAfterConversion,
                isOn -> { controller.openAfterConversion = isOn; controller.saveSettings(); });
        }

        // --- General ---
        LinearLayout gen = Rows.section(this, content,
            getString(R.string.settings_general), getString(R.string.settings_general_footer));
        Rows.valueRow(this, gen, getString(R.string.settings_image_dpi), controller.imageDPI + " DPI",
            0, true, 0, () -> {
                String[] labels = dpiLabels(controller.dpiValues, " DPI");
                int idx = Math.max(0, controller.dpiValues.indexOf(controller.imageDPI));
                Rows.optionPicker(this, getString(R.string.settings_image_dpi), labels, idx, which -> {
                    controller.imageDPI = controller.dpiValues.get(which);
                    controller.commit();
                });
            });
        Rows.valueRow(this, gen, getString(R.string.settings_image_quality),
            controller.qualityLabels().get(Labels.clamp(controller.imageQualityIndex, controller.qualityLabels().size())),
            0, true, 0, () -> {
                String[] labels = controller.qualityLabels().toArray(new String[0]);
                Rows.optionPicker(this, getString(R.string.settings_image_quality), labels,
                    Labels.clamp(controller.imageQualityIndex, labels.length), which -> {
                        controller.imageQualityIndex = which;
                        controller.commit();
                    });
            });

        // --- OCR ---
        LinearLayout ocr = Rows.section(this, content,
            getString(R.string.settings_ocr), getString(R.string.settings_ocr_footer));
        Rows.switchRow(this, ocr, getString(R.string.enable_ocr), controller.ocrEnabled,
            0, ContextCompat.getColor(this, R.color.danger), true,
            isOn -> { controller.ocrEnabled = isOn; controller.commit(); });
        Rows.valueRow(this, ocr, getString(R.string.settings_ocr_resolution), String.valueOf(controller.ocrDPI),
            0, controller.ocrEnabled, 0, () -> {
                String[] labels = new String[controller.ocrDpiValues.size()];
                for (int i = 0; i < labels.length; i++) labels[i] = String.valueOf(controller.ocrDpiValues.get(i));
                int idx = Math.max(0, controller.ocrDpiValues.indexOf(controller.ocrDPI));
                Rows.optionPicker(this, getString(R.string.settings_ocr_resolution), labels, idx, which -> {
                    controller.ocrDPI = controller.ocrDpiValues.get(which);
                    controller.commit();
                });
            });
        Rows.switchRow(this, ocr, getString(R.string.settings_ocr_scan), controller.ocrImageScan,
            0, 0, controller.ocrEnabled,
            isOn -> { controller.ocrImageScan = isOn; controller.saveSettings(); });
        Rows.valueRow(this, ocr, getString(R.string.languages),
            controller.ocrLanguageDisplay().isEmpty() ? getString(R.string.not_selected) : controller.ocrLanguageDisplay(),
            0, controller.ocrEnabled, ContextCompat.getColor(this, R.color.danger),
            () -> startActivity(new Intent(this, OcrLanguageActivity.class)));

        // --- Output formats ---
        LinearLayout fmt = Rows.section(this, content,
            getString(R.string.settings_output_formats), getString(R.string.settings_output_formats_footer));
        String[] outline = Labels.wordOutline(this);
        Rows.valueRow(this, fmt, "Word", "DOCX · " + outline[Labels.clamp(controller.wordOutlineType, 3)],
            R.drawable.ic_description, () -> startActivity(new Intent(this, WordSettingsActivity.class)));
        String[] xlFmt = Labels.excelFormat(this);
        Rows.valueRow(this, fmt, "Excel", "XLSX · CSV · " + xlFmt[Labels.clamp(controller.excelFormatOption, 3)],
            R.drawable.ic_grid_on, () -> startActivity(new Intent(this, ExcelSettingsActivity.class)));
        String[] htmlLay = Labels.htmlLayout(this);
        Rows.valueRow(this, fmt, "HTML", "HTML · " + htmlLay[Labels.clamp(controller.htmlLayoutMode, 2)],
            R.drawable.ic_html, () -> startActivity(new Intent(this, HtmlSettingsActivity.class)));
        Rows.valueRow(this, fmt, "Image", "JPEG · PNG · TIFF … · " + controller.imageOutputDPI + " DPI",
            R.drawable.ic_image, () -> startActivity(new Intent(this, ImageSettingsActivity.class)));
        Rows.valueRow(this, fmt, "Element",
            "XML · " + getString(controller.elementPackageZip ? R.string.element_zip_package : R.string.element_loose_files),
            R.drawable.ic_code, () -> startActivity(new Intent(this, ElementSettingsActivity.class)));
    }

    private static String[] dpiLabels(java.util.List<Integer> values, String suffix) {
        String[] out = new String[values.size()];
        for (int i = 0; i < out.length; i++) out[i] = values.get(i) + suffix;
        return out;
    }
}
