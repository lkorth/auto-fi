package com.lukekorth.auto_fi.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import com.google.firebase.analytics.FirebaseAnalytics;

import static com.lukekorth.auto_fi.services.VpnService.DISCONNECT_VPN_INTENT_ACTION;

public class DeviceIdleReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"android.os.action.DEVICE_IDLE_MODE_CHANGED".equals(intent.getAction())) {
            return;
        }

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager.isDeviceIdleMode()) {
            context.sendBroadcast(new Intent(DISCONNECT_VPN_INTENT_ACTION));
            FirebaseAnalytics.getInstance(context).logEvent("disconnect_for_device_idle", null);
        }
    }
}
