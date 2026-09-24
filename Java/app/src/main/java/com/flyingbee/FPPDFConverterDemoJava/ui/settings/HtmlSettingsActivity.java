package com.flyingbee.FPPDFConverterDemoJava.ui.settings;

import android.widget.LinearLayout;

import com.flyingbee.FPPDFConverterDemoJava.R;
import com.flyingbee.FPPDFConverterDemoJava.ui.BaseScreenActivity;
import com.flyingbee.FPPDFConverterDemoJava.ui.Rows;

/** HTML options — Kotlin HtmlSettingsScreen. */
public final class HtmlSettingsActivity extends BaseScreenActivity {

    @Override
    protected int titleRes() {
        return R.string.settings_html;
    }

    @Override
    protected void render() {
        content.removeAllViews();

        LinearLayout layout = Rows.section(this, content,
            getString(R.string.settings_html_layout), getString(R.string.settings_html_layout_footer));
        String[] layoutLabels = Labels.htmlLayout(this);
        Rows.valueRow(this, layout, getString(R.string.settings_html_layout_mode),
            layoutLabels[Labels.clamp(controller.htmlLayoutMode, 2)],
            () -> Rows.optionPicker(this, getString(R.string.settings_html_layout_mode), layoutLabels,
                Labels.clamp(controller.htmlLayoutMode, 2), which -> {
                    controller.htmlLayoutMode = which;
                    controller.commit();
                }));
        String[] paragraphLabels = Labels.htmlParagraph(this);
        Rows.valueRow(this, layout, getString(R.string.settings_html_paragraph),
            paragraphLabels[Labels.clamp(controller.htmlTextFlowParagraph, 2)],
            0, controller.htmlLayoutMode == 1, 0,
            () -> Rows.optionPicker(this, getString(R.string.settings_html_paragraph), paragraphLabels,
                Labels.clamp(controller.htmlTextFlowParagraph, 2), which -> {
                    controller.htmlTextFlowParagraph = which;
                    controller.commit();
                }));

        LinearLayout output = Rows.section(this, content, getString(R.string.settings_html_output));
        String[] mergeLabels = Labels.htmlMerge(this);
        Rows.valueRow(this, output, getString(R.string.settings_html_merge),
            mergeLabels[Labels.clamp(controller.htmlMergeResource, 4)],
            () -> Rows.optionPicker(this, getString(R.string.settings_html_merge), mergeLabels,
                Labels.clamp(controller.htmlMergeResource, 4), which -> {
                    controller.htmlMergeResource = which;
                    controller.commit();
                }));
        String[] navLabels = Labels.htmlNav(this);
        Rows.valueRow(this, output, getString(R.string.settings_html_nav),
            navLabels[Labels.clamp(controller.htmlNavigationBar, 2)],
            () -> Rows.optionPicker(this, getString(R.string.settings_html_nav), navLabels,
                Labels.clamp(controller.htmlNavigationBar, 2), which -> {
                    controller.htmlNavigationBar = which;
                    controller.commit();
                }));
        Rows.switchRow(this, output, getString(R.string.settings_html_zip), controller.htmlPackageZip,
            isOn -> { controller.htmlPackageZip = isOn; controller.saveSettings(); });
    }
}
