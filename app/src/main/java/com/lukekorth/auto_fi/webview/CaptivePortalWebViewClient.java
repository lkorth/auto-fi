package com.lukekorth.auto_fi.webview;

import android.content.Context;
import android.content.Intent;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.lukekorth.auto_fi.services.ConnectivityCheckIntentService;

import java.io.IOException;
import java.io.InputStream;

public class CaptivePortalWebViewClient extends WebViewClient {

    private Context mContext;
    private String mBypassJavascript;

    public CaptivePortalWebViewClient(Context context) {
        mContext = context;
        try {
            InputStream in = context.getAssets().open("captive_portal_bypass.js");
            byte[] buffer = new byte[in.available()];
            in.read(buffer);
            in.close();
            mBypassJavascript = new String(buffer);
        } catch (IOException ignored) {}
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        view.loadUrl("javascript:" + mBypassJavascript);
        mContext.startService(new Intent(mContext, ConnectivityCheckIntentService.class));
    }
}
