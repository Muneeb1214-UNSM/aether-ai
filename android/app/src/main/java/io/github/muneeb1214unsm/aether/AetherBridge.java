package io.github.muneeb1214unsm.aether;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

/**
 * What the web app can ask the phone to do, as window.AetherNative:
 * set alarms, speak with the phone's voice and listen with the phone's speech recognition.
 * (Android's WebView has no speech features of its own; bridge.js wires these up as the
 * standard speechSynthesis and SpeechRecognition the web app already uses.)
 */
final class AetherBridge {
    private final MainActivity act;
    private final WebView web;

    private TextToSpeech tts;
    private boolean ttsReady;
    private boolean ttsFailed;
    private final ArrayList<Object[]> pendingSpeech = new ArrayList<>();

    private SpeechRecognizer recognizer;
    private Object[] pendingListen;

    AetherBridge(MainActivity act, WebView web) {
        this.act = act;
        this.web = web;
    }

    // ---------- alarms ----------

    @JavascriptInterface
    public boolean isAvailable() {
        return true;
    }

    @JavascriptInterface
    public String appVersion() {
        return MainActivity.version(act);
    }

    @JavascriptInterface
    public int scheduleAlarms(String json) {
        AlarmScheduler.saveItems(act, json);
        return AlarmScheduler.scheduleAll(act);
    }

    @JavascriptInterface
    public boolean canExactAlarms() {
        return AlarmScheduler.canExact(act);
    }

    @JavascriptInterface
    public boolean notificationsAllowed() {
        NotificationManager nm = act.getSystemService(NotificationManager.class);
        return nm == null || nm.areNotificationsEnabled();
    }

    @JavascriptInterface
    public void openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < 31) return;
        act.runOnUiThread(() -> {
            try {
                act.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + act.getPackageName())));
            } catch (Exception ignored) {}
        });
    }

    @JavascriptInterface
    public void openNotificationSettings() {
        act.runOnUiThread(() -> {
            try {
                Intent i;
                if (Build.VERSION.SDK_INT >= 26) {
                    i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, act.getPackageName());
                } else {
                    i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + act.getPackageName()));
                }
                act.startActivity(i);
            } catch (Exception ignored) {}
        });
    }

    /** Rings like a real alert in 10 seconds, so you can lock the phone and listen. */
    @JavascriptInterface
    public void testAlarm() {
        long now = System.currentTimeMillis();
        AlarmScheduler.addItem(act, "test-" + now, now + 10000, "AETHER test alert",
                "This is how alerts sound when the app is closed.", "", true);
        AlarmScheduler.scheduleAll(act);
    }

    // ---------- speaking (inside the app) ----------

    @JavascriptInterface
    public void speak(String text, float rate, String id) {
        act.runOnUiThread(() -> {
            if (ttsFailed) {
                spoken(id);
                return;
            }
            if (tts == null) {
                tts = new TextToSpeech(act, status -> {
                    ttsReady = status == TextToSpeech.SUCCESS;
                    ttsFailed = !ttsReady;
                    if (ttsReady) {
                        int lang = tts.setLanguage(Locale.US);
                        if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) tts.setLanguage(Locale.getDefault());
                        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                            @Override public void onStart(String uid) {}
                            @Override public void onDone(String uid) { spoken(uid); }
                            @Override public void onError(String uid) { spoken(uid); }
                            @Override public void onStop(String uid, boolean interrupted) { spoken(uid); }
                        });
                    }
                    for (Object[] p : pendingSpeech) {
                        if (ttsReady) say((String) p[0], (Float) p[1], (String) p[2]);
                        else spoken((String) p[2]);
                    }
                    pendingSpeech.clear();
                });
            }
            if (!ttsReady) pendingSpeech.add(new Object[]{text, rate, id});
            else say(text, rate, id);
        });
    }

    private void say(String text, float rate, String id) {
        tts.setSpeechRate(rate > 0 ? rate : 1f);
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, id);
    }

    @JavascriptInterface
    public void stopSpeaking() {
        act.runOnUiThread(() -> {
            if (tts != null) tts.stop();
        });
    }

    private void spoken(String id) {
        js("window.__aetherTtsDone&&window.__aetherTtsDone(" + JSONObject.quote(id) + ")");
    }

    // ---------- listening ----------

    @JavascriptInterface
    public void startListening(String lang, boolean interim, String id) {
        act.runOnUiThread(() -> {
            if (act.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                pendingListen = new Object[]{lang, interim, id};
                act.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MainActivity.REQ_MIC);
                return;
            }
            listen(lang, interim, id);
        });
    }

    void onMicPermission(boolean granted) {
        Object[] p = pendingListen;
        pendingListen = null;
        if (p == null) return;
        if (granted) listen((String) p[0], (Boolean) p[1], (String) p[2]);
        else {
            heard((String) p[2], "error", "not-allowed", null);
            heard((String) p[2], "end", null, null);
        }
    }

    private void listen(String lang, boolean interim, String id) {
        if (!SpeechRecognizer.isRecognitionAvailable(act)) {
            heard(id, "error", "service-not-allowed", null);
            heard(id, "end", null, null);
            return;
        }
        if (recognizer != null) recognizer.destroy();
        recognizer = SpeechRecognizer.createSpeechRecognizer(act);
        recognizer.setRecognitionListener(new RecognitionListener() {
            private boolean ended;

            private void end() {
                if (ended) return;
                ended = true;
                heard(id, "end", null, null);
            }

            @Override public void onReadyForSpeech(Bundle b) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float v) {}
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onEvent(int type, Bundle b) {}

            @Override
            public void onError(int e) {
                String code;
                if (e == SpeechRecognizer.ERROR_NO_MATCH || e == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) code = "no-speech";
                else if (e == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) code = "not-allowed";
                else if (e == SpeechRecognizer.ERROR_NETWORK || e == SpeechRecognizer.ERROR_NETWORK_TIMEOUT) code = "network";
                else code = "aborted";
                heard(id, "error", code, null);
                end();
            }

            @Override
            public void onResults(Bundle b) {
                String t = firstResult(b);
                if (t != null) heard(id, "final", null, t);
                end();
            }

            @Override
            public void onPartialResults(Bundle b) {
                String t = firstResult(b);
                if (t != null && interim) heard(id, "partial", null, t);
            }
        });
        Intent in = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        in.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        in.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang == null || lang.isEmpty() ? "en-US" : lang);
        in.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, interim);
        in.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        in.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, act.getPackageName());
        recognizer.startListening(in);
    }

    @JavascriptInterface
    public void stopListening() {
        act.runOnUiThread(() -> {
            if (recognizer != null) recognizer.stopListening();
        });
    }

    private static String firstResult(Bundle b) {
        ArrayList<String> list = b == null ? null : b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    private void heard(String id, String type, String error, String text) {
        try {
            JSONObject o = new JSONObject();
            o.put("type", type);
            if (error != null) o.put("error", error);
            if (text != null) o.put("text", text);
            js("window.__aetherSR&&window.__aetherSR(" + JSONObject.quote(id) + "," + o + ")");
        } catch (Exception ignored) {}
    }

    private void js(String code) {
        act.runOnUiThread(() -> web.evaluateJavascript(code, null));
    }

    void destroy() {
        if (tts != null) tts.shutdown();
        if (recognizer != null) recognizer.destroy();
    }
}
