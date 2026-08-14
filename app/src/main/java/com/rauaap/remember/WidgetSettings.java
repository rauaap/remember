package com.rauaap.remember;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.TypedValue;

/** Per-widget-instance appearance, as chosen in {@link WidgetConfigActivity}. */
public final class WidgetSettings {

    public static final float MIN_FONT_SP = 10f;
    public static final float MAX_FONT_SP = 30f;
    public static final float MAX_RADIUS_DP = 48f;

    static final float DEFAULT_FONT_SP = 15f;
    static final int DEFAULT_TEXT_COLOR = 0xFFF2EFEA;
    static final int DEFAULT_BACKGROUND_COLOR = 0xCC1A1820;
    static final float DEFAULT_RADIUS_DP = 16f;

    private static final String PREFS = "widget_settings";

    public float fontSizeSp = DEFAULT_FONT_SP;
    public int textColor = DEFAULT_TEXT_COLOR;
    public int backgroundColor = DEFAULT_BACKGROUND_COLOR;
    public float cornerRadiusDp = DEFAULT_RADIUS_DP;

    public static WidgetSettings load(Context context, int appWidgetId) {
        SharedPreferences prefs = prefs(context);
        WidgetSettings settings = new WidgetSettings();
        settings.fontSizeSp = prefs.getFloat(key(appWidgetId, "font"), DEFAULT_FONT_SP);
        settings.textColor = prefs.getInt(key(appWidgetId, "text"), DEFAULT_TEXT_COLOR);
        settings.backgroundColor =
                prefs.getInt(key(appWidgetId, "background"), DEFAULT_BACKGROUND_COLOR);
        settings.cornerRadiusDp = prefs.getFloat(key(appWidgetId, "radius"), DEFAULT_RADIUS_DP);
        return settings;
    }

    public void save(Context context, int appWidgetId) {
        prefs(context).edit()
                .putFloat(key(appWidgetId, "font"), fontSizeSp)
                .putInt(key(appWidgetId, "text"), textColor)
                .putInt(key(appWidgetId, "background"), backgroundColor)
                .putFloat(key(appWidgetId, "radius"), cornerRadiusDp)
                .apply();
    }

    public static void delete(Context context, int appWidgetId) {
        prefs(context).edit()
                .remove(key(appWidgetId, "font"))
                .remove(key(appWidgetId, "text"))
                .remove(key(appWidgetId, "background"))
                .remove(key(appWidgetId, "radius"))
                .apply();
    }

    /**
     * Row padding is a fraction of the font size rather than a fixed dp, so a
     * 10 sp list reads as a tight one instead of carrying the airy gaps that
     * only suit 30 sp. Returned in pixels, ready for
     * {@code RemoteViews.setViewPadding} or {@code View.setPadding}.
     */
    public int rowPaddingVerticalPx(Context context) {
        return sp(context, fontSizeSp * 0.3f);
    }

    /** Horizontal counterpart of {@link #rowPaddingVerticalPx}. */
    public int rowPaddingHorizontalPx(Context context) {
        return sp(context, fontSizeSp * 0.45f);
    }

    private static int sp(Context context, float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, value, context.getResources().getDisplayMetrics()));
    }

    private static String key(int appWidgetId, String name) {
        return appWidgetId + "_" + name;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
