package com.rauaap.remember;

import android.app.Activity;
import android.app.AlertDialog;
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

import java.util.ArrayList;
import java.util.List;

/** One checklist: tick items off, add new ones, rename or remove old ones. */
public class ChecklistActivity extends Activity {

    public static final String EXTRA_CHECKLIST_ID = "checklist_id";

    private String checklistId;
    private final List<Checklist.Item> items = new ArrayList<>();
    private ItemAdapter adapter;
    private TextView titleView;
    private EditText newItemField;

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
        adapter = new ItemAdapter();

        ListView list = findViewById(R.id.item_list);
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isFinishing()) {
            reload();
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
