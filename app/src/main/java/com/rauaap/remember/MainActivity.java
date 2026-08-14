package com.rauaap.remember;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Home screen: every checklist, newest last. */
public class MainActivity extends Activity {

    private final List<Checklist> checklists = new ArrayList<>();
    private ChecklistAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        adapter = new ChecklistAdapter();
        ListView list = findViewById(R.id.checklist_list);
        list.setEmptyView(findViewById(R.id.checklist_empty));
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> open(checklists.get(position)));
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            showChecklistMenu(checklists.get(position));
            return true;
        });

        findViewById(R.id.button_new_checklist).setOnClickListener(v -> Dialogs.prompt(
                this, R.string.new_checklist, R.string.checklist_name_hint, "", title -> {
                    open(ChecklistStore.create(this, title));
                }));
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        checklists.clear();
        checklists.addAll(ChecklistStore.all(this));
        adapter.notifyDataSetChanged();
    }

    private void open(Checklist checklist) {
        Intent intent = new Intent(this, ChecklistActivity.class);
        intent.putExtra(ChecklistActivity.EXTRA_CHECKLIST_ID, checklist.id);
        startActivity(intent);
    }

    private void showChecklistMenu(final Checklist checklist) {
        String[] actions = {
                getString(R.string.rename),
                getString(R.string.delete),
        };
        new AlertDialog.Builder(this)
                .setTitle(checklist.title)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        Dialogs.prompt(
                                this,
                                R.string.rename_checklist,
                                R.string.checklist_name_hint,
                                checklist.title,
                                title -> {
                                    ChecklistStore.rename(this, checklist.id, title);
                                    reload();
                                });
                    } else {
                        Dialogs.confirmDelete(this, checklist.title, () -> {
                            ChecklistStore.delete(this, checklist.id);
                            reload();
                        });
                    }
                })
                .show();
    }

    private class ChecklistAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return checklists.size();
        }

        @Override
        public Checklist getItem(int position) {
            return checklists.get(position);
        }

        @Override
        public long getItemId(int position) {
            return checklists.get(position).id.hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(MainActivity.this)
                        .inflate(R.layout.row_checklist, parent, false);
            }
            Checklist checklist = checklists.get(position);
            ((TextView) view.findViewById(R.id.row_title)).setText(checklist.title);
            ((TextView) view.findViewById(R.id.row_count)).setText(String.format(
                    Locale.US, "%d/%d", checklist.checkedCount(), checklist.items.size()));
            return view;
        }
    }
}
