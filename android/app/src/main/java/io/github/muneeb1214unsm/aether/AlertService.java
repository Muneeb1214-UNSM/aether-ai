package io.github.muneeb1214unsm.aether;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.Locale;

/**
 * Shows the alert (sound, vibration, lock screen) and reads it out loud with the phone's
 * text-to-speech, even when AETHER is closed and the phone is locked.
 */
public class AlertService extends Service implements TextToSpeech.OnInitListener {
    static final String CHANNEL = "aether_alerts_v1";
    static final String QUIET_CHANNEL = "aether_status_v1";
    private static final long SPEAK_AFTER_MS = 1800; // let the alert sound play first
    private static final long MAX_RUN_MS = 45000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextToSpeech tts;
    private boolean ttsReady;
    private String speech;

    static void createChannel(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        if (nm.getNotificationChannel(QUIET_CHANNEL) == null) {
            nm.createNotificationChannel(new NotificationChannel(QUIET_CHANNEL, c.getString(R.string.channel_quiet), NotificationManager.IMPORTANCE_DEFAULT));
        }
        if (nm.getNotificationChannel(CHANNEL) != null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, c.getString(R.string.channel_alerts), NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription(c.getString(R.string.channel_alerts_desc));
        ch.enableVibration(true);
        ch.setVibrationPattern(new long[]{0, 500, 200, 500, 200, 800});
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        // Alarm audio plays at alarm volume, so alerts are heard even when media volume is low.
        ch.setSound(sound, new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
        nm.createNotificationChannel(ch);
    }

    static int notificationId(Intent i) {
        String k = i.getStringExtra("k");
        return 2000 + ((k == null ? "alert" : k).hashCode() & 0xFFFF);
    }

    static Notification build(Context c, Intent i, int nid) {
        String title = i.getStringExtra("title");
        if (title == null || title.isEmpty()) title = "AETHER alert";
        String body = i.getStringExtra("body");
        if (body == null) body = "";
        String url = i.getStringExtra("url");
        if (url == null) url = "";

        Intent open = new Intent(c, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra("say", title).putExtra("sayb", body);
        PendingIntent openPi = PendingIntent.getActivity(c, nid, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String kind = i.getStringExtra("kind");
        boolean isTask = ("task".equals(kind) || "taskpre".equals(kind)) && i.getStringExtra("tid") != null;
        boolean autoWa = i.getBooleanExtra("auto", false);
        PendingIntent snoozePi = button(c, i, AlarmScheduler.ACTION_SNOOZE, nid, 1);
        PendingIntent donePi = button(c, i, AlarmScheduler.ACTION_DONE, nid, 2);

        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CHANNEL) : new Notification.Builder(c);
        b.setSmallIcon(R.drawable.ic_stat_aether)
                .setContentTitle(title)
                .setContentText(body.isEmpty() ? "Tap to open AETHER" : body)
                .setStyle(new Notification.BigTextStyle().bigText(body.isEmpty() ? "Tap to open AETHER" : body))
                .setContentIntent(openPi)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setColor(0xFF818CF8);
        if (autoWa) {
            // Being sent by itself: just say so.
            b.setContentText("Sending it for you now…");
        } else if (isTask) {
            // Your choice: finish it, push it 5 minutes, or delete it.
            b.addAction(new Notification.Action.Builder((Icon) null, "Done", button(c, i, AlarmScheduler.ACTION_TASK_DONE, nid, 3)).build());
            b.addAction(new Notification.Action.Builder((Icon) null, "Snooze 5 min", snoozePi).build());
            b.addAction(new Notification.Action.Builder((Icon) null, "Delete task", button(c, i, AlarmScheduler.ACTION_TASK_DELETE, nid, 4)).build());
        } else {
            if (url.startsWith("https://wa.me/")) {
                PendingIntent waPi = PendingIntent.getActivity(c, nid + 300000,
                        new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                b.addAction(new Notification.Action.Builder((Icon) null, "Send on WhatsApp", waPi).build());
            }
            b.addAction(new Notification.Action.Builder((Icon) null, "Snooze 5 min", snoozePi).build());
            b.addAction(new Notification.Action.Builder((Icon) null, "Got it", donePi).build());
        }
        if (Build.VERSION.SDK_INT < 26) b.setPriority(Notification.PRIORITY_MAX).setDefaults(Notification.DEFAULT_ALL);
        return b.build();
    }

    /** A notification button that carries everything about the alert (so Snooze can repeat it). */
    private static PendingIntent button(Context c, Intent alarm, String action, int nid, int slot) {
        Intent b = new Intent(c, AlarmReceiver.class).setAction(action).putExtra("nid", nid);
        if (alarm.getExtras() != null) b.putExtras(alarm.getExtras());
        b.putExtra("nid", nid).removeExtra("auto");
        return PendingIntent.getBroadcast(c, nid + slot * 100000, b, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Fallback when the service can't start: show the alert without speaking. */
    static void showOnly(Context c, Intent i) {
        createChannel(c);
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        int nid = notificationId(i);
        if (nm != null) nm.notify(nid, build(c, i, nid));
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        if (i == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        createChannel(this);
        int nid = notificationId(i);
        Notification n = build(this, i, nid);
        if (Build.VERSION.SDK_INT >= 29) startForeground(nid, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        else startForeground(nid, n);

        String title = i.getStringExtra("title");
        if (title == null || title.isEmpty()) title = "AETHER alert";
        String body = i.getStringExtra("body");
        String say = i.getStringExtra("say");
        if (say != null && !say.isEmpty()) {
            // A full sentence from the web app, like "Muneeb, it's time for Gym."
            speech = title.startsWith("Snoozed: ") ? "Reminder again. " + say : say;
        } else {
            boolean sayBody = body != null && !body.isEmpty() && !body.contains("AETHER alert") && !body.startsWith("Tap");
            speech = "Alert. " + title.replace("Snoozed: ", "") + (sayBody ? ". " + body : "");
        }

        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::finish, MAX_RUN_MS);
        if (tts == null) tts = new TextToSpeech(this, this);
        else if (ttsReady) speakSoon();
        return START_NOT_STICKY;
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS || tts == null) {
            finish();
            return;
        }
        int lang = tts.setLanguage(Locale.US);
        if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) tts.setLanguage(Locale.getDefault());
        tts.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build());
        tts.setSpeechRate(0.95f);
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) {}
            @Override public void onDone(String id) { if ("aether-2".equals(id)) handler.post(AlertService.this::finish); }
            @Override public void onError(String id) { handler.post(AlertService.this::finish); }
        });
        ttsReady = true;
        speakSoon();
    }

    /** Says the alert twice, with a short pause, after the alert sound. */
    private void speakSoon() {
        handler.postDelayed(() -> {
            if (tts == null || speech == null) return;
            tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null, "aether-1");
            tts.playSilentUtterance(1200, TextToSpeech.QUEUE_ADD, "aether-gap");
            tts.speak(speech, TextToSpeech.QUEUE_ADD, null, "aether-2");
        }, SPEAK_AFTER_MS);
    }

    /** Stops speaking but leaves the alert on screen with its buttons. */
    private void finish() {
        handler.removeCallbacksAndMessages(null);
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_DETACH);
        else stopForeground(false);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }
}
