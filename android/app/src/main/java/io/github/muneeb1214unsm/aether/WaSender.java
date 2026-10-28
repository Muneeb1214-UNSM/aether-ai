package io.github.muneeb1214unsm.aether;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Sends scheduled WhatsApp messages by itself: opens the chat with the message typed in,
 * and WaAccessibilityService taps Send. Messages wait in a queue until the phone is unlocked,
 * because Android doesn't let apps open WhatsApp over the lock screen.
 */
final class WaSender {
    private static final long STEP_TIMEOUT_MS = 30000;
    private static final int UNLOCK_NOTIFICATION = 4001;
    private static final Handler handler = new Handler(Looper.getMainLooper());

    private WaSender() {}

    static String whatsappPackage(Context c) {
        for (String p : new String[]{"com.whatsapp", "com.whatsapp.w4b"}) {
            try {
                c.getPackageManager().getPackageInfo(p, 0);
                return p;
            } catch (Exception ignored) {}
        }
        return null;
    }

    /** True when the user turned on AETHER's WhatsApp helper in Accessibility settings. */
    static boolean serviceOn(Context c) {
        String on = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (on == null) return false;
        String me = new ComponentName(c, WaAccessibilityService.class).flattenToString();
        for (String s : on.split(":")) if (s.equalsIgnoreCase(me)) return true;
        return false;
    }

    static boolean ready(Context c) {
        return serviceOn(c) && whatsappPackage(c) != null;
    }

    static synchronized void enqueue(Context c, Bundle b) {
        try {
            JSONArray q = new JSONArray(AlarmScheduler.prefs(c).getString("wa_queue", "[]"));
            JSONObject m = new JSONObject();
            m.put("k", b.getString("k", ""));
            m.put("phone", b.getString("phone", ""));
            m.put("text", b.getString("text", ""));
            m.put("name", b.getString("name", "your contact"));
            q.put(m);
            AlarmScheduler.prefs(c).edit().putString("wa_queue", q.toString()).apply();
        } catch (Exception ignored) {}
    }

    static synchronized JSONObject inFlight(Context c) {
        try {
            String s = AlarmScheduler.prefs(c).getString("wa_inflight", "");
            return s.isEmpty() ? null : new JSONObject(s);
        } catch (Exception e) {
            return null;
        }
    }

    /** Starts the next waiting message, if nothing is being sent right now. */
    static synchronized void next(Context c) {
        JSONObject cur = inFlight(c);
        if (cur != null) {
            if (System.currentTimeMillis() - cur.optLong("started") < STEP_TIMEOUT_MS) return;
            finish(c, false);
            return;
        }
        JSONArray q;
        try {
            q = new JSONArray(AlarmScheduler.prefs(c).getString("wa_queue", "[]"));
        } catch (Exception e) {
            q = new JSONArray();
        }
        if (q.length() == 0) return;
        String pkg = whatsappPackage(c);
        if (pkg == null || !serviceOn(c)) return;

        KeyguardManager km = c.getSystemService(KeyguardManager.class);
        JSONObject m = q.optJSONObject(0);
        if (km != null && km.isKeyguardLocked()) {
            notify(c, UNLOCK_NOTIFICATION, "Unlock to send your WhatsApp message",
                    "AETHER will send your message to " + m.optString("name") + " as soon as you unlock your phone.", null);
            return;
        }
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm != null) nm.cancel(UNLOCK_NOTIFICATION);

        JSONArray rest = new JSONArray();
        for (int i = 1; i < q.length(); i++) rest.put(q.opt(i));
        try {
            m.put("started", System.currentTimeMillis());
        } catch (Exception ignored) {}
        AlarmScheduler.prefs(c).edit().putString("wa_queue", rest.toString()).putString("wa_inflight", m.toString()).apply();

        Intent chat = new Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone="
                + m.optString("phone") + "&text=" + Uri.encode(m.optString("text"))))
                .setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            c.startActivity(chat);
            handler.postDelayed(() -> {
                JSONObject still = inFlight(c);
                if (still != null && still.optString("k").equals(m.optString("k"))) finish(c, false);
            }, STEP_TIMEOUT_MS);
        } catch (Exception e) {
            finish(c, false);
        }
    }

    /** Called after Send was tapped (or it couldn't be). Tells the user, records it, and moves on. */
    static synchronized void finish(Context c, boolean sent) {
        JSONObject m = inFlight(c);
        AlarmScheduler.prefs(c).edit().remove("wa_inflight").apply();
        if (m == null) return;
        String name = m.optString("name");
        int id = 4100 + (m.optString("k").hashCode() & 0xFFF);
        if (sent) {
            AlarmScheduler.addEvent(c, "wa-sent", "k", m.optString("k"));
            notify(c, id, "WhatsApp sent to " + name, m.optString("text"), null);
        } else {
            String link = "https://wa.me/" + m.optString("phone") + "?text=" + Uri.encode(m.optString("text"));
            notify(c, id, "Couldn't send to " + name + " automatically", "Tap to open WhatsApp and send it yourself.", link);
        }
        handler.postDelayed(() -> next(c), 2500);
    }

    private static void notify(Context c, int id, String title, String body, String link) {
        AlertService.createChannel(c);
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        Intent open = link != null
                ? new Intent(Intent.ACTION_VIEW, Uri.parse(link)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                : new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(c, id, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, AlertService.QUIET_CHANNEL) : new Notification.Builder(c);
        b.setSmallIcon(R.drawable.ic_stat_aether).setContentTitle(title).setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setContentIntent(pi).setAutoCancel(true).setColor(0xFF16A34A);
        nm.notify(id, b.build());
    }
}
