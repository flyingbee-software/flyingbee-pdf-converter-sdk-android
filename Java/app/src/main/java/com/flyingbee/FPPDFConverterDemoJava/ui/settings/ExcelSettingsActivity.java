package com.flyingbee.FPPDFConverterDemoJava.ui.settings;

import android.widget.LinearLayout;

import com.flyingbee.FPPDFConverterDemoJava.R;
import com.flyingbee.FPPDFConverterDemoJava.ui.BaseScreenActivity;
import com.flyingbee.FPPDFConverterDemoJava.ui.Rows;

/** Excel (xlsx / csv) options — Kotlin ExcelSettingsScreen. */
public final class ExcelSettingsActivity extends BaseScreenActivity {

    @Override
    protected int titleRes() {
        return R.string.settings_excel;
    }

    @Override
    protected void render() {
        content.removeAllViews();

        LinearLayout sheet = Rows.section(this, content, getString(R.string.settings_excel_sheet));
        Rows.switchRow(this, sheet, getString(R.string.settings_excel_all_in_one), controller.excelAllInOneSheet,
            isOn -> { controller.excelAllInOneSheet = isOn; controller.saveSettings(); });
        String[] styleLabels = Labels.excelSheetStyle(this);
        Rows.valueRow(this, sheet, getString(R.string.settings_excel_sheet_style),
            styleLabels[Labels.clamp(controller.excelAllInOneStyle, 2)],
            () -> Rows.optionPicker(this, getString(R.string.settings_excel_sheet_style), styleLabels,
                Labels.clamp(controller.excelAllInOneStyle, 2), which -> {
                    controller.excelAllInOneStyle = which;
                    controller.commit();
                }));
        Rows.switchRow(this, sheet, getString(R.string.settings_excel_recognize_numbers), controller.excelRecognizeNumber,
            isOn -> { controller.excelRecognizeNumber = isOn; controller.saveSettings(); });

        LinearLayout data = Rows.section(this, content, getString(R.string.settings_excel_data));
        String[] formatLabels = Labels.excelFormat(this);
        Rows.valueRow(this, data, getString(R.string.settings_excel_format),
            formatLabels[Labels.clamp(controller.excelFormatOption, 3)],
            () -> Rows.optionPicker(this, getString(R.string.settings_excel_format), formatLabels,
                Labels.clamp(controller.excelFormatOption, 3), which -> {
                    controller.excelFormatOption = which;
                    controller.commit();
                }));
        String[] sepLabels = Labels.excelSeparator(this);
        Rows.valueRow(this, data, getString(R.string.settings_excel_thousand),
            sepLabels[Labels.clamp(controller.excelThousandSeparator, 5)],
            () -> Rows.optionPicker(this, getString(R.string.settings_excel_thousand), sepLabels,
                Labels.clamp(controller.excelThousandSeparator, 5), which -> {
                    controller.excelThousandSeparator = which;
                    controller.commit();
                }));
        String[] overlapLabels = Labels.excelOverlap(this);
        Rows.valueRow(this, data, getString(R.string.settings_excel_overlap),
            overlapLabels[Labels.clamp(controller.excelOverlapText, 3)],
            () -> Rows.optionPicker(this, getString(R.string.settings_excel_overlap), overlapLabels,
                Labels.clamp(controller.excelOverlapText, 3), which -> {
                    controller.excelOverlapText = which;
                    controller.commit();
                }));

        LinearLayout csv = Rows.section(this, content, getString(R.string.settings_excel_csv));
        Rows.switchRow(this, csv, getString(R.string.settings_excel_csv_zip), controller.csvPackageZip,
            isOn -> { controller.csvPackageZip = isOn; controller.saveSettings(); });
    }
}
