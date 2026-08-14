package com.rauaap.remember;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** One checklist: tick items off, add new ones, rename or remove old ones. */
public class ChecklistActivity extends Activity {

    public static final String EXTRA_CHECKLIST_ID = "checklist_id";

    /**
     * Set by the quick settings tile: pin this checklist to the notification
     * shade once opened. The activity does it rather than the tile because only
     * an activity can ask for the notification permission.
     */
    public static final String EXTRA_PIN_NOTIFICATION = "pin_notification";

    private static final int REQUEST_POST_NOTIFICATIONS = 1;

    private String checklistId;
    private final List<Checklist.Item> items = new ArrayList<>();
    private ItemAdapter adapter;
    private TextView titleView;
    private EditText newItemField;
    private View removeCheckedButton;
    private ListView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checklistId = getIntent().getStringExtra(EXTRA_CHECKLIST_ID);
        if (ChecklistStore.get(this, checklistId) == null) {
            // The checklist was deleted while a widget still pointed at it.
            finish();
            return;
        }
        setContentView(R.layout.activity_checklist);

        titleView = findViewById(R.id.checklist_title);
        newItemField = findViewById(R.id.new_item_field);
        removeCheckedButton = findViewById(R.id.button_remove_checked);
        adapter = new ItemAdapter();

        list = findViewById(R.id.item_list);
        list.setEmptyView(findViewById(R.id.item_empty));
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            Checklist.Item item = items.get(position);
            ChecklistStore.setItemChecked(this, checklistId, item.id, !item.checked);
            reload();
        });
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            showItemMenu(items.get(position));
            return true;
        });

        findViewById(R.id.button_add_item).setOnClickListener(v -> addItem());
        newItemField.setOnEditorActionListener((v, actionId, event) -> {
            addItem();
            return true;
        });
        findViewById(R.id.button_uncheck_all).setOnClickListener(v -> {
            ChecklistStore.uncheckAll(this, checklistId);
            reload();
        });
        removeCheckedButton.setOnClickListener(v -> removeChecked());
        findViewById(R.id.button_back).setOnClickListener(v -> openChecklists());

        // A list started from the tile arrives with a placeholder name, so the
        // title is the one place it can be renamed without going back home.
        titleView.setOnClickListener(v -> Dialogs.prompt(
                this,
                R.string.rename_checklist,
                R.string.checklist_name_hint,
                titleView.getText().toString(),
                title -> {
                    ChecklistStore.rename(this, checklistId, title);
                    reload();
                }));

        if (savedInstanceState == null
                && getIntent().getBooleanExtra(EXTRA_PIN_NOTIFICATION, false)) {
            pinNotification();
        }
    }

    /**
     * Up to the home screen. The widget opens a checklist directly, so there may
     * be no MainActivity behind this one to go back to.
     */
    private void openChecklists() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void removeChecked() {
        Checklist checklist = ChecklistStore.get(this, checklistId);
        int checked = checklist == null ? 0 : checklist.checkedCount();
        if (checked == 0) {
            return;
        }
        String title = getResources().getQuantityString(
                R.plurals.remove_checked_title, checked, checked);
        Dialogs.confirm(this, title, R.string.remove, () -> {
            ChecklistStore.deleteCheckedItems(this, checklistId);
            reload();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isFinishing()) {
            reload();
        }
    }

    /** Pins the checklist, asking for the notification permission if it is missing. */
    private void pinNotification() {
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            ChecklistNotifications.pin(this, checklistId);
        } else {
            requestPermissions(
                    new String[] {Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_POST_NOTIFICATIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != REQUEST_POST_NOTIFICATIONS) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            ChecklistNotifications.pin(this, checklistId);
        } else {
            // The checklist is still there; it just has nothing in the shade.
            Toast.makeText(this, R.string.notifications_blocked, Toast.LENGTH_LONG).show();
        }
    }

    private void addItem() {
        String text = newItemField.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }
        ChecklistStore.addItem(this, checklistId, text);
        newItemField.setText("");
        reload();
        // Items append to the end, which may be below the fold on a long list.
        list.smoothScrollToPosition(items.size() - 1);
    }

    private void reload() {
        Checklist checklist = ChecklistStore.get(this, checklistId);
        if (checklist == null) {
            finish();
            return;
        }
        titleView.setText(checklist.title);
        items.clear();
        items.addAll(checklist.items);
        adapter.notifyDataSetChanged();

        boolean anyChecked = checklist.checkedCount() > 0;
        removeCheckedButton.setEnabled(anyChecked);
        removeCheckedButton.setAlpha(anyChecked ? 1f : 0.4f);
    }

    private void showItemMenu(final Checklist.Item item) {
        String[] actions = {
                getString(R.string.edit),
                getString(R.string.delete),
        };
        new AlertDialog.Builder(this)
                .setTitle(item.text)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        Dialogs.prompt(
                                this,
                                R.string.edit_item,
                                R.string.item_hint,
                                item.text,
                                text -> {
                                    ChecklistStore.editItem(this, checklistId, item.id, text);
                                    reload();
                                });
                    } else {
                        ChecklistStore.deleteItem(this, checklistId, item.id);
                        reload();
                    }
                })
                .show();
    }

    private class ItemAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Checklist.Item getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return items.get(position).id.hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(ChecklistActivity.this)
                        .inflate(R.layout.row_item, parent, false);
            }
            Checklist.Item item = items.get(position);
            CheckBox box = view.findViewById(R.id.row_checkbox);
            TextView text = view.findViewById(R.id.row_text);
            box.setChecked(item.checked);
            text.setText(item.text);
            text.setPaintFlags(item.checked
                    ? text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                    : text.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            text.setTextColor(getColor(
                    item.checked ? R.color.text_secondary : R.color.text_primary));
            return view;
        }
    }
}
