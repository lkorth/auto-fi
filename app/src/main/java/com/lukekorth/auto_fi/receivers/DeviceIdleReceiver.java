package com.lukekorth.auto_fi.receivers;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

import com.google.firebase.analytics.FirebaseAnalytics;

import static com.lukekorth.auto_fi.services.VpnService.DISCONNECT_VPN_INTENT_ACTION;

public class DeviceIdleReceiver extends BroadcastReceiver {

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onReceive(Context context, Intent intent) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager.isDeviceIdleMode()) {
            context.sendBroadcast(new Intent(DISCONNECT_VPN_INTENT_ACTION));
            FirebaseAnalytics.getInstance(context).logEvent("disconnect_for_device_idle", null);
        }
    }
}
