package com.lukekorth.auto_fi.interfaces;

import android.net.VpnService;

import com.lukekorth.auto_fi.utilities.WifiHelper;

public interface VpnServiceInterface {
    WifiHelper getWifiHelper();
    VpnService.Builder getBuilder();
    boolean protect(int fileDescriptor);
    void successfullyConnected();
    void setNotificationMessage(int message);
    void shutdown();
}
