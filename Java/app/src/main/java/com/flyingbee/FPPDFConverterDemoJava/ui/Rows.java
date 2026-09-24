package com.flyingbee.FPPDFConverterDemoJava.ui;

import android.app.Activity;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.flyingbee.FPPDFConverterDemoJava.R;

/**
 * Rows : the imperative counterpart of the Kotlin demo's Common.kt composables
 * (SectionCard / ValueRow / SwitchRow / TextFieldRow / MultiCheckRow /
 * OptionPickerDialog). Screens inflate the shared item layouts and fill them
 * through these helpers, so every screen looks identical to the Compose UI.
 */
public final class Rows {

    private Rows() {}

    /** Callbacks used by the row builders below. */
    public interface Click { void onClick(); }
    public interface Toggle { void onToggle(boolean isOn); }
    public interface TextEdit { void onText(String value); }

    // --- SectionCard ---------------------------------------------------------

    /**
     * Adds a titled grouped card (header + rounded body + optional footer) to
     * {@code parent} and returns the body container rows should go into.
     */
    public static LinearLayout section(Activity act, LinearLayout parent, String title, String footer) {
        View v = LayoutInflater.from(act).inflate(R.layout.view_section, parent, false);
        ((TextView) v.findViewById(R.id.section_title)).setText(title);
        TextView ft = v.findViewById(R.id.section_footer);
        if (footer != null && !footer.isEmpty()) {
            ft.setText(footer);
            ft.setVisibility(View.VISIBLE);
        }
        LinearLayout body = v.findViewById(R.id.section_body);
        parent.addView(v);
        return body;
    }

    /** Convenience overload: section without footer. */
    public static LinearLayout section(Activity act, LinearLayout parent, String title) {
        return section(act, parent, title, null);
    }

    // --- ValueRow ------------------------------------------------------------

    /** iconRes may be 0; valueColor 0 means the default onSurfaceVariant. */
    public static void valueRow(Activity act, LinearLayout parent, String title, String value,
                                int iconRes, boolean enabled, int valueColor, Click onClick) {
        View row = LayoutInflater.from(act).inflate(R.layout.item_value_row, parent, false);
        ImageView icon = row.findViewById(R.id.row_icon);
        if (iconRes != 0) {
            icon.setImageResource(iconRes);
            icon.setVisibility(View.VISIBLE);
        }
        ((TextView) row.findViewById(R.id.row_title)).setText(title);
        TextView val = row.findViewById(R.id.row_value);
        val.setText(value);
        if (valueColor != 0) val.setTextColor(valueColor);

        if (onClick != null) {
            row.findViewById(R.id.row_chevron).setVisibility(View.VISIBLE);
            row.setOnClickListener(v -> onClick.onClick());
        } else {
            row.setClickable(false);
            row.setBackgroundResource(0);
        }
        row.setAlpha(enabled ? 1f : 0.45f);
        row.setEnabled(enabled);
        parent.addView(row);
    }

    public static void valueRow(Activity act, LinearLayout parent, String title, String value, Click onClick) {
        valueRow(act, parent, title, value, 0, true, 0, onClick);
    }

    /** Non-clickable display-only row (no chevron). */
    public static void valueRow(Activity act, LinearLayout parent, String title, String value) {
        valueRow(act, parent, title, value, 0, true, 0, null);
    }

    /** Non-clickable display-only row with an explicit value colour. */
    public static void valueRow(Activity act, LinearLayout parent, String title, String value, int valueColor) {
        valueRow(act, parent, title, value, 0, true, valueColor, null);
    }

    public static void valueRow(Activity act, LinearLayout parent, String title, String value,
                                int iconRes, Click onClick) {
        valueRow(act, parent, title, value, iconRes, true, 0, onClick);
    }

    // --- SwitchRow -----------------------------------------------------------

    public static void switchRow(Activity act, LinearLayout parent, String title, boolean checked,
                                 int iconRes, int titleColor, boolean enabled, Toggle onChange) {
        View row = LayoutInflater.from(act).inflate(R.layout.item_switch_row, parent, false);
        ImageView icon = row.findViewById(R.id.row_icon);
        if (iconRes != 0) {
            icon.setImageResource(iconRes);
            icon.setVisibility(View.VISIBLE);
        }
        TextView t = row.findViewById(R.id.row_title);
        t.setText(title);
        if (titleColor != 0) t.setTextColor(titleColor);

        MaterialSwitch sw = row.findViewById(R.id.row_switch);
        sw.setChecked(checked);
        sw.setEnabled(enabled);
        sw.setOnCheckedChangeListener((b, isOn) -> onChange.onToggle(isOn));
        row.setAlpha(enabled ? 1f : 0.45f);
        parent.addView(row);
    }

    public static void switchRow(Activity act, LinearLayout parent, String title, boolean checked, Toggle onChange) {
        switchRow(act, parent, title, checked, 0, 0, true, onChange);
    }

    // --- TextFieldRow --------------------------------------------------------

    public static void textFieldRow(Activity act, LinearLayout parent, String label, String value,
                                    String placeholder, boolean numeric, TextEdit onChange) {
        View row = LayoutInflater.from(act).inflate(R.layout.item_text_field_row, parent, false);
        ((TextView) row.findViewById(R.id.row_label)).setText(label);
        TextInputLayout layout = row.findViewById(R.id.row_input_layout);
        if (placeholder != null) layout.setHint(placeholder);
        TextInputEditText edit = row.findViewById(R.id.row_input);
        edit.setInputType(numeric ? InputType.TYPE_CLASS_NUMBER : InputType.TYPE_CLASS_TEXT);
        // Avoid the cursor jumping while typing: only set the text when it
        // actually differs from what is on screen.
        if (!value.contentEquals(edit.getText())) edit.setText(value);
        edit.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                onChange.onText(s.toString());
            }
        });
        parent.addView(row);
    }

    // --- Selected file / no file / check rows --------------------------------

    public static void selectedFileRow(Activity act, LinearLayout parent, String name, String meta, Click onClick) {
        View row = LayoutInflater.from(act).inflate(R.layout.item_selected_file, parent, false);
        ((TextView) row.findViewById(R.id.file_name)).setText(name);
        ((TextView) row.findViewById(R.id.file_meta)).setText(meta);
        if (onClick != null) row.setOnClickListener(v -> onClick.onClick());
        parent.addView(row);
    }

    public static void noFileRow(Activity act, LinearLayout parent, Click onClick) {
        View row = LayoutInflater.from(act).inflate(R.layout.item_no_file_row, parent, false);
        if (onClick != null) row.setOnClickListener(v -> onClick.onClick());
        parent.addView(row);
    }

    public static void checkRow(Activity act, LinearLayout parent, String label, boolean checked, Toggle onChange) {
        View row = LayoutInflater.from(act).inflate(R.layout.item_check_row, parent, false);
        ((TextView) row.findViewById(R.id.row_label)).setText(label);
        com.google.android.material.checkbox.MaterialCheckBox cb = row.findViewById(R.id.row_check);
        cb.setChecked(checked);
        row.setOnClickListener(v -> {
            boolean next = !cb.isChecked();
            cb.setChecked(next);
            onChange.onToggle(next);
        });
        parent.addView(row);
    }

    // --- OptionPickerDialog ----------------------------------------------------

    /** Single-choice picker — the Android form of the iOS action sheet. */
    public static void optionPicker(Activity act, String title, String[] options,
                                    int selectedIndex, IntClick onSelected) {
        new AlertDialog.Builder(act)
            .setTitle(title)
            .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                dialog.dismiss();
                onSelected.onPick(which);
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    public interface IntClick { void onPick(int index); }

    // --- Safe area (status bar / notch / navigation bar) ---------------------

    /**
     * Edge-to-edge: adds the top window inset (status bar + display cutout /
     * notch) as padding to {@code view}, so a coloured top bar keeps its
     * background behind the status bar while its content stays below the notch.
     */
    public static void applyTopInset(View view) {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.systemBars()
                    | androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, v.getPaddingBottom());
            return insets;
        });
    }

    /** Keeps scrollable content clear of the navigation bar / gesture inset. */
    public static void applyBottomInset(View view) {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.systemBars()
                    | androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, v.getPaddingTop(), bars.right, bars.bottom);
            return insets;
        });
    }

    /** A plain text block styled like the Compose bodySmall footer text. */
    public static TextView bodyText(Activity act, LinearLayout parent, String text,
                                    int horizontalPaddingDp, int verticalPaddingDp) {
        TextView tv = new TextView(act);
        tv.setText(text);
        tv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
        tv.setTextColor(ContextCompat.getColor(act, R.color.on_surface_variant));
        float density = act.getResources().getDisplayMetrics().density;
        tv.setPadding((int) (horizontalPaddingDp * density), (int) (verticalPaddingDp * density),
            (int) (horizontalPaddingDp * density), (int) (verticalPaddingDp * density));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        parent.addView(tv, lp);
        return tv;
    }
}
