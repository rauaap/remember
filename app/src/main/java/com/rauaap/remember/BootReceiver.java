package com.rauaap.remember;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * A reboot empties the notification shade, so pinned checklists are posted
 * again. Kept apart from {@link NotificationActionReceiver} because receiving a
 * system broadcast means being exported, and the notification actions should not
 * be.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            ChecklistNotifications.refresh(context);
        }
    }
}
