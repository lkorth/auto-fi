package com.lukekorth.auto_fi.services;

import android.app.IntentService;
import android.content.Intent;

import com.google.firebase.analytics.FirebaseAnalytics;
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
                FirebaseAnalytics.getInstance(this).logEvent("connectivity_connected", null);
                break;
            } case REDIRECTED: {
                startService(new Intent(this, CaptivePortalBypassService.class));
                FirebaseAnalytics.getInstance(this).logEvent("connectivity_redirected", null);
                break;
            } case NO_CONNECTIVITY: {
                new WifiHelper(this).blacklistAndDisconnectFromCurrentWifiNetwork();
                FirebaseAnalytics.getInstance(this).logEvent("connectivity_none", null);
                break;
            }
        }

        new WifiHelper(this).cleanupSavedWifiNetworks();

        sIsRunning = false;
    }
}
