package com.example.tvlauncher;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/** Remembers which apps you chose, in the order you added them. */
final class Store {

    private static final String FILE = "launcher";
    private static final String KEY_APPS = "apps";
    private static final String KEY_FIRST_RUN_DONE = "first_run_done";

    private Store() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static List<String> getApps(Context c) {
        List<String> out = new ArrayList<>();
        String saved = prefs(c).getString(KEY_APPS, "");
        if (saved == null || saved.isEmpty()) {
            return out;
        }
        for (String pkg : saved.split(",")) {
            if (!pkg.isEmpty() && !out.contains(pkg)) {
                out.add(pkg);
            }
        }
        return out;
    }

    static void setApps(Context c, List<String> apps) {
        StringBuilder sb = new StringBuilder();
        for (String pkg : apps) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(pkg);
        }
        prefs(c).edit().putString(KEY_APPS, sb.toString()).apply();
    }

    static boolean isFirstRunDone(Context c) {
        return prefs(c).getBoolean(KEY_FIRST_RUN_DONE, false);
    }

    static void markFirstRunDone(Context c) {
        prefs(c).edit().putBoolean(KEY_FIRST_RUN_DONE, true).apply();
    }
}
