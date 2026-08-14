package com.rauaap.remember;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * The buttons on a pinned checklist's notification: the dismiss action takes it
 * down for good, and the delete intent — a swipe — puts it straight back.
 */
public class NotificationActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String checklistId = intent.getStringExtra(ChecklistActivity.EXTRA_CHECKLIST_ID);
        if (checklistId == null) {
            return;
        }
        if (ChecklistNotifications.ACTION_DISMISS.equals(intent.getAction())) {
            ChecklistNotifications.unpin(context, checklistId);
        } else if (ChecklistNotifications.ACTION_RESTORE.equals(intent.getAction())) {
            ChecklistNotifications.restore(context, checklistId);
        }
    }
}
