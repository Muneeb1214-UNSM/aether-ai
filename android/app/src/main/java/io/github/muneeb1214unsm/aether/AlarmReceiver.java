package io.github.muneeb1214unsm.aether;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import org.json.JSONObject;

/** An alarm went off, or the user tapped a button on an alert (Snooze, Got it, Done, Delete task). */
public class AlarmReceiver extends BroadcastReceiver {
    private static final long SNOOZE_MS = 5 * 60 * 1000L;

    @Override
    public void onReceive(Context c, Intent i) {
        String action = i.getAction();
        if (action == null) return;
        Bundle extras = i.getExtras();

        if (!AlarmScheduler.ACTION_ALARM.equals(action)) {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm != null) nm.cancel(i.getIntExtra("nid", 0));
            c.stopService(new Intent(c, AlertService.class));
            String tid = i.getStringExtra("tid");
            switch (action) {
                case AlarmScheduler.ACTION_SNOOZE: {
                    long now = System.currentTimeMillis();
                    String title = i.getStringExtra("title");
                    if (title == null || title.isEmpty()) title = "AETHER alert";
                    JSONObject again = AlarmScheduler.fromExtras(extras, "snooze-" + now, now + SNOOZE_MS);
                    try {
                        again.put("title", title.startsWith("Snoozed: ") ? title : "Snoozed: " + title);
                    } catch (Exception ignored) {}
                    AlarmScheduler.addItem(c, again);
                    AlarmScheduler.scheduleAll(c);
                    toast(c, "Snoozed for 5 minutes");
                    break;
                }
                case AlarmScheduler.ACTION_TASK_DONE:
                    AlarmScheduler.removeTask(c, tid);
                    AlarmScheduler.addEvent(c, "done", "tid", tid);
                    AlarmScheduler.scheduleAll(c);
                    toast(c, "Marked as done");
                    break;
                case AlarmScheduler.ACTION_TASK_DELETE:
                    AlarmScheduler.removeTask(c, tid);
                    AlarmScheduler.addEvent(c, "delete", "tid", tid);
                    AlarmScheduler.scheduleAll(c);
                    toast(c, "Task deleted");
                    break;
                default:
                    break; // Got it: just close
            }
            return;
        }

        boolean force = i.getBooleanExtra("force", false);
        boolean whatsapp = "wa".equals(i.getStringExtra("kind"));
        if (whatsapp && WaSender.ready(c)) {
            // Send it by itself; the alert just says what is happening.
            WaSender.enqueue(c, extras == null ? new Bundle() : extras);
            WaSender.next(c);
            if (MainActivity.inForeground && !force) {
                AlarmScheduler.scheduleAll(c);
                return;
            }
            Intent service = new Intent(c, AlertService.class).putExtras(i)
                    .putExtra("say", "Sending your WhatsApp message to " + nameOr(i.getStringExtra("name")) + ".")
                    .putExtra("auto", true);
            start(c, service, i);
            AlarmScheduler.scheduleAll(c);
            return;
        }
        if (MainActivity.inForeground && !force) {
            // AETHER is open on screen: it shows the alert and reads it out itself.
            AlarmScheduler.scheduleAll(c);
            return;
        }
        start(c, new Intent(c, AlertService.class).putExtras(i), i);
        AlarmScheduler.scheduleAll(c);
    }

    private static void start(Context c, Intent service, Intent original) {
        try {
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(service);
            else c.startService(service);
        } catch (Exception e) {
            AlertService.showOnly(c, original);
        }
    }

    private static String nameOr(String n) {
        return n == null || n.isEmpty() ? "your contact" : n;
    }

    private static void toast(Context c, String text) {
        try {
            Toast.makeText(c, text, Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {}
    }
}
