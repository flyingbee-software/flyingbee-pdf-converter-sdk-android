package com.flyingbee.FPPDFConverterDemoJava.ui.settings;

import android.widget.LinearLayout;

import com.flyingbee.FPPDFConverterDemoJava.R;
import com.flyingbee.FPPDFConverterDemoJava.ui.BaseScreenActivity;
import com.flyingbee.FPPDFConverterDemoJava.ui.Rows;

/** Word (docx) options — Kotlin WordSettingsScreen. */
public final class WordSettingsActivity extends BaseScreenActivity {

    @Override
    protected int titleRes() {
        return R.string.settings_word;
    }

    @Override
    protected void render() {
        content.removeAllViews();

        LinearLayout layout = Rows.section(this, content, getString(R.string.settings_word_layout));
        Rows.switchRow(this, layout, getString(R.string.settings_word_trim), controller.wordTrimBlankSpace,
            isOn -> { controller.wordTrimBlankSpace = isOn; controller.saveSettings(); });
        Rows.switchRow(this, layout, getString(R.string.settings_word_merge_paragraphs), controller.wordMergeParagraphs,
            isOn -> { controller.wordMergeParagraphs = isOn; controller.saveSettings(); });

        LinearLayout graphics = Rows.section(this, content, getString(R.string.settings_word_graphics));
        Rows.switchRow(this, graphics, getString(R.string.settings_word_shapes_to_image), controller.wordShapeToImage,
            isOn -> { controller.wordShapeToImage = isOn; controller.saveSettings(); });
        Rows.switchRow(this, graphics, getString(R.string.settings_word_merge_intersect), controller.wordMergeIntersectImages,
            isOn -> { controller.wordMergeIntersectImages = isOn; controller.saveSettings(); });
        Rows.valueRow(this, graphics, getString(R.string.settings_image_dpi), controller.wordImageDPI + " DPI",
            () -> {
                String[] labels = new String[controller.wordDpiValues.size()];
                for (int i = 0; i < labels.length; i++) labels[i] = controller.wordDpiValues.get(i) + " DPI";
                int idx = Math.max(0, controller.wordDpiValues.indexOf(controller.wordImageDPI));
                Rows.optionPicker(this, getString(R.string.settings_image_dpi), labels, idx, which -> {
                    controller.wordImageDPI = controller.wordDpiValues.get(which);
                    controller.commit();
                });
            });

        LinearLayout outline = Rows.section(this, content, getString(R.string.settings_word_outline));
        String[] labels = Labels.wordOutline(this);
        Rows.valueRow(this, outline, getString(R.string.settings_word_outline),
            labels[Labels.clamp(controller.wordOutlineType, labels.length)],
            () -> Rows.optionPicker(this, getString(R.string.settings_word_outline), labels,
                Labels.clamp(controller.wordOutlineType, labels.length), which -> {
                    controller.wordOutlineType = which;
                    controller.commit();
                }));
    }
}
