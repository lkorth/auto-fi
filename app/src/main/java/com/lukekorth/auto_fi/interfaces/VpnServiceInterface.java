package com.lukekorth.auto_fi.interfaces;

import android.net.VpnService;

public interface VpnServiceInterface {
    VpnService.Builder getBuilder();
    boolean protect(int fileDescriptor);
    void setNotificationMessage(String message);
    void shutdown();
}
