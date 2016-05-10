package com.lukekorth.auto_fi.webview;

import android.webkit.JavascriptInterface;

import com.lukekorth.auto_fi.utilities.Logger;

public class JavascriptLoggingInterface {

    @JavascriptInterface
    public void log(String message) {
        Logger.info("JS Console: " + message);
    }
}
