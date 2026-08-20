package com.rauaap.remember;

import android.app.PendingIntent;
import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * Quick settings tile: one tap opens the home screen armed to pin. Whichever
 * checklist you go into next — an existing one or a brand new one — is the one
 * that ends up in the notification shade.
 *
 * <p>The pinning itself is left to {@link ChecklistActivity}, which — unlike a
 * service — can ask for the notification permission.
 */
public class PinChecklistTileService extends TileService {

    private static final String ACTION_PIN = "com.rauaap.remember.PIN_CHECKLIST";

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
            unlockAndRun(this::openChecklists);
        } else {
            openChecklists();
        }
    }

    private void openChecklists() {
        // SINGLE_TOP alongside CLEAR_TOP so an already open home screen is
        // handed the intent instead of being torn down and rebuilt.
        Intent intent = new Intent(this, MainActivity.class)
                .setAction(ACTION_PIN)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_PIN_NEXT, true);
        startActivityAndCollapse(PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
    }
}
