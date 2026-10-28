package io.github.muneeb1214unsm.aether;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Android forgets alarms on restart and app updates, so set them again. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        AlarmScheduler.scheduleAll(c);
    }
}
