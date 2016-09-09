package com.lukekorth.auto_fi.webview;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.Proxy;
import android.net.ProxyInfo;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.WebView;

import com.lukekorth.auto_fi.interfaces.CaptivePortalWebViewListener;
import com.lukekorth.auto_fi.utilities.Logger;
import com.lukekorth.auto_fi.utilities.WifiHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CaptivePortalWebView extends WebView {

    private WifiHelper mWifiHelper;

    public CaptivePortalWebView(Context context) {
        super(context);
        init();
    }

    public CaptivePortalWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CaptivePortalWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CaptivePortalWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public CaptivePortalWebView(Context context, AttributeSet attrs, int defStyleAttr, boolean privateBrowsing) {
        super(context, attrs, defStyleAttr, privateBrowsing);
        init();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void init() {
        clearCache(true);
        getSettings().setJavaScriptEnabled(true);
        addJavascriptInterface(new JavascriptLoggingInterface(), "AutoFi");
        setWebChromeClient(new CaptivePortalWebChromeClient());

        mWifiHelper = new WifiHelper(getContext());
    }

    public void attemptBypass(Application application, CaptivePortalWebViewListener listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bindProcessToNetwork();
        }

        setWebViewClient(new CaptivePortalWebViewClient(application, listener, this));
        loadData("", "text/html", null);
    }

    public void tearDown() {
        unbindProcessFromNetwork();
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
}
