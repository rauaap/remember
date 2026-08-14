package com.rauaap.remember;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import java.util.List;

/**
 * Home screen widget listing every checklist. Tapping a row opens that
 * checklist; tapping the empty state opens the app.
 *
 * <p>Rows are pushed as a {@link RemoteViews.RemoteCollectionItems} collection
 * rather than served from a {@code RemoteViewsService}, so a refresh is just
 * another {@code updateAppWidget} call.
 */
public class ChecklistWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            update(context, manager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            WidgetSettings.delete(context, appWidgetId);
        }
    }

    /** Redraws every placed widget. Called whenever the checklists change. */
    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName provider = new ComponentName(context, ChecklistWidgetProvider.class);
        for (int appWidgetId : manager.getAppWidgetIds(provider)) {
            update(context, manager, appWidgetId);
        }
    }

    /** Redraws a single widget with its current settings and checklists. */
    public static void update(Context context, AppWidgetManager manager, int appWidgetId) {
        WidgetSettings settings = WidgetSettings.load(context, appWidgetId);
        List<Checklist> checklists = ChecklistStore.all(context);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_checklists);
        views.setInt(R.id.widget_root, "setBackgroundColor", settings.backgroundColor);
        views.setViewOutlinePreferredRadius(
                R.id.widget_root, settings.cornerRadiusDp, TypedValue.COMPLEX_UNIT_DIP);

        boolean empty = checklists.isEmpty();
        views.setViewVisibility(R.id.widget_list, empty ? View.GONE : View.VISIBLE);
        views.setViewVisibility(R.id.widget_empty, empty ? View.VISIBLE : View.GONE);

        views.setTextColor(R.id.widget_empty, settings.textColor);
        views.setTextViewTextSize(
                R.id.widget_empty, TypedValue.COMPLEX_UNIT_SP, settings.fontSizeSp);
        views.setOnClickPendingIntent(R.id.widget_empty, openApp(context, appWidgetId));

        RemoteViews.RemoteCollectionItems.Builder items =
                new RemoteViews.RemoteCollectionItems.Builder()
                        .setHasStableIds(true)
                        .setViewTypeCount(1);
        for (Checklist checklist : checklists) {
            items.addItem(checklist.id.hashCode(), row(context, checklist, settings));
        }
        views.setRemoteAdapter(R.id.widget_list, items.build());
        views.setPendingIntentTemplate(R.id.widget_list, openChecklistTemplate(context, appWidgetId));

        manager.updateAppWidget(appWidgetId, views);
    }

    private static RemoteViews row(
            Context context, Checklist checklist, WidgetSettings settings) {
        RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_row);
        row.setTextViewText(R.id.widget_row_title, checklist.title);
        row.setTextColor(R.id.widget_row_title, settings.textColor);
        row.setTextViewTextSize(
                R.id.widget_row_title, TypedValue.COMPLEX_UNIT_SP, settings.fontSizeSp);

        int paddingH = settings.rowPaddingHorizontalPx(context);
        int paddingV = settings.rowPaddingVerticalPx(context);
        row.setViewPadding(R.id.widget_row, paddingH, paddingV, paddingH, paddingV);

        Intent fillIn = new Intent();
        fillIn.putExtra(ChecklistActivity.EXTRA_CHECKLIST_ID, checklist.id);
        row.setOnClickFillInIntent(R.id.widget_row, fillIn);
        return row;
    }

    /**
     * Template for row taps. Mutable so each row's fill-in intent can supply its
     * own checklist id; the request code keeps widgets from sharing one instance.
     */
    private static PendingIntent openChecklistTemplate(Context context, int appWidgetId) {
        Intent intent = new Intent(context, ChecklistActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        return PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
    }

    private static PendingIntent openApp(Context context, int appWidgetId) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
