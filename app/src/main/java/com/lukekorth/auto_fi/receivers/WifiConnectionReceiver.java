package com.lukekorth.auto_fi.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.lukekorth.auto_fi.models.Settings;
import com.lukekorth.auto_fi.models.WifiNetwork;
import com.lukekorth.auto_fi.services.ConnectivityCheckIntentService;
import com.lukekorth.auto_fi.services.VpnService;
import com.lukekorth.auto_fi.utilities.Logger;
import com.lukekorth.auto_fi.utilities.VpnHelper;
import com.lukekorth.auto_fi.utilities.WifiUtils;

import java.util.List;

import io.realm.Realm;

public class WifiConnectionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (WifiUtils.isConnectedToWifi(context)) {
            WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            if (wifiInfo != null) {
                WifiConfiguration configuration = WifiUtils.getWifiNetwork(wifiInfo.getNetworkId());
                if (configuration != null && configuration.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE)) {
                    if (Settings.isEnabled(context) && VpnHelper.isVpnEnabled(context)) {
                        Logger.info("Connected to unsecured wifi network, checking connectivity");
                        context.startService(new Intent(context, ConnectivityCheckIntentService.class));
                    }
                }
            }
        } else {
            Logger.debug("Disconnected from wifi, sending broadcast to disconnect VPN");
            context.sendBroadcast(new Intent(VpnService.DISCONNECT_VPN_INTENT_ACTION));

            Logger.debug("Cleaning up saved wifi networks");
            Realm realm = Realm.getDefaultInstance();
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            List<WifiConfiguration> wifiConfigurationList = wifiManager.getConfiguredNetworks();
            if (wifiConfigurationList != null) {
                for (WifiConfiguration configuration : wifiConfigurationList) {
                    if (configuration.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE)) {
                        if (WifiNetwork.find(realm, configuration.SSID) != null) {
                            wifiManager.removeNetwork(configuration.networkId);
                        }
                    }
                }
                wifiManager.saveConfiguration();
            }

            realm.close();
        }
    }
}
