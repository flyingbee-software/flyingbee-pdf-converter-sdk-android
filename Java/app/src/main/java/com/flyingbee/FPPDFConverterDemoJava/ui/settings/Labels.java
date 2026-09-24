package com.flyingbee.FPPDFConverterDemoJava.ui.settings;

import android.app.Activity;

import com.flyingbee.FPPDFConverterDemoJava.Models.ImageFormat;
import com.flyingbee.FPPDFConverterDemoJava.R;

/**
 * Labels : the localised picker option lists shared by the settings screens
 * (the Kotlin demo's private *Labels() composables). Each option maps 1:1 to
 * the index used by the controller state / SDK enums.
 */
final class Labels {

    private Labels() {}

    static String[] wordOutline(Activity a) {
        return new String[]{
            a.getString(R.string.picker_none),
            a.getString(R.string.picker_pdf_outline),
            a.getString(R.string.picker_detect_outline),
        };
    }

    static String[] excelFormat(Activity a) {
        return new String[]{
            a.getString(R.string.picker_keep_formatting),
            a.getString(R.string.picker_retain_structure),
            a.getString(R.string.picker_special_version),
        };
    }

    static String[] excelSeparator(Activity a) {
        return new String[]{
            a.getString(R.string.picker_auto),
            a.getString(R.string.picker_comma),
            a.getString(R.string.picker_dot),
            a.getString(R.string.picker_blank_comma),
            a.getString(R.string.picker_apostrophe_comma),
        };
    }

    static String[] excelOverlap(Activity a) {
        return new String[]{
            a.getString(R.string.picker_auto),
            a.getString(R.string.picker_merge),
            a.getString(R.string.picker_split),
        };
    }

    static String[] excelSheetStyle(Activity a) {
        return new String[]{
            a.getString(R.string.settings_excel_style_append_rows),
            a.getString(R.string.settings_excel_style_append_cols),
        };
    }

    static String[] htmlLayout(Activity a) {
        return new String[]{
            a.getString(R.string.settings_html_page_view),
            a.getString(R.string.settings_html_text_flow),
        };
    }

    static String[] htmlParagraph(Activity a) {
        return new String[]{
            a.getString(R.string.settings_html_blank_line),
            a.getString(R.string.settings_html_first_indent),
        };
    }

    static String[] htmlMerge(Activity a) {
        return new String[]{
            a.getString(R.string.picker_none),
            a.getString(R.string.picker_css_js),
            a.getString(R.string.picker_css_js_small),
            a.getString(R.string.picker_css_js_all),
        };
    }

    static String[] htmlNav(Activity a) {
        return new String[]{
            a.getString(R.string.picker_none),
            a.getString(R.string.picker_pdf_outline),
        };
    }

    static String[] imageFormats() {
        ImageFormat[] v = ImageFormat.values();
        String[] out = new String[v.length];
        for (int i = 0; i < v.length; i++) out[i] = v[i].displayName;
        return out;
    }

    /** Clamp an index into [0, len) so a stale persisted value can never crash a picker. */
    static int clamp(int i, int len) {
        return i < 0 ? 0 : (i >= len ? len - 1 : i);
    }
}
