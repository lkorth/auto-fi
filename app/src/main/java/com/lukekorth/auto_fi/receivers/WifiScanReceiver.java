package com.lukekorth.auto_fi.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import com.lukekorth.auto_fi.models.Settings;
import com.lukekorth.auto_fi.models.WifiNetwork;
import com.lukekorth.auto_fi.utilities.Logger;
import com.lukekorth.auto_fi.utilities.VpnHelper;
import com.lukekorth.auto_fi.utilities.WifiUtilities;

import java.util.List;

public class WifiScanReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Settings.isEnabled(context) && VpnHelper.isVpnEnabled(context) &&
                intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

            List<ScanResult> scanResults = wifiManager.getScanResults();
            if (!WifiUtilities.isConnectedToWifi(context) && scanResults.size() > 0) {
                ScanResult selectedNetwork = null;
                for (ScanResult scanResult : wifiManager.getScanResults()) {
                    if (isNetworkUnsecured(scanResult)) {
                        if (selectedNetwork == null) {
                            selectedNetwork = scanResult;
                        } else if (WifiManager.compareSignalLevel(scanResult.level, selectedNetwork.level) > 0) {
                            selectedNetwork = scanResult;
                        }
                    }
                }

                if (selectedNetwork != null) {
                    Logger.debug("Found network " + selectedNetwork.SSID + " nearby");

                    boolean configured = false;
                    List<WifiConfiguration> wifiConfigurationList = wifiManager.getConfiguredNetworks();
                    if (wifiConfigurationList == null) {
                        return;
                    }

                    for (WifiConfiguration configuredNetwork : wifiConfigurationList) {
                        if (configuredNetwork.SSID.equals(selectedNetwork.SSID)) {
                            Logger.debug("Network " + selectedNetwork.SSID + " is already configured.");
                            configured = true;
                        }
                    }

                    String ssid = "\"" + selectedNetwork.SSID + "\"";
                    if (WifiNetwork.isBlacklisted(ssid)) {
                        Logger.info(selectedNetwork.SSID + " is blacklisted");
                    } else if (!configured) {
                        WifiConfiguration configuration = new WifiConfiguration();
                        configuration.SSID = ssid;
                        configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                        int networkId = wifiManager.addNetwork(configuration);
                        wifiManager.enableNetwork(networkId, true);
                        wifiManager.saveConfiguration();
                        wifiManager.reconnect();
                    }
                }
            }
        }
    }

    private boolean isNetworkUnsecured(ScanResult result) {
        return !(result.capabilities.contains("WEP") || result.capabilities.contains("PSK") ||
                result.capabilities.contains("EAP"));
    }
}
