package com.rauaap.remember;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The whole checklist database: a JSON array in SharedPreferences, held in a
 * process-wide cache. Every mutation persists and refreshes the home screen
 * widgets and pinned notifications, so callers never have to remember to do any
 * of it.
 */
public final class ChecklistStore {

    private static final String PREFS = "checklists";
    private static final String KEY_DATA = "data";

    private static List<Checklist> cache;

    private ChecklistStore() {
    }

    public static synchronized List<Checklist> all(Context context) {
        return new ArrayList<>(load(context));
    }

    public static synchronized Checklist get(Context context, String id) {
        for (Checklist checklist : load(context)) {
            if (checklist.id.equals(id)) {
                return checklist;
            }
        }
        return null;
    }

    public static synchronized Checklist create(Context context, String title) {
        Checklist checklist = Checklist.create(title);
        load(context).add(checklist);
        persist(context);
        return checklist;
    }

    public static synchronized void rename(Context context, String id, String title) {
        Checklist checklist = get(context, id);
        if (checklist != null) {
            checklist.title = title;
            persist(context);
        }
    }

    public static synchronized void delete(Context context, String id) {
        List<Checklist> checklists = load(context);
        for (int i = 0; i < checklists.size(); i++) {
            if (checklists.get(i).id.equals(id)) {
                checklists.remove(i);
                persist(context);
                return;
            }
        }
    }

    public static synchronized void addItem(Context context, String checklistId, String text) {
        Checklist checklist = get(context, checklistId);
        if (checklist != null) {
            checklist.items.add(Checklist.Item.create(text));
            persist(context);
        }
    }

    public static synchronized void editItem(
            Context context, String checklistId, String itemId, String text) {
        Checklist checklist = get(context, checklistId);
        Checklist.Item item = checklist == null ? null : checklist.item(itemId);
        if (item != null) {
            item.text = text;
            persist(context);
        }
    }

    public static synchronized void setItemChecked(
            Context context, String checklistId, String itemId, boolean checked) {
        Checklist checklist = get(context, checklistId);
        Checklist.Item item = checklist == null ? null : checklist.item(itemId);
        if (item != null && item.checked != checked) {
            item.checked = checked;
            persist(context);
        }
    }

    public static synchronized void deleteItem(
            Context context, String checklistId, String itemId) {
        Checklist checklist = get(context, checklistId);
        if (checklist == null) {
            return;
        }
        for (int i = 0; i < checklist.items.size(); i++) {
            if (checklist.items.get(i).id.equals(itemId)) {
                checklist.items.remove(i);
                persist(context);
                return;
            }
        }
    }

    public static synchronized void deleteCheckedItems(Context context, String checklistId) {
        Checklist checklist = get(context, checklistId);
        if (checklist == null) {
            return;
        }
        boolean changed = false;
        for (Iterator<Checklist.Item> items = checklist.items.iterator(); items.hasNext(); ) {
            if (items.next().checked) {
                items.remove();
                changed = true;
            }
        }
        if (changed) {
            persist(context);
        }
    }

    public static synchronized void uncheckAll(Context context, String checklistId) {
        Checklist checklist = get(context, checklistId);
        if (checklist == null) {
            return;
        }
        boolean changed = false;
        for (Checklist.Item item : checklist.items) {
            if (item.checked) {
                item.checked = false;
                changed = true;
            }
        }
        if (changed) {
            persist(context);
        }
    }

    private static List<Checklist> load(Context context) {
        if (cache != null) {
            return cache;
        }
        List<Checklist> checklists = new ArrayList<>();
        String data = prefs(context).getString(KEY_DATA, null);
        if (data != null) {
            try {
                JSONArray json = new JSONArray(data);
                for (int i = 0; i < json.length(); i++) {
                    JSONObject entry = json.optJSONObject(i);
                    if (entry != null) {
                        checklists.add(Checklist.fromJson(entry));
                    }
                }
            } catch (JSONException e) {
                // Corrupt storage: start over rather than crash on every launch.
                checklists.clear();
            }
        }
        cache = checklists;
        return cache;
    }

    private static void persist(Context context) {
        JSONArray json = new JSONArray();
        try {
            for (Checklist checklist : load(context)) {
                json.put(checklist.toJson());
            }
        } catch (JSONException e) {
            return;
        }
        prefs(context).edit().putString(KEY_DATA, json.toString()).apply();
        ChecklistWidgetProvider.updateAll(context);
        ChecklistNotifications.refresh(context);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
