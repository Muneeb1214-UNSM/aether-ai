package io.github.muneeb1214unsm.aether;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Keeps the upcoming alerts sent by the web app and turns the next ones into exact Android alarms. */
final class AlarmScheduler {
    static final String ACTION_ALARM = "io.github.muneeb1214unsm.aether.ALARM";
    static final String ACTION_SNOOZE = "io.github.muneeb1214unsm.aether.SNOOZE";
    static final String ACTION_DONE = "io.github.muneeb1214unsm.aether.DONE";
    static final String ACTION_TASK_DONE = "io.github.muneeb1214unsm.aether.TASK_DONE";
    static final String ACTION_TASK_DELETE = "io.github.muneeb1214unsm.aether.TASK_DELETE";
    /** Text fields an alert can carry from the web app to the alarm and its buttons. */
    static final String[] FIELDS = {"k", "title", "body", "url", "say", "kind", "tid", "phone", "text", "name"};
    private static final String PREFS = "aether";
    private static final int MAX_ALARMS = 60;
    private static final int REQ_BASE = 1000;

    private AlarmScheduler() {}

    static SharedPreferences prefs(Context c) {
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

    static synchronized void addItem(Context c, JSONObject it) {
        try {
            JSONArray arr = new JSONArray(prefs(c).getString("items", "[]"));
            arr.put(it);
            prefs(c).edit().putString("items", arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    /** Builds an alert from an alarm's extras, for example to snooze it. */
    static JSONObject fromExtras(Bundle b, String key, long at) {
        JSONObject it = new JSONObject();
        try {
            for (String f : FIELDS) {
                String v = b == null ? null : b.getString(f);
                if (v != null) it.put(f, v);
            }
            it.put("k", key);
            it.put("at", at);
        } catch (Exception ignored) {}
        return it;
    }

    /** Removes every upcoming alarm for a task that was done or deleted from a notification. */
    static synchronized void removeTask(Context c, String tid) {
        if (tid == null || tid.isEmpty()) return;
        try {
            JSONArray arr = new JSONArray(prefs(c).getString("items", "[]"));
            JSONArray kept = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject it = arr.optJSONObject(i);
                if (it != null && !tid.equals(it.optString("tid"))) kept.put(it);
            }
            prefs(c).edit().putString("items", kept.toString()).apply();
        } catch (Exception ignored) {}
    }

    // ---------- events the web app picks up next time it opens ----------

    static synchronized void addEvent(Context c, String type, String key, String value) {
        try {
            JSONArray arr = new JSONArray(prefs(c).getString("events", "[]"));
            JSONObject e = new JSONObject();
            e.put("type", type);
            e.put(key, value);
            e.put("at", System.currentTimeMillis());
            arr.put(e);
            prefs(c).edit().putString("events", arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    static synchronized String takeEvents(Context c) {
        String s = prefs(c).getString("events", "[]");
        prefs(c).edit().putString("events", "[]").apply();
        return s;
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
                    .putExtra("force", it.optBoolean("force", false));
            for (String f : FIELDS) {
                if (it.has(f)) in.putExtra(f, it.optString(f));
            }
            if (!it.has("title")) in.putExtra("title", "AETHER alert");
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
