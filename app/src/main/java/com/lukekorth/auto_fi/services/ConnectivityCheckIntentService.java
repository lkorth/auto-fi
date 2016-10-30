package com.lukekorth.auto_fi.services;

import android.app.IntentService;
import android.content.Intent;

import com.lukekorth.auto_fi.models.Settings;
import com.lukekorth.auto_fi.utilities.ConnectivityUtils;
import com.lukekorth.auto_fi.utilities.VpnHelper;
import com.lukekorth.auto_fi.utilities.WifiHelper;

public class ConnectivityCheckIntentService extends IntentService {

    public static boolean sIsRunning = false;

    public ConnectivityCheckIntentService() {
        super(ConnectivityCheckIntentService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        sIsRunning = true;

        switch (ConnectivityUtils.checkConnectivity(this)) {
            case CONNECTED: {
                if (Settings.autoConnectToVpn(this)) {
                    VpnHelper.startVpn(this);
                }
                break;
            } case REDIRECTED: {
                startService(new Intent(this, CaptivePortalBypassService.class));
                break;
            } case NO_CONNECTIVITY: {
                new WifiHelper(this).blacklistAndDisconnectFromCurrentWifiNetwork();
                break;
            }
        }

        new WifiHelper(this).cleanupSavedWifiNetworks();

        sIsRunning = false;
    }
}
