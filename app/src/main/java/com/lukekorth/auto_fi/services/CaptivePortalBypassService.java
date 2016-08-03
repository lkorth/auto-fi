package com.lukekorth.auto_fi.services;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.CaptivePortal;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.Proxy;
import android.net.ProxyInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.webkit.WebView;

import com.lukekorth.auto_fi.utilities.Logger;
import com.lukekorth.auto_fi.utilities.WifiHelper;
import com.lukekorth.auto_fi.webview.CaptivePortalWebViewClient;
import com.lukekorth.auto_fi.webview.JavascriptLoggingInterface;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CaptivePortalBypassService extends Service {

    private WifiHelper mWifiHelper;
    private BroadcastReceiver mDisconnectReceiver;
    private CaptivePortal mCaptivePortal;
    private WebView mWebView;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.info("Attempting to bypass captive portal");

        mWifiHelper = new WifiHelper(this);

        if (intent != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mCaptivePortal = intent.getParcelableExtra(ConnectivityManager.EXTRA_CAPTIVE_PORTAL);
        }

        setupWifiConnectionBroadcastReceiver();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bindProcessToNetwork();
        }

        try {
            loadWebView();
        } catch (IOException e) {
            stop(false);
        }

        return START_NOT_STICKY;
    }

    public void captivePortalBypassed() {
        if (mCaptivePortal != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mCaptivePortal.reportCaptivePortalDismissed();
        }
    }

    public void stop(boolean blacklistNetwork) {
        if (blacklistNetwork) {
            mWifiHelper.blacklistAndDisconnectFromCurrentWifiNetwork();
        }

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mWebView.stopLoading();
            }
        });

        unregisterReceiver(mDisconnectReceiver);
        unbindProcessFromNetwork();
        stopSelf();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void loadWebView() throws IOException {
        mWebView = new WebView(this);
        mWebView.clearCache(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.addJavascriptInterface(new JavascriptLoggingInterface(), "AutoFi");
        mWebView.setWebViewClient(new CaptivePortalWebViewClient(this, mWebView));
        mWebView.loadData("", "text/html", null);
    }

    private void setupWifiConnectionBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mDisconnectReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION) &&
                        !mWifiHelper.isConnectedToWifi()) {
                    stop(false);
                }
            }
        };
        registerReceiver(mDisconnectReceiver, filter);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void bindProcessToNetwork() {
        try {
            Network network = mWifiHelper.getLollipopWifiNetwork();
            if (network != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mWifiHelper.getConnectivityManager().bindProcessToNetwork(network);
                } else {
                    ConnectivityManager.setProcessDefaultNetwork(network);
                }

                setProxyProperties(network);
            }
        } catch (Exception e) {
            Logger.warn(e.getMessage());
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setProxyProperties(Network network) throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        final LinkProperties linkProperties = mWifiHelper.getConnectivityManager().getLinkProperties(network);
        if (linkProperties != null) {
            ProxyInfo proxyInfo = linkProperties.getHttpProxy();
            String host = "";
            String port = "";
            Uri pacFileUrl = Uri.EMPTY;
            String exclusionList = "";
            if (proxyInfo != null) {
                host = proxyInfo.getHost();
                port = Integer.toString(proxyInfo.getPort());
                pacFileUrl = proxyInfo.getPacFileUrl();

                Method getExclustionListMethod = ProxyInfo.class.getDeclaredMethod("getExclusionList");
                getExclustionListMethod.setAccessible(true);
                exclusionList = (String) getExclustionListMethod.invoke(proxyInfo);
            }

            Method setHttpProxySystemPropertyMethod = Proxy.class.getDeclaredMethod("setHttpProxySystemProperty",
                    String.class, String.class, String.class, Uri.class);
            setHttpProxySystemPropertyMethod.setAccessible(true);
            setHttpProxySystemPropertyMethod.invoke(null, host, port, exclusionList, pacFileUrl);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void unbindProcessFromNetwork() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mWifiHelper.getConnectivityManager().bindProcessToNetwork(null);
        } else {
            try {
                ConnectivityManager.setProcessDefaultNetwork(null);
            } catch (IllegalStateException ignored) {}
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
