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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.lukekorth.auto_fi.models.WifiNetwork;

import java.util.List;

import io.realm.Realm;

public class WifiHelper {

    private ConnectivityManager mConnectivityManager;
    private WifiManager mWifiManager;

    public WifiHelper(Context context) {
        context = context.getApplicationContext();
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
        if (Version.isAtLeastLollipop()) {
            return getLollipopWifiNetwork() != null;
        } else {
            NetworkInfo networkInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return networkInfo != null && networkInfo.isConnectedOrConnecting();
        }
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
    @Nullable
    public Network getLollipopWifiNetwork() {
        if (!Version.isAtLeastLollipop()) {
            return null;
        }

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

    @NonNull
    public String getCurrentNetworkName() {
        WifiConfiguration configuration = getCurrentNetwork();
        if (configuration != null) {
            return configuration.SSID.replace("\"", "");
        }

        return "NO_CONNECTED_NETWORK";
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

        WifiNetwork.setAllAutoConnectedNetworksDisconnected();
        cleanupSavedWifiNetworks();
    }

    public void blacklistAndDisconnectFromCurrentWifiNetwork() {
        WifiConfiguration configuration = getCurrentNetwork();
        if (configuration != null && WifiNetwork.isAutoconnectedNetwork(configuration)) {
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

    @SuppressWarnings("deprecation")
    @Nullable
    public Network bindToCurrentNetwork() {
        Network network = getLollipopWifiNetwork();
        if (network != null) {
            if (Version.isAtLeastMarshmallow()) {
                getConnectivityManager().bindProcessToNetwork(network);
            } else if (Version.isAtLeastLollipop()) {
                ConnectivityManager.setProcessDefaultNetwork(network);
            }
        }

        return network;
    }

    @SuppressWarnings("deprecation")
    public void unbindFromCurrentNetwork() {
        if (Version.isAtLeastMarshmallow()) {
            getConnectivityManager().bindProcessToNetwork(null);
        } else if (Version.isAtLeastLollipop()) {
            try {
                ConnectivityManager.setProcessDefaultNetwork(null);
            } catch (IllegalStateException ignored) {}
        }
    }
}
