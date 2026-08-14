package com.rauaap.remember;

import android.app.PendingIntent;
import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import java.util.List;

/**
 * Quick settings tile: one tap starts a fresh checklist, opens it and pins it to
 * the notification shade. The pinning is left to {@link ChecklistActivity},
 * which — unlike a service — can ask for the notification permission.
 */
public class NewChecklistTileService extends TileService {

    private static final String ACTION_NEW = "com.rauaap.remember.NEW_CHECKLIST";

    @Override
    public void onStartListening() {
        super.onStartListening();
        Tile tile = getQsTile();
        if (tile != null) {
            // Inactive, not active: this tile does something rather than being
            // on or off, so the highlighted "on" look would be misleading. It
            // stays tappable either way — the state is only cosmetic here.
            tile.setState(Tile.STATE_INACTIVE);
            tile.updateTile();
        }
    }

    @Override
    public void onClick() {
        super.onClick();
        if (isLocked()) {
            unlockAndRun(this::createAndOpen);
        } else {
            createAndOpen();
        }
    }

    private void createAndOpen() {
        Checklist checklist = ChecklistStore.create(this, nextTitle());
        Intent intent = new Intent(this, ChecklistActivity.class)
                .setAction(ACTION_NEW)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ChecklistActivity.EXTRA_CHECKLIST_ID, checklist.id)
                .putExtra(ChecklistActivity.EXTRA_PIN_NOTIFICATION, true);
        startActivityAndCollapse(PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
    }

    /** The lowest numbered name not already taken, so tapping twice never collides. */
    private String nextTitle() {
        List<Checklist> checklists = ChecklistStore.all(this);
        for (int number = 1; ; number++) {
            String title = getString(R.string.quick_checklist_name, number);
            boolean taken = false;
            for (Checklist checklist : checklists) {
                if (checklist.title.equals(title)) {
                    taken = true;
                    break;
                }
            }
            if (!taken) {
                return title;
            }
        }
    }
}
