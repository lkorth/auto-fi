package com.lukekorth.auto_fi.utilities;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.Nullable;

import com.lukekorth.auto_fi.models.WifiNetwork;

import java.util.List;

import io.realm.Realm;

public class WifiHelper {

    private ConnectivityManager mConnectivityManager;
    private WifiManager mWifiManager;

    public WifiHelper(Context context) {
        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    public ConnectivityManager getConnectivityManager() {
        return mConnectivityManager;
    }

    public WifiManager getWifiManager() {
        return mWifiManager;
    }

    public boolean isConnectedToWifi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (Network network : mConnectivityManager.getAllNetworks()) {
                NetworkInfo networkInfo = mConnectivityManager.getNetworkInfo(network);
                if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    return true;
                }
            }
        } else {
            NetworkInfo networkInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return networkInfo != null && networkInfo.isConnectedOrConnecting();
        }

        return false;
    }

    @Nullable
    public WifiConfiguration getWifiNetwork(@Nullable WifiInfo wifiInfo) {
        if (wifiInfo != null) {
            List<WifiConfiguration> wifiConfigurationList = mWifiManager.getConfiguredNetworks();
            if (wifiConfigurationList != null) {
                for (WifiConfiguration configuration : mWifiManager.getConfiguredNetworks()) {
                    if (configuration.networkId == wifiInfo.getNetworkId()) {
                        return configuration;
                    }
                }
            }
        }

        return null;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Network getLollipopWifiNetwork() {
        for (Network network : mConnectivityManager.getAllNetworks()) {
            NetworkInfo networkInfo = mConnectivityManager.getNetworkInfo(network);
            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                return network;
            }
        }

        return null;
    }

    @Nullable
    public WifiConfiguration getCurrentNetwork() {
        return getWifiNetwork(mWifiManager.getConnectionInfo());
    }

    public boolean isWifiUnsecured(@Nullable WifiConfiguration wifiConfiguration) {
        return wifiConfiguration != null && wifiConfiguration.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE);

    }

    public void disconnectFromCurrentWifiNetwork() {
        WifiConfiguration configuration = getCurrentNetwork();
        if (configuration != null) {
            mWifiManager.removeNetwork(configuration.networkId);
            mWifiManager.saveConfiguration();
            mWifiManager.disconnect();
        }
    }

    public void blacklistAndDisconnectFromCurrentWifiNetwork() {
        WifiConfiguration configuration = getCurrentNetwork();
        if (configuration != null) {
            Logger.info("Blacklisting " + configuration.SSID);
            WifiNetwork.blacklist(configuration.SSID);
            disconnectFromCurrentWifiNetwork();
        }
    }

    public void cleanupSavedWifiNetworks() {
        Logger.debug("Cleaning up saved wifi networks");

        Realm realm = Realm.getDefaultInstance();
        List<WifiConfiguration> wifiConfigurationList = mWifiManager.getConfiguredNetworks();
        if (wifiConfigurationList != null) {
            for (WifiConfiguration configuration : wifiConfigurationList) {
                if (configuration.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE)) {
                    if (WifiNetwork.find(realm, configuration.SSID) != null) {
                        WifiConfiguration currentNetwork = getCurrentNetwork();
                        if (currentNetwork == null || configuration.networkId != currentNetwork.networkId) {
                            mWifiManager.removeNetwork(configuration.networkId);
                        }
                    }
                }
            }
            mWifiManager.saveConfiguration();
        }

        realm.close();
    }
}
