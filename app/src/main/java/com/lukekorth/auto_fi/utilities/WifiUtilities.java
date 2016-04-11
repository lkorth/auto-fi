package com.lukekorth.auto_fi.utilities;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.annotation.Nullable;

import com.lukekorth.auto_fi.AutoFiApplication;

public class WifiUtilities {

    @Nullable
    public static WifiConfiguration getWifiNetwork(int networkId) {
        WifiManager wifiManager = (WifiManager) AutoFiApplication.getContext().getSystemService(Context.WIFI_SERVICE);
        for (WifiConfiguration configuration : wifiManager.getConfiguredNetworks()) {
            if (configuration.networkId == networkId) {
                return configuration;
            }
        }

        return null;
    }

}
