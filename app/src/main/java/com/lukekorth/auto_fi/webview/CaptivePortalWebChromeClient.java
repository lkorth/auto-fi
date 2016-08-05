package com.lukekorth.auto_fi.webview;

import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;

import com.lukekorth.auto_fi.utilities.Logger;

public class CaptivePortalWebChromeClient extends WebChromeClient {

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        Logger.error("[WebView Error] Source: " + consoleMessage.sourceId() + ", line: " +
                consoleMessage.lineNumber() + ", message: " + consoleMessage.message());

        return true;
    }
}
