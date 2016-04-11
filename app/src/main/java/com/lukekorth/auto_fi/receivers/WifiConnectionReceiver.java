package com.lukekorth.auto_fi.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.lukekorth.auto_fi.services.ConnectivityCheckIntentService;
import com.lukekorth.auto_fi.utilities.Logger;
import com.lukekorth.auto_fi.utilities.WifiUtilities;

public class WifiConnectionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if (networkInfo.isConnected()) {
            WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            WifiConfiguration configuration = WifiUtilities.getWifiNetwork(wifiInfo.getNetworkId());
            if (configuration != null && configuration.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE)) {
                Logger.info("Connected to unsecured wifi network, checking connectivity");
                context.startService(new Intent(context, ConnectivityCheckIntentService.class));
            }
        }
    }
}
