package com.lukekorth.auto_fi.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lukekorth.auto_fi.models.Settings;
import com.lukekorth.auto_fi.utilities.Logger;
import com.lukekorth.auto_fi.utilities.WifiHelper;

import java.util.concurrent.TimeUnit;

public class UserPresentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Settings.autoConnectToWifi(context)) {
            return;
        }

        WifiHelper wifiHelper = new WifiHelper(context);
        if (!wifiHelper.isConnectedToWifi()) {
            long lastScan = WifiScanReceiver.getLastScan(context);
            if (System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5) > lastScan) {
                Logger.debug("Requesting wifi scan after unlock");
                wifiHelper.getWifiManager().startScan();
            }
        }
    }
}
