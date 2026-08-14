package com.rauaap.remember;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;

/** Small shared dialogs: naming things, and confirming destructive things. */
final class Dialogs {

    interface OnText {
        void onText(String text);
    }

    private Dialogs() {
    }

    /** Asks for a single line of text; the callback only fires for non-blank input. */
    static void prompt(
            Context context, int titleRes, int hintRes, String initial, final OnText callback) {
        final EditText input = new EditText(context);
        input.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setHint(hintRes);
        input.setSingleLine(true);
        input.setText(initial);
        input.setSelection(input.getText().length());

        int padding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 20, context.getResources().getDisplayMetrics());
        FrameLayout container = new FrameLayout(context);
        container.setPadding(padding, padding / 2, padding, 0);
        container.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(titleRes)
                .setView(container)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, which) -> {
                    String text = input.getText().toString().trim();
                    if (!text.isEmpty()) {
                        callback.onText(text);
                    }
                })
                .create();
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    static void confirmDelete(
            Context context, String name, final Runnable onConfirm) {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.delete_title, name))
                .setMessage(R.string.delete_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, which) -> onConfirm.run())
                .show();
    }
}
