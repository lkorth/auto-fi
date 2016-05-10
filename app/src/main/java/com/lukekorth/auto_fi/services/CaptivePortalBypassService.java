package com.lukekorth.auto_fi.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.webkit.WebView;

import com.lukekorth.auto_fi.utilities.ConnectivityUtils;
import com.lukekorth.auto_fi.webview.CaptivePortalWebViewClient;
import com.lukekorth.auto_fi.webview.JavascriptLoggingInterface;

public class CaptivePortalBypassService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        loadWebView();
        return START_NOT_STICKY;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void loadWebView() {
        WebView webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new JavascriptLoggingInterface(), "AutoFi");
        webView.setWebViewClient(new CaptivePortalWebViewClient(this));
        webView.loadUrl(ConnectivityUtils.CONNECTIVITY_CHECK_URL);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
