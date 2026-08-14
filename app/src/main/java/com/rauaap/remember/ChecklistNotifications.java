package com.rauaap.remember;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Checklists pinned to the notification shade. A pinned checklist keeps a
 * notification that opens it on tap and shows what is still unticked; the set of
 * pinned ids lives in its own preferences file so the notifications can be put
 * back after a reboot.
 *
 * <p>The notification is ongoing and carries {@code FLAG_NO_CLEAR}, so "clear
 * all" leaves it alone. Since Android 14 that is not enough to stop a deliberate
 * swipe, so the delete intent posts it straight back — the only way out is the
 * dismiss action, which unpins first.
 *
 * <p>No locking: every entry point (tile taps, notification actions, activity
 * callbacks, boot) runs on the main thread.
 */
public final class ChecklistNotifications {

    static final String ACTION_DISMISS = "com.rauaap.remember.DISMISS_NOTIFICATION";
    static final String ACTION_RESTORE = "com.rauaap.remember.RESTORE_NOTIFICATION";

    private static final String ACTION_OPEN = "com.rauaap.remember.OPEN_NOTIFICATION";

    private static final String PREFS = "notifications";
    private static final String KEY_PINNED = "pinned";
    private static final String CHANNEL = "pinned_checklists";

    /** Unticked items listed in the expanded notification before it trails off. */
    private static final int MAX_LISTED_ITEMS = 8;

    private ChecklistNotifications() {
    }

    /** Pins a checklist, posting its notification. Does nothing if already pinned. */
    public static void pin(Context context, String checklistId) {
        List<String> pinned = load(context);
        if (!pinned.contains(checklistId)) {
            pinned.add(checklistId);
            save(context, pinned);
        }
        Checklist checklist = ChecklistStore.get(context, checklistId);
        if (checklist != null) {
            post(context, checklist);
        }
    }

    /** Unpins a checklist and takes its notification down for good. */
    public static void unpin(Context context, String checklistId) {
        List<String> pinned = load(context);
        if (pinned.remove(checklistId)) {
            save(context, pinned);
        }
        manager(context).cancel(notificationId(checklistId));
    }

    /**
     * Puts a swiped-away notification back, unless the dismiss action already
     * unpinned the checklist.
     */
    public static void restore(Context context, String checklistId) {
        if (load(context).contains(checklistId)) {
            Checklist checklist = ChecklistStore.get(context, checklistId);
            if (checklist != null) {
                post(context, checklist);
            }
        }
    }

    /**
     * Reposts every pinned notification, forgetting the ones whose checklist has
     * been deleted. Called whenever the checklists change, and again after a
     * reboot has cleared the shade.
     */
    public static void refresh(Context context) {
        List<String> pinned = load(context);
        boolean changed = false;
        for (Iterator<String> ids = pinned.iterator(); ids.hasNext(); ) {
            String checklistId = ids.next();
            Checklist checklist = ChecklistStore.get(context, checklistId);
            if (checklist == null) {
                manager(context).cancel(notificationId(checklistId));
                ids.remove();
                changed = true;
            } else {
                post(context, checklist);
            }
        }
        if (changed) {
            save(context, pinned);
        }
    }

    private static void post(Context context, Checklist checklist) {
        NotificationManager manager = manager(context);
        manager.createNotificationChannel(new NotificationChannel(
                CHANNEL,
                context.getString(R.string.channel_pinned_checklists),
                NotificationManager.IMPORTANCE_LOW));

        Notification.Action dismiss = new Notification.Action.Builder(
                Icon.createWithResource(context, R.drawable.ic_close_mono),
                context.getString(R.string.dismiss_notification),
                broadcast(context, ACTION_DISMISS, checklist.id))
                .build();

        Notification notification = new Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_checklist_mono)
                .setColor(context.getColor(R.color.accent))
                .setContentTitle(checklist.title)
                .setContentText(summary(context, checklist))
                .setStyle(new Notification.BigTextStyle().bigText(details(context, checklist)))
                .setContentIntent(open(context, checklist.id))
                .setDeleteIntent(broadcast(context, ACTION_RESTORE, checklist.id))
                .addAction(dismiss)
                .setOngoing(true)
                .setShowWhen(false)
                .setLocalOnly(true)
                .build();
        notification.flags |= Notification.FLAG_NO_CLEAR;

        manager.notify(notificationId(checklist.id), notification);
    }

    /** The collapsed line: how far through the list you are. */
    private static String summary(Context context, Checklist checklist) {
        if (checklist.items.isEmpty()) {
            return context.getString(R.string.notification_no_items);
        }
        return context.getString(
                R.string.notification_progress,
                checklist.checkedCount(),
                checklist.items.size());
    }

    /** The expanded line: what is still unticked, or the summary if nothing is. */
    private static CharSequence details(Context context, Checklist checklist) {
        StringBuilder text = new StringBuilder();
        int listed = 0;
        for (Checklist.Item item : checklist.items) {
            if (item.checked) {
                continue;
            }
            if (listed == MAX_LISTED_ITEMS) {
                text.append("\n…");
                break;
            }
            if (listed > 0) {
                text.append('\n');
            }
            text.append("• ").append(item.text);
            listed++;
        }
        return listed == 0 ? summary(context, checklist) : text;
    }

    /**
     * Opens the checklist. The action keeps this distinct from the widget's and
     * the tile's pending intents, which target the same activity; the request
     * code keeps one checklist's intent from overwriting another's.
     */
    private static PendingIntent open(Context context, String checklistId) {
        Intent intent = new Intent(context, ChecklistActivity.class)
                .setAction(ACTION_OPEN)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ChecklistActivity.EXTRA_CHECKLIST_ID, checklistId);
        return PendingIntent.getActivity(
                context,
                notificationId(checklistId),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent broadcast(Context context, String action, String checklistId) {
        Intent intent = new Intent(context, NotificationActionReceiver.class)
                .setAction(action)
                .putExtra(ChecklistActivity.EXTRA_CHECKLIST_ID, checklistId);
        return PendingIntent.getBroadcast(
                context,
                notificationId(checklistId),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static int notificationId(String checklistId) {
        return checklistId.hashCode();
    }

    private static List<String> load(Context context) {
        List<String> pinned = new ArrayList<>();
        String data = prefs(context).getString(KEY_PINNED, null);
        if (data != null) {
            try {
                JSONArray json = new JSONArray(data);
                for (int i = 0; i < json.length(); i++) {
                    String checklistId = json.optString(i, null);
                    if (checklistId != null) {
                        pinned.add(checklistId);
                    }
                }
            } catch (JSONException e) {
                // Corrupt storage: nothing is pinned rather than crash on boot.
                pinned.clear();
            }
        }
        return pinned;
    }

    private static void save(Context context, List<String> pinned) {
        JSONArray json = new JSONArray();
        for (String checklistId : pinned) {
            json.put(checklistId);
        }
        prefs(context).edit().putString(KEY_PINNED, json.toString()).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static NotificationManager manager(Context context) {
        return context.getSystemService(NotificationManager.class);
    }
}
