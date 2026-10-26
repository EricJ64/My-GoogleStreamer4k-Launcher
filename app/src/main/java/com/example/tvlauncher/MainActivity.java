package com.example.tvlauncher;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
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
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
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

    // Greeting, weather and the "pop" on the tile you are on
    private static final String WEATHER_CITY = "Lisbon";
    private static final String WEATHER_URL =
            "https://api.open-meteo.com/v1/forecast?latitude=38.7223&longitude=-9.1393"
                    + "&current=temperature_2m,weather_code"
                    + "&daily=temperature_2m_max,temperature_2m_min,sunrise,sunset"
                    + "&temperature_unit=fahrenheit&timezone=Europe%2FLisbon&forecast_days=1";
    private static final long WEATHER_EVERY_MS = 30L * 60_000;   // refresh every 30 minutes
    private static final long WEATHER_RETRY_MS = 5L * 60_000;    // try again after 5 minutes if it failed
    private static final float POP_SCALE = 1.10f;
    private TextView greetingView;
    private TextView weatherTemp;
    private TextView weatherDesc;
    private volatile long lastWeatherFetch = 0;
    private TextView weatherExtra;
    private View tintView;
    private int sunriseMin = -1;   // minutes after midnight, from the weather service
    private int sunsetMin = -1;

    // Colour laid over the photo: normal, warm around sunrise and sunset, darker at night
    private static final int TINT_DAY = 0x66000000;
    private static final int TINT_WARM = 0x66331400;
    private static final int TINT_NIGHT = 0x99000610;
    private int currentTint = TINT_DAY;
    private ValueAnimator tintAnim;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateGreeting();
            updateTint();
            if (System.currentTimeMillis() - lastWeatherFetch > WEATHER_EVERY_MS) {
                fetchWeather();
            }
            handler.postDelayed(this, 60_000);
        }
    };

    private void updateGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int text;
        if (hour >= 5 && hour < 12) {
            text = R.string.greeting_morning;
        } else if (hour >= 12 && hour < 17) {
            text = R.string.greeting_afternoon;
        } else if (hour >= 17 && hour < 22) {
            text = R.string.greeting_evening;
        } else {
            text = R.string.greeting_night;
        }
        greetingView.setText(text);
    }

    private void fetchWeather() {
        lastWeatherFetch = System.currentTimeMillis();
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(WEATHER_URL).openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                JSONObject current = new JSONObject(sb.toString()).getJSONObject("current");
                final int temp = (int) Math.round(current.getDouble("temperature_2m"));
                final int code = current.getInt("weather_code");
                JSONObject daily = new JSONObject(sb.toString()).getJSONObject("daily");
                final int high = (int) Math.round(daily.getJSONArray("temperature_2m_max").getDouble(0));
                final int low = (int) Math.round(daily.getJSONArray("temperature_2m_min").getDouble(0));
                final String sunrise = daily.getJSONArray("sunrise").getString(0);
                final String sunset = daily.getJSONArray("sunset").getString(0);
                handler.post(() -> showWeather(temp, code, high, low, sunrise, sunset));
            } catch (Exception e) {
                // No internet or the service is down: keep what is shown and retry soon.
                lastWeatherFetch = System.currentTimeMillis() - (WEATHER_EVERY_MS - WEATHER_RETRY_MS);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    private void showWeather(int temp, int code, int high, int low, String sunrise, String sunset) {
        weatherTemp.setText(temp + "\u00B0F");
        weatherDesc.setText(describeWeather(code) + " \u00B7 " + WEATHER_CITY);
        sunriseMin = minutesOfDay(sunrise);
        sunsetMin = minutesOfDay(sunset);
        weatherExtra.setText("High " + high + "\u00B0 \u00B7 Low " + low + "\u00B0\n"
                + "Sunrise " + prettyTime(sunriseMin) + " \u00B7 Sunset " + prettyTime(sunsetMin));
        updateTint();
    }

    /** Turns "2026-10-26T07:30" into 450 (minutes after midnight). */
    private static int minutesOfDay(String isoTime) {
        try {
            String hhmm = isoTime.substring(isoTime.indexOf('T') + 1);
            return Integer.parseInt(hhmm.substring(0, 2)) * 60 + Integer.parseInt(hhmm.substring(3, 5));
        } catch (Exception e) {
            return -1;
        }
    }

    private String prettyTime(int minutes) {
        if (minutes < 0) {
            return "--";
        }
        int h = minutes / 60;
        int m = minutes % 60;
        if (DateFormat.is24HourFormat(this)) {
            return String.format(java.util.Locale.US, "%02d:%02d", h, m);
        }
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return String.format(java.util.Locale.US, "%d:%02d %s", h12, m, h < 12 ? "AM" : "PM");
    }

    /** Warm tint around sunrise and sunset, darker through the night, normal in the day. */
    private void updateTint() {
        Calendar c = Calendar.getInstance();
        int m = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        int sr = sunriseMin >= 0 ? sunriseMin : 7 * 60;
        int ss = sunsetMin >= 0 ? sunsetMin : 18 * 60;
        int target;
        if (m >= sr - 30 && m < sr + 60) {
            target = TINT_WARM;
        } else if (m >= sr + 60 && m < ss - 60) {
            target = TINT_DAY;
        } else if (m >= ss - 60 && m < ss + 90) {
            target = TINT_WARM;
        } else {
            target = TINT_NIGHT;
        }
        if (target == currentTint) {
            return;
        }
        int from = currentTint;
        currentTint = target;
        if (tintAnim != null) {
            tintAnim.cancel();
        }
        tintAnim = ValueAnimator.ofObject(new ArgbEvaluator(), from, target);
        tintAnim.setDuration(4000);
        tintAnim.addUpdateListener(a -> tintView.setBackgroundColor((int) a.getAnimatedValue()));
        tintAnim.start();
    }

    private static String describeWeather(int code) {
        if (code == 0) return "Clear";
        if (code == 1) return "Mostly clear";
        if (code == 2) return "Partly cloudy";
        if (code == 3) return "Overcast";
        if (code == 45 || code == 48) return "Fog";
        if (code >= 51 && code <= 57) return "Drizzle";
        if (code >= 61 && code <= 67) return "Rain";
        if (code >= 71 && code <= 77) return "Snow";
        if (code >= 80 && code <= 82) return "Showers";
        if (code == 85 || code == 86) return "Snow showers";
        if (code >= 95) return "Thunderstorm";
        return "";
    }

    /** Makes the tile you are on grow slightly; all the others return to normal size. */
    private void applyPop(int selected) {
        int first = grid.getFirstVisiblePosition();
        for (int c = 0; c < grid.getChildCount(); c++) {
            float target = (first + c == selected) ? POP_SCALE : 1f;
            grid.getChildAt(c).animate().scaleX(target).scaleY(target).setDuration(140).start();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bgBack = findViewById(R.id.bg_back);
        bgFront = findViewById(R.id.bg_front);
        cityIndex = new java.util.Random().nextInt(CITIES.length);
        bgBack.setImageResource(CITIES[cityIndex]);

        greetingView = findViewById(R.id.greeting);
        weatherTemp = findViewById(R.id.weather_temp);
        weatherDesc = findViewById(R.id.weather_desc);
        weatherExtra = findViewById(R.id.weather_extra);
        tintView = findViewById(R.id.tint);

        grid = findViewById(R.id.grid);
        adapter = new TileAdapter();
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((parent, view, position, id) -> onTileClicked(tiles.get(position)));
        grid.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyPop(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                applyPop(-1);
            }
        });
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
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop rotating while another app is on screen, to save work.
        handler.removeCallbacks(rotateBackground);
        handler.removeCallbacks(tick);
        if (tintAnim != null) {
            tintAnim.cancel();
        }
        tintView.setBackgroundColor(currentTint);
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
        grid.post(() -> applyPop(grid.getSelectedItemPosition()));
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
            view.animate().cancel();
            view.setScaleX(1f);
            view.setScaleY(1f);
            Tile tile = tiles.get(position);
            ((ImageView) view.findViewById(R.id.icon)).setImageDrawable(tile.icon);
            ((TextView) view.findViewById(R.id.label)).setText(tile.label);
            return view;
        }
    }
}
