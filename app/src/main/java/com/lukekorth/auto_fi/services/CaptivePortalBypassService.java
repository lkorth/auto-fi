package com.lukekorth.auto_fi.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.CaptivePortal;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;

import com.lukekorth.auto_fi.interfaces.CaptivePortalWebViewListener;
import com.lukekorth.auto_fi.utilities.Logger;
import com.lukekorth.auto_fi.utilities.VpnHelper;
import com.lukekorth.auto_fi.utilities.WifiHelper;
import com.lukekorth.auto_fi.webview.CaptivePortalWebView;

public class CaptivePortalBypassService extends Service implements CaptivePortalWebViewListener {

    private WifiHelper mWifiHelper;
    private BroadcastReceiver mDisconnectReceiver;
    private CaptivePortal mCaptivePortal;
    private CaptivePortalWebView mWebView;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.info("Attempting to bypass captive portal");

        mWifiHelper = new WifiHelper(this);

        if (intent != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mCaptivePortal = intent.getParcelableExtra(ConnectivityManager.EXTRA_CAPTIVE_PORTAL);
        }

        setupWifiConnectionBroadcastReceiver();
        mWebView = new CaptivePortalWebView(this);
        mWebView.attemptBypass(getApplication(), this);

        return START_NOT_STICKY;
    }

    @Override
    public void onComplete(boolean successfullyBypassed) {
        if (successfullyBypassed) {
            if (mCaptivePortal != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mCaptivePortal.reportCaptivePortalDismissed();
            }
            VpnHelper.startVpn(this);
        } else {
            mWifiHelper.blacklistAndDisconnectFromCurrentWifiNetwork();
        }

        stop();
    }

    private void stop() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mWebView.stopLoading();
            }
        });

        try {
            unregisterReceiver(mDisconnectReceiver);
        } catch (IllegalArgumentException ignored) {}

        mWebView.tearDown();
        stopSelf();
    }

    private void setupWifiConnectionBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mDisconnectReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION) &&
                        !mWifiHelper.isConnectedToWifi()) {
                    stop();
                }
            }
        };
        registerReceiver(mDisconnectReceiver, filter);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
