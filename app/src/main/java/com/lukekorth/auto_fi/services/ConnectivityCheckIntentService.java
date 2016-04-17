package com.lukekorth.auto_fi.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import com.lukekorth.auto_fi.models.WifiNetwork;
import com.lukekorth.auto_fi.utilities.Logger;
import com.lukekorth.auto_fi.utilities.StreamUtils;
import com.lukekorth.auto_fi.utilities.VpnHelper;
import com.lukekorth.auto_fi.utilities.WifiUtilities;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import io.realm.Realm;

import static java.net.HttpURLConnection.HTTP_OK;

public class ConnectivityCheckIntentService extends IntentService {

    public ConnectivityCheckIntentService() {
        super(ConnectivityCheckIntentService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        boolean hasConnectivity = false;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL("http://vpn.ofkorth.net/connectivity-check/").openConnection();
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HTTP_OK) {
                if (StreamUtils.readStream(connection.getInputStream()).contains("E1A304E5-E244-4846-B613-6290055A211D")) {
                    hasConnectivity = true;
                }
            }
        } catch (IOException e) {
            Logger.info("Connectivity check failed. " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        Logger.info("Wifi network has connectivity: " + hasConnectivity);

        if (!hasConnectivity) {
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            int networkId = wifiManager.getConnectionInfo().getNetworkId();
            WifiConfiguration configuration = WifiUtilities.getWifiNetwork(networkId);

            if (configuration != null) {
                WifiNetwork.blacklist(configuration.SSID);
            }

            wifiManager.removeNetwork(networkId);
            wifiManager.saveConfiguration();
            wifiManager.disconnect();
        } else {
            VpnHelper.startVpn(this);
        }
    }
}
