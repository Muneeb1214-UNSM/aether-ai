package io.github.muneeb1214unsm.aether;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Keeps the upcoming alerts sent by the web app and turns the next ones into exact Android alarms. */
final class AlarmScheduler {
    static final String ACTION_ALARM = "io.github.muneeb1214unsm.aether.ALARM";
    static final String ACTION_SNOOZE = "io.github.muneeb1214unsm.aether.SNOOZE";
    static final String ACTION_DONE = "io.github.muneeb1214unsm.aether.DONE";
    private static final String PREFS = "aether";
    private static final int MAX_ALARMS = 60;
    private static final int REQ_BASE = 1000;

    private AlarmScheduler() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Replaces the list from the web app, keeping snoozes and tests that only exist on the phone. */
    static synchronized void saveItems(Context c, String json) {
        long now = System.currentTimeMillis();
        JSONArray merged = new JSONArray();
        try {
            JSONArray fresh = new JSONArray(json);
            for (int i = 0; i < fresh.length(); i++) merged.put(fresh.get(i));
        } catch (Exception ignored) {}
        try {
            JSONArray old = new JSONArray(prefs(c).getString("items", "[]"));
            for (int i = 0; i < old.length(); i++) {
                JSONObject it = old.optJSONObject(i);
                if (it == null) continue;
                String k = it.optString("k");
                if ((k.startsWith("snooze-") || k.startsWith("test-")) && it.optLong("at") > now) merged.put(it);
            }
        } catch (Exception ignored) {}
        prefs(c).edit().putString("items", merged.toString()).apply();
    }

    static synchronized void addItem(Context c, String key, long at, String title, String body, String url, boolean force) {
        try {
            JSONArray arr = new JSONArray(prefs(c).getString("items", "[]"));
            JSONObject it = new JSONObject();
            it.put("k", key);
            it.put("at", at);
            it.put("title", title == null ? "AETHER alert" : title);
            it.put("body", body == null ? "" : body);
            it.put("url", url == null ? "" : url);
            if (force) it.put("force", true);
            arr.put(it);
            prefs(c).edit().putString("items", arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    static boolean canExact(Context c) {
        if (Build.VERSION.SDK_INT < 31) return true;
        AlarmManager am = c.getSystemService(AlarmManager.class);
        return am != null && am.canScheduleExactAlarms();
    }

    /** Cancels the old alarms and sets the next ones. Called after every change, every alarm and every reboot. */
    static synchronized int scheduleAll(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return 0;
        SharedPreferences p = prefs(c);

        int old = p.getInt("count", 0);
        for (int i = 0; i < old; i++) {
            PendingIntent pi = PendingIntent.getBroadcast(c, REQ_BASE + i,
                    new Intent(c, AlarmReceiver.class).setAction(ACTION_ALARM),
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null) {
                am.cancel(pi);
                pi.cancel();
            }
        }

        long now = System.currentTimeMillis();
        List<JSONObject> upcoming = new ArrayList<>();
        JSONArray kept = new JSONArray();
        try {
            JSONArray arr = new JSONArray(p.getString("items", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject it = arr.optJSONObject(i);
                if (it != null && it.optLong("at", 0) > now) {
                    upcoming.add(it);
                    kept.put(it);
                }
            }
        } catch (Exception ignored) {}
        upcoming.sort((a, b) -> Long.compare(a.optLong("at"), b.optLong("at")));

        boolean exact = canExact(c);
        PendingIntent show = PendingIntent.getActivity(c, 1, new Intent(c, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        int n = 0;
        for (JSONObject it : upcoming) {
            if (n >= MAX_ALARMS) break;
            Intent in = new Intent(c, AlarmReceiver.class).setAction(ACTION_ALARM)
                    .putExtra("k", it.optString("k"))
                    .putExtra("title", it.optString("title", "AETHER alert"))
                    .putExtra("body", it.optString("body"))
                    .putExtra("url", it.optString("url"))
                    .putExtra("force", it.optBoolean("force", false));
            PendingIntent pi = PendingIntent.getBroadcast(c, REQ_BASE + n, in,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            long at = it.optLong("at");
            try {
                if (exact) am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, show), pi);
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } catch (SecurityException e) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            }
            n++;
        }
        p.edit().putInt("count", n).putString("items", kept.toString()).apply();
        return n;
    }
}
