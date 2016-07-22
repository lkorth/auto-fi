package com.lukekorth.auto_fi.webview;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Proxy;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.lukekorth.auto_fi.services.CaptivePortalBypassService;
import com.lukekorth.auto_fi.utilities.ConnectivityUtils;
import com.lukekorth.auto_fi.utilities.VpnHelper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static com.google.android.gms.internal.zzs.TAG;

public class CaptivePortalWebViewClient extends WebViewClient {

    private CaptivePortalBypassService mService;
    private Application mApplication;
    private Context mContext;
    private WebView mWebView;
    private String mBypassJavascript;
    private boolean mFirstPageLoad = true;
    private int mBypassAttempts = 0;

    public CaptivePortalWebViewClient(CaptivePortalBypassService service, WebView webView) throws IOException {
        mService = service;
        mApplication = service.getApplication();
        mContext = mApplication.getApplicationContext();
        mWebView = webView;

        InputStream in = mContext.getAssets().open("captive_portal_bypass.js");
        byte[] buffer = new byte[in.available()];
        in.read(buffer);
        in.close();
        mBypassJavascript = new String(buffer).replaceAll("//.*\n", "").replace("\n", "");
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        if (!mFirstPageLoad) {
            bypassCaptivePortal(view);
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        if (mFirstPageLoad) {
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

    private void loadConnectivityCheckUrl(WebView webView) {
        webView.loadUrl(ConnectivityUtils.CONNECTIVITY_CHECK_URL);
    }

    private void bypassCaptivePortal(WebView view) {
        mBypassAttempts++;
        if (mBypassAttempts > 3) {
            mService.stop(true);
            return;
        }

        view.loadUrl("javascript:" + mBypassJavascript);
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
                    VpnHelper.startVpn(mContext);
                    FirebaseAnalytics.getInstance(mContext).logEvent("captive_portal_bypassed", null);
                    mService.stop(false);
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
