package com.example.tvlauncher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lists every installed app. Press OK on one to add it to (or remove it from) the home screen. */
public class PickerActivity extends Activity {

    private static class Entry {
        String packageName;
        CharSequence label;
        Drawable icon;
    }

    private final List<Entry> entries = new ArrayList<>();
    private List<String> chosen;
    private PickAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picker);

        chosen = Store.getApps(this);
        loadEntries();

        ListView list = findViewById(R.id.list);
        adapter = new PickAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> toggle(entries.get(position)));
        list.requestFocus();
    }

    private void loadEntries() {
        PackageManager pm = getPackageManager();
        Map<String, Entry> found = new LinkedHashMap<>();
        addApps(pm, Intent.CATEGORY_LEANBACK_LAUNCHER, found);
        addApps(pm, Intent.CATEGORY_LAUNCHER, found);

        entries.clear();
        entries.addAll(found.values());
        Collections.sort(entries, new Comparator<Entry>() {
            @Override
            public int compare(Entry a, Entry b) {
                return a.label.toString().compareToIgnoreCase(b.label.toString());
            }
        });
    }

    private void addApps(PackageManager pm, String category, Map<String, Entry> found) {
        Intent query = new Intent(Intent.ACTION_MAIN);
        query.addCategory(category);
        for (ResolveInfo info : pm.queryIntentActivities(query, 0)) {
            String pkg = info.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || found.containsKey(pkg)) {
                continue;
            }
            Entry entry = new Entry();
            entry.packageName = pkg;
            entry.label = info.loadLabel(pm);
            entry.icon = info.loadIcon(pm);
            found.put(pkg, entry);
        }
    }

    private void toggle(Entry entry) {
        if (chosen.contains(entry.packageName)) {
            chosen.remove(entry.packageName);
        } else {
            chosen.add(entry.packageName);
        }
        Store.setApps(this, chosen);
        adapter.notifyDataSetChanged();
    }

    private class PickAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return entries.size();
        }

        @Override
        public Object getItem(int position) {
            return entries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.item_pick, parent, false);
            }
            Entry entry = entries.get(position);
            ((ImageView) view.findViewById(R.id.icon)).setImageDrawable(entry.icon);
            ((TextView) view.findViewById(R.id.label)).setText(entry.label);
            view.findViewById(R.id.status).setVisibility(
                    chosen.contains(entry.packageName) ? View.VISIBLE : View.INVISIBLE);
            return view;
        }
    }
}
