package com.rauaap.remember;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/**
 * Widget appearance settings, shown when a widget is placed and again whenever
 * it is reconfigured. Nothing is written until Save, so backing out leaves an
 * existing widget untouched.
 */
public class WidgetConfigActivity extends Activity {

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private WidgetSettings settings;

    private View previewCard;
    private TextView[] previewRows;
    private TextView fontValue;
    private TextView radiusValue;
    private View textSwatch;
    private View backgroundSwatch;
    private TextView textHex;
    private TextView backgroundHex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }
        // Backing out of an initial placement must cancel the widget.
        setResult(RESULT_CANCELED, new Intent().putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId));

        setContentView(R.layout.activity_widget_config);
        settings = WidgetSettings.load(this, appWidgetId);

        previewCard = findViewById(R.id.preview_card);
        previewRows = new TextView[] {
                findViewById(R.id.preview_row_1),
                findViewById(R.id.preview_row_2),
                findViewById(R.id.preview_row_3),
        };
        fontValue = findViewById(R.id.font_value);
        radiusValue = findViewById(R.id.radius_value);
        textSwatch = findViewById(R.id.text_color_swatch);
        backgroundSwatch = findViewById(R.id.background_color_swatch);
        textHex = findViewById(R.id.text_color_hex);
        backgroundHex = findViewById(R.id.background_color_hex);

        fillPreviewRows();
        setUpFontSlider();
        setUpRadiusSlider();

        findViewById(R.id.text_color_row).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickColor(R.string.text_color, settings.textColor, new ColorTarget() {
                    @Override
                    public void apply(int color) {
                        settings.textColor = color;
                    }
                });
            }
        });
        findViewById(R.id.background_color_row).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickColor(R.string.background_color, settings.backgroundColor, new ColorTarget() {
                    @Override
                    public void apply(int color) {
                        settings.backgroundColor = color;
                    }
                });
            }
        });

        findViewById(R.id.button_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });
        findViewById(R.id.button_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        refreshPreview();
    }

    private void setUpFontSlider() {
        SeekBar slider = findViewById(R.id.font_slider);
        slider.setMax((int) (WidgetSettings.MAX_FONT_SP - WidgetSettings.MIN_FONT_SP));
        slider.setProgress((int) (settings.fontSizeSp - WidgetSettings.MIN_FONT_SP));
        slider.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                settings.fontSizeSp = WidgetSettings.MIN_FONT_SP + progress;
                refreshPreview();
            }
        });
    }

    private void setUpRadiusSlider() {
        SeekBar slider = findViewById(R.id.radius_slider);
        slider.setMax((int) WidgetSettings.MAX_RADIUS_DP);
        slider.setProgress((int) settings.cornerRadiusDp);
        slider.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                settings.cornerRadiusDp = progress;
                refreshPreview();
            }
        });
    }

    /** Preview the user's own checklists when there are any. */
    private void fillPreviewRows() {
        List<Checklist> checklists = ChecklistStore.all(this);
        String[] fallback = {
                getString(R.string.preview_row_1),
                getString(R.string.preview_row_2),
                getString(R.string.preview_row_3),
        };
        for (int i = 0; i < previewRows.length; i++) {
            previewRows[i].setText(
                    i < checklists.size() ? checklists.get(i).title : fallback[i]);
        }
    }

    private interface ColorTarget {
        void apply(int color);
    }

    private void pickColor(int titleRes, int current, final ColorTarget target) {
        final ColorPickerView picker = new ColorPickerView(this);
        picker.setColor(current);
        new AlertDialog.Builder(this)
                .setTitle(titleRes)
                .setView(picker)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.set, (dialog, which) -> {
                    target.apply(picker.getColor());
                    refreshPreview();
                })
                .show();
    }

    private void refreshPreview() {
        GradientDrawable background = new GradientDrawable();
        background.setColor(settings.backgroundColor);
        background.setCornerRadius(dp(settings.cornerRadiusDp));
        previewCard.setBackground(background);

        int paddingH = settings.rowPaddingHorizontalPx(this);
        int paddingV = settings.rowPaddingVerticalPx(this);
        for (TextView row : previewRows) {
            row.setTextColor(settings.textColor);
            row.setTextSize(TypedValue.COMPLEX_UNIT_SP, settings.fontSizeSp);
            row.setPadding(paddingH, paddingV, paddingH, paddingV);
        }

        fontValue.setText(String.format(Locale.US, "%d sp", (int) settings.fontSizeSp));
        radiusValue.setText(String.format(Locale.US, "%d dp", (int) settings.cornerRadiusDp));
        textSwatch.setBackground(swatch(settings.textColor));
        backgroundSwatch.setBackground(swatch(settings.backgroundColor));
        textHex.setText(ColorPickerView.toHex(settings.textColor));
        backgroundHex.setText(ColorPickerView.toHex(settings.backgroundColor));
    }

    private GradientDrawable swatch(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(6));
        drawable.setStroke((int) dp(1), Color.argb(80, 255, 255, 255));
        return drawable;
    }

    private float dp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private void save() {
        settings.save(this, appWidgetId);
        ChecklistWidgetProvider.update(this, AppWidgetManager.getInstance(this), appWidgetId);
        setResult(RESULT_OK, new Intent().putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId));
        finish();
    }

    private abstract static class SimpleSeekBarListener
            implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }
}
