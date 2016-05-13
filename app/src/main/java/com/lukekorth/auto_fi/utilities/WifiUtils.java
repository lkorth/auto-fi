package com.lukekorth.auto_fi.utilities;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.annotation.Nullable;

import com.lukekorth.auto_fi.AutoFiApplication;
import com.lukekorth.auto_fi.models.WifiNetwork;

import java.util.List;

public class WifiUtils {

    public static boolean isConnectedToWifi() {
        ConnectivityManager connectivityManager = (ConnectivityManager) AutoFiApplication.getContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    @Nullable
    public static WifiConfiguration getWifiNetwork(int networkId) {
        WifiManager wifiManager = (WifiManager) AutoFiApplication.getContext().getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> wifiConfigurationList = wifiManager.getConfiguredNetworks();
        if (wifiConfigurationList != null) {
            for (WifiConfiguration configuration : wifiManager.getConfiguredNetworks()) {
                if (configuration.networkId == networkId) {
                    return configuration;
                }
            }
        }

        return null;
    }

    @Nullable
    public static WifiConfiguration getCurrentNetwork() {
        return WifiUtils.getWifiNetwork(getWifiManager().getConnectionInfo().getNetworkId());
    }

    public static void disconnectFromCurrentWifiNetwork() {
        WifiManager wifiManager = getWifiManager();
        WifiConfiguration configuration = getCurrentNetwork();
        if (configuration != null) {
            wifiManager.removeNetwork(configuration.networkId);
            wifiManager.saveConfiguration();
            wifiManager.disconnect();
        }
    }

    public static void blacklistAndDisconnectFromCurrentWifiNetwork() {
        WifiConfiguration configuration = getCurrentNetwork();
        if (configuration != null) {
            Logger.info("Blacklisting " + configuration.SSID);
            WifiNetwork.blacklist(configuration.SSID);
            disconnectFromCurrentWifiNetwork();
        }
    }

    private static WifiManager getWifiManager() {
        return (WifiManager) AutoFiApplication.getContext().getSystemService(Context.WIFI_SERVICE);
    }
}
