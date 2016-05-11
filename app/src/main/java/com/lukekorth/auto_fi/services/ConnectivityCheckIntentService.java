package com.lukekorth.auto_fi.services;

import android.app.IntentService;
import android.content.Intent;

import com.lukekorth.auto_fi.utilities.ConnectivityUtils;
import com.lukekorth.auto_fi.utilities.VpnHelper;
import com.lukekorth.auto_fi.utilities.WifiUtils;

public class ConnectivityCheckIntentService extends IntentService {

    public static final String EXTRA_ATTEMPT_TO_BYPASS_CAPTIVE_PORTAL =
            "com.lukekorth.auto_fi.EXTRA_ATTEMPT_TO_BYPASS_CAPTIVE_PORTAL";

    public ConnectivityCheckIntentService() {
        super(ConnectivityCheckIntentService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        switch (ConnectivityUtils.checkConnectivity()) {
            case CONNECTED: {
                VpnHelper.startVpn(this);
                break;
            } case REDIRECTED: {
                if (intent.getBooleanExtra(EXTRA_ATTEMPT_TO_BYPASS_CAPTIVE_PORTAL, true)) {
                    startService(new Intent(this, CaptivePortalBypassService.class));
                } else {
                    WifiUtils.blacklistAndDisconnectFromCurrentWifiNetwork(this);
                }
                break;
            } case NO_CONNECTIVITY: {
                WifiUtils.blacklistAndDisconnectFromCurrentWifiNetwork(this);
                break;
            }
        }
    }
}
