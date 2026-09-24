package com.flyingbee.FPPDFConverterDemoJava.ui;

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import com.flyingbee.FPPDFConverterDemoJava.ConverterController;
import com.flyingbee.FPPDFConverterDemoJava.R;

/**
 * BaseScreenActivity : the Java counterpart of the Kotlin demo's
 * SettingsScaffold — a top bar with a back arrow plus a scrollable content
 * column. Sub-screens rebuild their rows inside {@link #render()}.
 */
public abstract class BaseScreenActivity extends AppCompatActivity
        implements ConverterController.StateListener {

    protected ConverterController controller;
    protected LinearLayout content;

    protected abstract int titleRes();

    /** Rebuild every row of this screen from the controller state. */
    protected abstract void render();

    /** Called when the user taps back (default: save + finish). */
    protected void onBack() {
        controller.saveSettings();
        finish();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this,
            androidx.activity.SystemBarStyle.light(0, 0),
            androidx.activity.SystemBarStyle.dark(android.graphics.Color.BLACK));
        setContentView(R.layout.activity_screen);
        controller = ConverterController.get();

        MaterialToolbar toolbar = findViewById(R.id.screen_toolbar);
        toolbar.setTitle(titleRes());
        toolbar.setNavigationOnClickListener(v -> onBack());
        content = findViewById(R.id.screen_content);

        // Safe area: toolbar clears the status bar / notch, content the nav bar.
        Rows.applyTopInset(toolbar);
        Rows.applyBottomInset(findViewById(R.id.screen_scroll));
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
        if (!isFinishing()) render();
    }
}
