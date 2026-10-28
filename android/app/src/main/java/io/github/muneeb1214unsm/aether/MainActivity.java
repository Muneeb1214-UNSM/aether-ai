package io.github.muneeb1214unsm.aether;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/** The AETHER app: the live AETHER site in a full-screen WebView, plus phone alarms and voice. */
public class MainActivity extends Activity {
    static final String APP_URL = "https://aetherai-com.netlify.app/";
    static final int REQ_MIC = 7;
    private static final int REQ_NOTIFY = 8;

    /** True while AETHER is on screen; then the web app handles alerts itself. */
    static volatile boolean inForeground;

    private WebView web;
    private AetherBridge bridge;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        AlertService.createChannel(this);

        web = new WebView(this);
        setContentView(web);
        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setDatabaseEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setUserAgentString(ws.getUserAgentString() + " AetherAndroid/" + version(this));

        bridge = new AetherBridge(this, web);
        web.addJavascriptInterface(bridge, "AetherNative");

        final String polyfill = readAsset("bridge.js");
        final boolean atDocumentStart = WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT);
        if (atDocumentStart) WebViewCompat.addDocumentStartJavaScript(web, polyfill, Collections.singleton("*"));

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return openOutside(request.getUrl());
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (!atDocumentStart) view.evaluateJavascript(polyfill, null);
            }
        });
        web.setWebChromeClient(new WebChromeClient());

        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFY);
        }

        web.loadUrl(startUrl(getIntent()));
        AlarmScheduler.scheduleAll(this);
    }

    /** AETHER pages stay in the app; WhatsApp, social media and other links open in their own apps. */
    private boolean openOutside(Uri uri) {
        String appHost = Uri.parse(APP_URL).getHost();
        if ("https".equals(uri.getScheme()) && appHost != null && appHost.equals(uri.getHost())) return false;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception ignored) {}
        return true;
    }

    /** Opened from an alert: the web app shows it full screen and reads it out loud. */
    private static String startUrl(Intent i) {
        String say = i == null ? null : i.getStringExtra("say");
        if (say == null) return APP_URL;
        String body = i.getStringExtra("sayb");
        return APP_URL + "?say=" + Uri.encode(say) + "&sayb=" + Uri.encode(body == null ? "" : body) + "#alerts";
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        String say = i.getStringExtra("say");
        if (say != null && web != null) {
            String body = i.getStringExtra("sayb");
            web.evaluateJavascript("window.sayAlert&&window.sayAlert(" + JSONObject.quote(say) + "," + JSONObject.quote(body == null ? "" : body) + ")", null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        inForeground = true;
        web.onResume();
        web.resumeTimers();
    }

    @Override
    protected void onPause() {
        inForeground = false;
        web.onPause();
        web.pauseTimers(); // phone alarms take over while AETHER is in the background
        super.onPause();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_MIC) bridge.onMicPermission(results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    protected void onDestroy() {
        if (bridge != null) bridge.destroy();
        if (web != null) web.destroy();
        super.onDestroy();
    }

    static String version(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1";
        }
    }

    private String readAsset(String name) {
        try (InputStream in = getAssets().open(name); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return "";
        }
    }
}
