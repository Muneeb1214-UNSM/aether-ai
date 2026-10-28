package io.github.muneeb1214unsm.aether;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** An alarm went off, or the user tapped Snooze / Got it on an alert. */
public class AlarmReceiver extends BroadcastReceiver {
    private static final long SNOOZE_MS = 5 * 60 * 1000L;

    @Override
    public void onReceive(Context c, Intent i) {
        String action = i.getAction();

        if (AlarmScheduler.ACTION_SNOOZE.equals(action) || AlarmScheduler.ACTION_DONE.equals(action)) {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm != null) nm.cancel(i.getIntExtra("nid", 0));
            c.stopService(new Intent(c, AlertService.class));
            if (AlarmScheduler.ACTION_SNOOZE.equals(action)) {
                String title = i.getStringExtra("title");
                if (title == null || title.isEmpty()) title = "AETHER alert";
                if (!title.startsWith("Snoozed: ")) title = "Snoozed: " + title;
                long now = System.currentTimeMillis();
                AlarmScheduler.addItem(c, "snooze-" + now, now + SNOOZE_MS, title,
                        i.getStringExtra("body"), i.getStringExtra("url"), false);
                AlarmScheduler.scheduleAll(c);
            }
            return;
        }

        if (!AlarmScheduler.ACTION_ALARM.equals(action)) return;
        boolean force = i.getBooleanExtra("force", false);
        if (MainActivity.inForeground && !force) {
            // The app is open on screen: it shows the alert and reads it out itself.
            AlarmScheduler.scheduleAll(c);
            return;
        }
        Intent service = new Intent(c, AlertService.class).putExtras(i);
        try {
            if (android.os.Build.VERSION.SDK_INT >= 26) c.startForegroundService(service);
            else c.startService(service);
        } catch (Exception e) {
            AlertService.showOnly(c, i);
        }
        AlarmScheduler.scheduleAll(c);
    }
}
