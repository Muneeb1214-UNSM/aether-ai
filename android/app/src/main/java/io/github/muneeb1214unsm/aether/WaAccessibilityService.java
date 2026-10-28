package io.github.muneeb1214unsm.aether;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONObject;

import java.util.List;

/**
 * AETHER's WhatsApp helper. It only looks at WhatsApp, and only acts while AETHER is sending
 * one of your scheduled messages: it taps Send, then goes back to the home screen.
 */
public class WaAccessibilityService extends AccessibilityService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean registered;

    private final BroadcastReceiver unlocked = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            WaSender.next(c); // messages that waited for the phone to be unlocked
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        IntentFilter f = new IntentFilter(Intent.ACTION_USER_PRESENT);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(unlocked, f, Context.RECEIVER_EXPORTED);
        else registerReceiver(unlocked, f);
        registered = true;
        WaSender.next(this);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        JSONObject m = WaSender.inFlight(this);
        if (m == null || e == null || e.getPackageName() == null) return;
        String pkg = e.getPackageName().toString();
        if (!pkg.startsWith("com.whatsapp")) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        AccessibilityNodeInfo send = null;
        List<AccessibilityNodeInfo> byId = root.findAccessibilityNodeInfosByViewId(pkg + ":id/send");
        if (byId != null && !byId.isEmpty()) send = byId.get(0);
        if (send == null) {
            List<AccessibilityNodeInfo> byText = root.findAccessibilityNodeInfosByText("Send");
            if (byText != null) {
                for (AccessibilityNodeInfo n : byText) {
                    CharSequence d = n.getContentDescription();
                    if (d != null && "Send".contentEquals(d) && n.isClickable()) {
                        send = n;
                        break;
                    }
                }
            }
        }
        if (send == null) return;
        if (send.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            WaSender.finish(this, true);
            handler.postDelayed(() -> performGlobalAction(GLOBAL_ACTION_HOME), 1200);
        }
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        if (registered) {
            try {
                unregisterReceiver(unlocked);
            } catch (Exception ignored) {}
        }
        super.onDestroy();
    }
}
