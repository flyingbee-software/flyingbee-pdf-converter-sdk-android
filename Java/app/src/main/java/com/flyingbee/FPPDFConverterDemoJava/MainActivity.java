package com.flyingbee.FPPDFConverterDemoJava;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import com.flyingbee.FPPDFConverterDemoJava.Models.OutputFormat;
import com.flyingbee.FPPDFConverterDemoJava.Models.OutputFileItem;
import com.flyingbee.FPPDFConverterDemoJava.Models.PageRangeMode;
import com.flyingbee.FPPDFConverterDemoJava.ui.OcrLanguageActivity;
import com.flyingbee.FPPDFConverterDemoJava.ui.Rows;
import com.flyingbee.FPPDFConverterDemoJava.ui.SamplePickerActivity;
import com.flyingbee.FPPDFConverterDemoJava.ui.settings.SettingsActivity;

import java.io.File;
import java.util.Locale;

/**
 * MainActivity : the Home screen (Java counterpart of HomeScreen.kt). Rebuilds
 * the whole section stack on every state change — the imperative equivalent of
 * a Compose recomposition — and wires the SAF picker, incoming VIEW/SEND
 * intents, file preview, the share sheet and the encrypted-PDF password prompt.
 */
public final class MainActivity extends AppCompatActivity implements ConverterController.StateListener {

    private ConverterController controller;
    private LinearLayout sections;
    private TextView topbarSdkVersion;
    private TextView footerVersion;
    private TextView footerSdkVersion;

    // SAF document picker limited to PDFs.
    private final ActivityResultLauncher<String[]> pickPdf =
        registerForActivityResult(new ActivityResultContracts.OpenDocument(),
            uri -> { if (uri != null) controller.importPickedPdf(uri); });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // The demo is light-themed, so keep the status-bar icons dark over the
        // white top bar regardless of the system dark-mode setting.
        androidx.activity.EdgeToEdge.enable(this,
            androidx.activity.SystemBarStyle.light(0, 0),
            androidx.activity.SystemBarStyle.light(0, 0));
        setContentView(R.layout.activity_home);
        controller = ConverterController.get();

        // Safe area: the top bar clears the status bar / notch, the scroll
        // content clears the navigation bar.
        Rows.applyTopInset(findViewById(R.id.home_topbar));
        Rows.applyBottomInset(findViewById(R.id.home_scroll));

        sections = findViewById(R.id.home_sections);
        topbarSdkVersion = findViewById(R.id.topbar_sdk_version);
        footerVersion = findViewById(R.id.footer_version);
        footerSdkVersion = findViewById(R.id.footer_sdk_version);
        ((ImageButton) findViewById(R.id.topbar_settings))
            .setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        handleIncomingIntent(getIntent());
        controller.startup();
    }

    @Override
    protected void onResume() {
        super.onResume();
        controller.setListener(this);
        render();
    }

    @Override
    protected void onPause() {
        super.onPause();
        controller.clearListener(this);
    }

    @Override
    public void onStateChanged() {
        render();
        drainRequests();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIncomingIntent(intent);
    }

    // -----------------------------------------------------------------------

    /** Accepts a PDF handed over by another app (ACTION_VIEW / ACTION_SEND). */
    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;
        Uri uri = null;
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            uri = intent.getData();
        } else if (Intent.ACTION_SEND.equals(intent.getAction())) {
            if (Build.VERSION.SDK_INT >= 33) uri = intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri.class);
            else uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        }
        if (uri != null) controller.importPickedPdf(uri);
    }

    /** One-shot requests published by the controller (preview / share / password). */
    private void drainRequests() {
        if (controller.previewRequest != null) {
            File f = controller.previewRequest;
            controller.consumePreview();
            openForPreview(f);
        }
        if (controller.shareRequest != null) {
            File f = controller.shareRequest;
            controller.consumeShare();
            shareFile(f);
        }
        if (controller.passwordPromptFile != null) showPasswordDialog(controller.passwordPromptFile);
    }

    // === Full re-render ======================================================

    private void render() {
        topbarSdkVersion.setText(controller.sdkVersionText.isEmpty() ? " " : controller.sdkVersionText);
        footerVersion.setText(getString(R.string.version_label, controller.getAppVersionName()));
        footerSdkVersion.setText(getString(R.string.sdk_version_label, controller.sdkVersionText));

        sections.removeAllViews();
        renderInput();
        renderOutput();
        renderAction();
        if (!controller.outputFiles.isEmpty()) renderResult();
        renderLicense();
        renderAbout();
        drainRequests();
    }

    // --- INPUT ---
    private void renderInput() {
        LinearLayout card = Rows.section(this, sections, getString(R.string.section_input));
        if (controller.selectedFile != null) {
            int pages = controller.selectedPageCount();
            String suffix = getString(pages == 1 ? R.string.page_single_suffix : R.string.page_plural_suffix);
            Rows.selectedFileRow(this, card, controller.sourceName,
                getString(R.string.page_count_with_pages, pages, suffix, controller.selectedFileSizeText()),
                () -> controller.previewSource());
        } else {
            Rows.noFileRow(this, card, this::launchPdfPicker);
        }
        Rows.valueRow(this, card, getString(R.string.bundled_samples), "", R.drawable.ic_book,
            () -> startActivity(new Intent(this, SamplePickerActivity.class)));
        Rows.valueRow(this, card, getString(R.string.import_pdf), "", R.drawable.ic_folder,
            this::launchPdfPicker);
    }

    private void launchPdfPicker() {
        pickPdf.launch(new String[]{"application/pdf"});
    }

    // --- OUTPUT ---
    private void renderOutput() {
        LinearLayout card = Rows.section(this, sections, getString(R.string.section_output));
        Rows.valueRow(this, card, getString(R.string.output_format),
            getString(controller.outputFormat.displayNameRes), R.drawable.ic_article,
            () -> {
                String[] labels = new String[OutputFormat.values().length];
                for (int i = 0; i < labels.length; i++) labels[i] = getString(OutputFormat.values()[i].displayNameRes);
                Rows.optionPicker(this, getString(R.string.output_format), labels,
                    controller.outputFormat.ordinal(), which -> {
                        controller.outputFormat = OutputFormat.values()[which];
                        controller.commit();
                    });
            });
        Rows.valueRow(this, card, getString(R.string.page_range),
            getString(controller.pageRangeMode.displayNameRes), R.drawable.ic_list_numbered,
            () -> {
                String[] labels = new String[PageRangeMode.values().length];
                for (int i = 0; i < labels.length; i++) labels[i] = getString(PageRangeMode.values()[i].displayNameRes);
                Rows.optionPicker(this, getString(R.string.page_range), labels,
                    controller.pageRangeMode.ordinal(), which -> {
                        controller.pageRangeMode = PageRangeMode.values()[which];
                        controller.commit();
                    });
            });
        if (controller.pageRangeMode == PageRangeMode.Custom) {
            Rows.textFieldRow(this, card, getString(R.string.pages), controller.customPageRange,
                getString(R.string.pages_placeholder), false,
                text -> { controller.customPageRange = text; controller.saveSettings(); });
        }
        Rows.switchRow(this, card, getString(R.string.enable_ocr), controller.ocrEnabled,
            R.drawable.ic_photo_camera, ContextCompat.getColor(this, R.color.danger), true,
            isOn -> { controller.ocrEnabled = isOn; controller.commit(); });
        Rows.valueRow(this, card, getString(R.string.languages),
            controller.ocrLanguageDisplay().isEmpty() ? getString(R.string.not_selected) : controller.ocrLanguageDisplay(),
            R.drawable.ic_language, controller.ocrEnabled, 0,
            () -> startActivity(new Intent(this, OcrLanguageActivity.class)));
    }

    // --- ACTION ---
    private void renderAction() {
        LinearLayout card = Rows.section(this, sections, getString(R.string.section_action));

        // ActionRow: optional progress bar + coloured status text.
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        float d = getResources().getDisplayMetrics().density;
        box.setPadding((int) (16 * d), (int) (12 * d), (int) (16 * d), (int) (12 * d));
        card.addView(box);
        if (controller.isConverting) {
            LinearProgressIndicator pb = new LinearProgressIndicator(this);
            pb.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (4 * d)));
            int v = Math.round(Math.max(0f, Math.min(1f, controller.progressValue)) * 1000f);
            pb.setMin(0);
            pb.setMax(1000);
            pb.setProgress(v);
            box.addView(pb);
            box.addView(spacer((int) (10 * d)));
        }
        String status = controller.statusMessage;
        int statusColor;
        if (status.startsWith("✅")) statusColor = ContextCompat.getColor(this, R.color.status_success);
        else if (status.startsWith("❌") || status.startsWith("⚠")) statusColor = ContextCompat.getColor(this, R.color.status_warning);
        else statusColor = ContextCompat.getColor(this, R.color.on_surface_variant);
        Rows.bodyText(this, box, status, 0, 0).setTextColor(statusColor);

        // StartStopRow: full-width coloured button.
        final boolean converting = controller.isConverting;
        MaterialButton btn = new MaterialButton(this);
        btn.setText(converting ? R.string.stop : R.string.start_conversion);
        btn.setIconResource(converting ? R.drawable.ic_stop : R.drawable.ic_play);
        btn.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(this, converting ? R.color.btn_stop : R.color.btn_start)));
        btn.setTextColor(ContextCompat.getColor(this, R.color.white));
        btn.setEnabled(converting || controller.selectedFile != null);
        btn.setOnClickListener(v -> {
            if (controller.isConverting) controller.stopConversion();
            else controller.startConversion();
        });
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (int) (52 * d));
        blp.setMargins((int) (16 * d), (int) (8 * d), (int) (16 * d), (int) (8 * d));
        card.addView(btn, blp);
    }

    private android.view.View spacer(int heightPx) {
        android.view.View v = new android.view.View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, heightPx));
        return v;
    }

    // --- RESULT ---
    private void renderResult() {
        LinearLayout card = Rows.section(this, sections, getString(R.string.section_result));
        for (OutputFileItem item : controller.outputFiles) {
            final File f = item.file;
            Rows.valueRow(this, card, f.getName(), item.sizeText, () -> controller.previewOne(f));
        }
        Rows.valueRow(this, card, getString(R.string.share_save), "", R.drawable.ic_share,
            () -> controller.shareAllOutput());
    }

    // --- SDK LICENSE ---
    private void renderLicense() {
        LinearLayout card = Rows.section(this, sections, getString(R.string.section_license));
        Rows.valueRow(this, card, getString(R.string.organization),
            controller.licenseOrganization.isEmpty() ? "—" : controller.licenseOrganization);
        Rows.valueRow(this, card, getString(R.string.expired_date),
            controller.licenseExpiredDate.isEmpty() ? "—" : controller.licenseExpiredDate);
        Rows.valueRow(this, card, getString(R.string.expired),
            getString(controller.licenseExpired ? R.string.yes : R.string.no),
            ContextCompat.getColor(this, controller.licenseExpired ? R.color.danger : R.color.on_surface));
    }

    // --- ABOUT ---
    private void renderAbout() {
        LinearLayout card = Rows.section(this, sections, getString(R.string.section_about));
        Rows.valueRow(this, card, getString(R.string.contact_us), "",
            () -> controller.openUrl("https://www.flyingbee.com/contact-us"));
        Rows.valueRow(this, card, getString(R.string.product_url), "",
            () -> controller.openUrl("https://www.flyingbee.com/pdf-sdk"));
        Rows.valueRow(this, card, getString(R.string.github), "",
            () -> controller.openUrl("https://github.com/flyingbee-software/flyingbee-pdf-converter-sdk-ios"));
        Rows.bodyText(this, card, getString(R.string.about_desc), 16, 12);
    }

    // === Preview / share ======================================================

    /** "Quick Look" equivalent: open the produced file with a viewer app. */
    private void openForPreview(File file) {
        Uri uri = uriFor(file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeFor(file.getName()));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (Throwable t) {
            controller.setStatus(getString(R.string.status_no_preview_app, file.getName()));
        }
    }

    /** Share sheet equivalent (iOS UIActivityViewController). */
    private void shareFile(File file) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType(mimeFor(file.getName()));
        send.putExtra(Intent.EXTRA_STREAM, uriFor(file));
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(send, getString(R.string.share_save)));
        } catch (Throwable t) {
            controller.setStatus(getString(R.string.status_share_unavailable,
                t.getMessage() != null ? t.getMessage() : "unknown"));
        }
    }

    private Uri uriFor(File file) {
        return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
    }

    private static String mimeFor(String name) {
        int dot = name.lastIndexOf('.');
        String ext = dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.US) : "";
        switch (ext) {
            case "pdf": return "application/pdf";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "html": case "htm": return "text/html";
            case "txt": case "csv": case "xml": return "text/plain";
            case "png": return "image/png";
            case "jpg": case "jpeg": return "image/jpeg";
            case "gif": return "image/gif";
            case "bmp": return "image/bmp";
            case "tif": case "tiff": return "image/tiff";
            case "zip": return "application/zip";
            default: return "application/octet-stream";
        }
    }

    // === Password prompt ======================================================

    private void showPasswordDialog(File file) {
        TextInputEditText edit = new TextInputEditText(this);
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        com.google.android.material.textfield.TextInputLayout til =
            new com.google.android.material.textfield.TextInputLayout(this, null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        til.setHint(getString(R.string.pdf_password_label));
        til.addView(edit, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this)
            .setTitle(R.string.encrypted_pdf_title)
            .setMessage(getString(R.string.encrypted_pdf_body, file.getName()))
            .setView(til)
            .setNegativeButton(R.string.cancel, (d, w) -> controller.clearPasswordPrompt())
            .setPositiveButton(R.string.unlock, (d, w) -> {
                String pw = edit.getText() != null ? edit.getText().toString() : "";
                if (controller.pdfPasswordValid(file, pw)) {
                    controller.pdfPassword = pw;
                    controller.saveSettings();
                    controller.clearPasswordPrompt();
                } else {
                    controller.setStatus(getString(R.string.status_wrong_password));
                    controller.clearPasswordPrompt();
                }
            })
            .setOnCancelListener(d -> controller.clearPasswordPrompt())
            .show();
    }
}
