package com.lukekorth.auto_fi.webview;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Proxy;
import android.net.http.SslError;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.lukekorth.auto_fi.interfaces.CaptivePortalWebViewListener;
import com.lukekorth.auto_fi.utilities.ConnectivityUtils;
import com.lukekorth.auto_fi.utilities.Logger;
import com.lukekorth.auto_fi.utilities.StreamUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static com.google.android.gms.internal.zzs.TAG;

public class CaptivePortalWebViewClient extends WebViewClient {

    private Application mApplication;
    private Context mContext;
    private CaptivePortalWebViewListener mListener;
    private WebView mWebView;
    private String mBypassJavascript;
    private boolean mFirstPageLoad = true;
    private int mBypassAttempts = 0;

    public CaptivePortalWebViewClient(Application application,
                                      CaptivePortalWebViewListener listener, WebView webView) {
        mApplication = application;
        mContext = mApplication.getApplicationContext();
        mListener = listener;
        mWebView = webView;
        mBypassJavascript = StreamUtils.getAsset(mContext, "captive_portal_bypass.js");
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        Logger.info("Captive portal WebView loaded: " + url);

        if (mFirstPageLoad) {
            Logger.info("First page loaded, picking up proxy settings and loading connectivity url");
            mFirstPageLoad = false;
            // Now that WebView has loaded at least one page we know it has read in the proxy
            // settings.  Now prompt the WebView read the Network-specific proxy settings.
            setWebViewProxy();
            // Load the real page.
            loadConnectivityCheckUrl(view);
        } else {
            bypassCaptivePortal(view);
        }
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        Logger.warn("SSL error encountered while loading captive portal, proceeding");
        handler.proceed();
    }

    private void loadConnectivityCheckUrl(WebView webView) {
        webView.loadUrl(ConnectivityUtils.CONNECTIVITY_CHECK_URL);
    }

    private void bypassCaptivePortal(WebView view) {
        if (mBypassAttempts == 3) {
            Logger.info("3 captive portal bypasses attempted");
            mListener.onComplete(false);
            return;
        }

        mBypassAttempts++;
        Logger.info("Loading javascript to bypass captive portal");
        view.evaluateJavascript(mBypassJavascript, null);
        testForCaptivePortal();
    }

    private void testForCaptivePortal() {
        new Thread(new Runnable() {
            public void run() {
                // Give time for captive portal to open.
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {}

                if (ConnectivityUtils.checkConnectivity(mContext) == ConnectivityUtils.ConnectivityState.CONNECTED) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            mListener.onComplete(true);
                        }
                    });
                    FirebaseAnalytics.getInstance(mContext).logEvent("captive_portal_bypassed", null);
                } else {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            loadConnectivityCheckUrl(mWebView);
                        }
                    });
                }
            }
        }).start();
    }

    private void setWebViewProxy() {
        try {
            Field loadedApkField = Application.class.getDeclaredField("mLoadedApk");
            loadedApkField.setAccessible(true);
            Object loadedApk = loadedApkField.get(mApplication);
            Field receiversField = Class.forName("android.app.LoadedApk").getDeclaredField("mReceivers");
            receiversField.setAccessible(true);
            ArrayMap receivers = (ArrayMap) receiversField.get(loadedApk);
            for (Object receiverMap : receivers.values()) {
                for (Object rec : ((ArrayMap) receiverMap).keySet()) {
                    Class clazz = rec.getClass();
                    if (clazz.getName().contains("ProxyChangeListener")) {
                        Method onReceiveMethod = clazz.getDeclaredMethod("onReceive", Context.class, Intent.class);
                        Intent intent = new Intent(Proxy.PROXY_CHANGE_ACTION);
                        onReceiveMethod.invoke(rec, mContext, intent);
                        Log.v(TAG, "Prompting WebView proxy reload.");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception while setting WebView proxy: " + e);
        }
    }
}
