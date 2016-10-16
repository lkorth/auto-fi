package com.lukekorth.auto_fi.test;

import android.graphics.Bitmap;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.lukekorth.auto_fi.interfaces.CaptivePortalWebViewListener;
import com.lukekorth.auto_fi.utilities.StreamUtils;

import static android.support.test.InstrumentationRegistry.getTargetContext;

public class CaptivePortalTestWebViewClient extends WebViewClient {

    private String mBypassJavascript;
    private CaptivePortalWebViewListener mListener;

    public CaptivePortalTestWebViewClient(CaptivePortalWebViewListener listener) {
        mBypassJavascript = StreamUtils.getAsset(getTargetContext(), "captive_portal_bypass.js");
        mListener = listener;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        if (url.startsWith("http://bypassed/")) {
            mListener.onComplete(true);
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        view.evaluateJavascript(mBypassJavascript, null);
    }
}
