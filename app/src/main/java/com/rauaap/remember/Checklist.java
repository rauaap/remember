package com.rauaap.remember;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A named list of checkable items. Ids are stable across renames and reorders. */
public final class Checklist {

    public static final class Item {
        public final String id;
        public String text;
        public boolean checked;

        Item(String id, String text, boolean checked) {
            this.id = id;
            this.text = text;
            this.checked = checked;
        }

        static Item create(String text) {
            return new Item(UUID.randomUUID().toString(), text, false);
        }

        JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("text", text);
            json.put("checked", checked);
            return json;
        }

        static Item fromJson(JSONObject json) {
            return new Item(
                    json.optString("id", UUID.randomUUID().toString()),
                    json.optString("text", ""),
                    json.optBoolean("checked", false));
        }
    }

    public final String id;
    public String title;
    public final List<Item> items;

    private Checklist(String id, String title, List<Item> items) {
        this.id = id;
        this.title = title;
        this.items = items;
    }

    static Checklist create(String title) {
        return new Checklist(UUID.randomUUID().toString(), title, new ArrayList<Item>());
    }

    public int checkedCount() {
        int checked = 0;
        for (Item item : items) {
            if (item.checked) {
                checked++;
            }
        }
        return checked;
    }

    public Item item(String itemId) {
        for (Item item : items) {
            if (item.id.equals(itemId)) {
                return item;
            }
        }
        return null;
    }

    JSONObject toJson() throws JSONException {
        JSONArray jsonItems = new JSONArray();
        for (Item item : items) {
            jsonItems.put(item.toJson());
        }
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("title", title);
        json.put("items", jsonItems);
        return json;
    }

    static Checklist fromJson(JSONObject json) {
        List<Item> items = new ArrayList<>();
        JSONArray jsonItems = json.optJSONArray("items");
        if (jsonItems != null) {
            for (int i = 0; i < jsonItems.length(); i++) {
                JSONObject jsonItem = jsonItems.optJSONObject(i);
                if (jsonItem != null) {
                    items.add(Item.fromJson(jsonItem));
                }
            }
        }
        return new Checklist(
                json.optString("id", UUID.randomUUID().toString()),
                json.optString("title", ""),
                items);
    }
}
