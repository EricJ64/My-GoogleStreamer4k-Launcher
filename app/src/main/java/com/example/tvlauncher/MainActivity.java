package com.example.tvlauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** The home screen: a grid of the apps you picked, plus Settings and "Add / remove apps". */
public class MainActivity extends Activity {

    private static final int KIND_APP = 0;
    private static final int KIND_SETTINGS = 1;
    private static final int KIND_PICKER = 2;

    private static class Tile {
        final int kind;
        final String packageName;
        final CharSequence label;
        final Drawable icon;

        Tile(int kind, String packageName, CharSequence label, Drawable icon) {
            this.kind = kind;
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
        }
    }

    private final List<Tile> tiles = new ArrayList<>();
    private GridView grid;
    private TileAdapter adapter;

    // Rotating night-city backgrounds
    private static final int[] CITIES = {
            R.drawable.city_1, R.drawable.city_2, R.drawable.city_3, R.drawable.city_4,
            R.drawable.city_5, R.drawable.city_6, R.drawable.city_7, R.drawable.city_8
    };
    private static final long ROTATE_EVERY_MS = 60_000;   // change picture every minute
    private static final long FADE_MS = 2_500;            // smooth crossfade
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ImageView bgBack;
    private ImageView bgFront;
    private int cityIndex = 0;

    private final Runnable rotateBackground = new Runnable() {
        @Override
        public void run() {
            final int next = (cityIndex + 1) % CITIES.length;
            bgFront.setImageResource(CITIES[next]);
            bgFront.setAlpha(0f);
            bgFront.animate().alpha(1f).setDuration(FADE_MS).withEndAction(() -> {
                cityIndex = next;
                bgBack.setImageResource(CITIES[next]);
                bgFront.setAlpha(0f);
            }).start();
            handler.postDelayed(this, ROTATE_EVERY_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bgBack = findViewById(R.id.bg_back);
        bgFront = findViewById(R.id.bg_front);
        cityIndex = new java.util.Random().nextInt(CITIES.length);
        bgBack.setImageResource(CITIES[cityIndex]);

        grid = findViewById(R.id.grid);
        adapter = new TileAdapter();
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((parent, view, position, id) -> onTileClicked(tiles.get(position)));
        grid.setOnItemLongClickListener((parent, view, position, id) -> {
            Tile tile = tiles.get(position);
            if (tile.kind != KIND_APP) {
                return false;
            }
            showOptions(tile);
            return true;
        });

        // First time only: jump straight to the app chooser.
        if (!Store.isFirstRunDone(this)) {
            Store.markFirstRunDone(this);
            if (Store.getApps(this).isEmpty()) {
                // Start with the Play Store so you can always install more apps.
                List<String> starter = new ArrayList<>();
                starter.add("com.android.vending");
                Store.setApps(this, starter);
            }
            startActivity(new Intent(this, PickerActivity.class));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        rebuildTiles();
        handler.removeCallbacks(rotateBackground);
        handler.postDelayed(rotateBackground, ROTATE_EVERY_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop rotating while another app is on screen, to save work.
        handler.removeCallbacks(rotateBackground);
        bgFront.animate().cancel();
        bgFront.setAlpha(0f);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Home button pressed while already on the home screen: go back to the first tile.
        if (grid != null) {
            grid.setSelection(0);
        }
    }

    @Override
    public void onBackPressed() {
        // A home screen should not close when Back is pressed.
    }

    private void rebuildTiles() {
        PackageManager pm = getPackageManager();
        tiles.clear();

        for (String pkg : Store.getApps(this)) {
            try {
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                tiles.add(new Tile(KIND_APP, pkg, pm.getApplicationLabel(info), pm.getApplicationIcon(info)));
            } catch (PackageManager.NameNotFoundException e) {
                // App was uninstalled: just skip it.
            }
        }

        tiles.add(new Tile(KIND_SETTINGS, null, getString(R.string.tile_settings),
                whiteIcon(android.R.drawable.ic_menu_preferences)));
        tiles.add(new Tile(KIND_PICKER, null, getString(R.string.tile_add_apps),
                whiteIcon(android.R.drawable.ic_menu_add)));

        adapter.notifyDataSetChanged();
        grid.requestFocus();
    }

    private Drawable whiteIcon(int resId) {
        Drawable d = getResources().getDrawable(resId, getTheme()).mutate();
        d.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        return d;
    }

    private void onTileClicked(Tile tile) {
        if (tile.kind == KIND_APP) {
            launchApp(tile.packageName);
        } else if (tile.kind == KIND_SETTINGS) {
            openSettings();
        } else {
            startActivity(new Intent(this, PickerActivity.class));
        }
    }

    /** Shown when you hold OK on an app tile. */
    private void showOptions(final Tile tile) {
        String[] choices = {
                getString(R.string.move_earlier),
                getString(R.string.move_later),
                getString(R.string.remove_app)
        };
        new AlertDialog.Builder(this)
                .setTitle(tile.label)
                .setItems(choices, (dialog, which) -> {
                    List<String> apps = Store.getApps(this);
                    int index = apps.indexOf(tile.packageName);
                    if (index < 0) {
                        return;
                    }
                    if (which == 0 && index > 0) {
                        apps.remove(index);
                        apps.add(index - 1, tile.packageName);
                    } else if (which == 1 && index < apps.size() - 1) {
                        apps.remove(index);
                        apps.add(index + 1, tile.packageName);
                    } else if (which == 2) {
                        apps.remove(index);
                    }
                    Store.setApps(this, apps);
                    rebuildTiles();
                    for (int i = 0; i < tiles.size(); i++) {
                        if (tile.packageName.equals(tiles.get(i).packageName)) {
                            grid.setSelection(i);
                            break;
                        }
                    }
                })
                .show();
    }

    private void launchApp(String pkg) {
        PackageManager pm = getPackageManager();
        Intent intent = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (intent == null) {
            intent = pm.getLaunchIntentForPackage(pkg);
        }
        if (intent == null) {
            Toast.makeText(this, R.string.cannot_open, Toast.LENGTH_SHORT).show();
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.cannot_open, Toast.LENGTH_SHORT).show();
        }
    }

    private void openSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.cannot_open, Toast.LENGTH_SHORT).show();
        }
    }

    private class TileAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return tiles.size();
        }

        @Override
        public Object getItem(int position) {
            return tiles.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.item_app, parent, false);
            }
            Tile tile = tiles.get(position);
            ((ImageView) view.findViewById(R.id.icon)).setImageDrawable(tile.icon);
            ((TextView) view.findViewById(R.id.label)).setText(tile.label);
            return view;
        }
    }
}
