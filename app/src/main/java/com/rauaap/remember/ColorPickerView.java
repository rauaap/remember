package com.rauaap.remember;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

/**
 * RGBA colour picker: one slider per channel plus a #AARRGGBB text field. The
 * sliders and the field are two views of the same value, so editing either one
 * updates the other and the swatch.
 */
public class ColorPickerView extends LinearLayout {

    public interface OnColorChangedListener {
        void onColorChanged(int color);
    }

    private SeekBar alphaSlider;
    private SeekBar redSlider;
    private SeekBar greenSlider;
    private SeekBar blueSlider;
    private TextView alphaValue;
    private TextView redValue;
    private TextView greenValue;
    private TextView blueValue;
    private EditText hexField;
    private View swatch;

    private int color = Color.BLACK;
    /** Guards the slider/hex/swatch sync so an update cannot feed back on itself. */
    private boolean syncing;
    private OnColorChangedListener listener;

    public ColorPickerView(Context context) {
        this(context, null);
    }

    public ColorPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        int horizontal = dp(context, 24);
        setPadding(horizontal, dp(context, 16), horizontal, 0);
        inflate(context, R.layout.view_color_picker, this);

        alphaSlider = findViewById(R.id.picker_alpha);
        redSlider = findViewById(R.id.picker_red);
        greenSlider = findViewById(R.id.picker_green);
        blueSlider = findViewById(R.id.picker_blue);
        alphaValue = findViewById(R.id.picker_alpha_value);
        redValue = findViewById(R.id.picker_red_value);
        greenValue = findViewById(R.id.picker_green_value);
        blueValue = findViewById(R.id.picker_blue_value);
        hexField = findViewById(R.id.picker_hex);
        swatch = findViewById(R.id.picker_swatch);

        SeekBar.OnSeekBarChangeListener sliders = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!syncing) {
                    setColorInternal(colorFromSliders(), false);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
        alphaSlider.setOnSeekBarChangeListener(sliders);
        redSlider.setOnSeekBarChangeListener(sliders);
        greenSlider.setOnSeekBarChangeListener(sliders);
        blueSlider.setOnSeekBarChangeListener(sliders);

        hexField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (syncing) {
                    return;
                }
                Integer parsed = parseHex(editable.toString());
                if (parsed != null) {
                    // Leave the field alone: the user is still typing in it.
                    setColorInternal(parsed, true);
                }
            }
        });

        setColor(color);
    }

    public void setOnColorChangedListener(OnColorChangedListener listener) {
        this.listener = listener;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        setColorInternal(color, false);
    }

    private void setColorInternal(int color, boolean keepHexText) {
        this.color = color;
        syncing = true;
        alphaSlider.setProgress(Color.alpha(color));
        redSlider.setProgress(Color.red(color));
        greenSlider.setProgress(Color.green(color));
        blueSlider.setProgress(Color.blue(color));
        alphaValue.setText(String.valueOf(Color.alpha(color)));
        redValue.setText(String.valueOf(Color.red(color)));
        greenValue.setText(String.valueOf(Color.green(color)));
        blueValue.setText(String.valueOf(Color.blue(color)));
        if (!keepHexText) {
            hexField.setText(toHex(color));
        }
        syncing = false;

        swatch.setBackgroundColor(color);
        if (listener != null) {
            listener.onColorChanged(color);
        }
    }

    private int colorFromSliders() {
        return Color.argb(
                alphaSlider.getProgress(),
                redSlider.getProgress(),
                greenSlider.getProgress(),
                blueSlider.getProgress());
    }

    private static int dp(Context context, float value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics());
    }

    /** Formats as {@code #AARRGGBB}, the same form the field accepts. */
    public static String toHex(int color) {
        return String.format(Locale.US, "#%08X", color);
    }

    /**
     * Parses {@code #AARRGGBB}, {@code #RRGGBB} or either without the {@code #}.
     * Returns null when the text is not a complete colour.
     */
    public static Integer parseHex(String text) {
        String digits = text.trim();
        if (digits.startsWith("#")) {
            digits = digits.substring(1);
        }
        if (digits.length() != 6 && digits.length() != 8) {
            return null;
        }
        try {
            long value = Long.parseLong(digits, 16);
            if (digits.length() == 6) {
                value |= 0xFF000000L;
            }
            return (int) value;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
