package com.lukekorth.auto_fi.test;

import android.os.Handler;
import android.os.Looper;
import android.support.test.runner.AndroidJUnit4;
import android.webkit.WebView;

import com.lukekorth.auto_fi.interfaces.CaptivePortalWebViewListener;
import com.lukekorth.auto_fi.utilities.StreamUtils;
import com.lukekorth.auto_fi.webview.CaptivePortalWebChromeClient;
import com.lukekorth.auto_fi.webview.JavascriptLoggingInterface;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class CaptivePortalBypassTest {

    @Test(timeout = 1000)
    public void bypassesSubmitInput() {
        assertTrue(bypassCaptivePortal("submit_button.html"));
    }

    @Test(timeout = 1000)
    public void bypassesImageInput() {
        assertTrue(bypassCaptivePortal("image_button.html"));
    }

    private boolean bypassCaptivePortal(final String page) {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean bypassed = new AtomicBoolean(false);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                CaptivePortalTestWebViewClient webViewClient = new CaptivePortalTestWebViewClient(
                        new CaptivePortalWebViewListener() {
                            @Override
                            public void onComplete(boolean successfullyBypassed) {
                                bypassed.set(successfullyBypassed);
                                latch.countDown();
                            }
                        });

                WebView webView = new WebView(getTargetContext());
                webView.clearCache(true);
                webView.getSettings().setJavaScriptEnabled(true);
                webView.addJavascriptInterface(new JavascriptLoggingInterface(), "AutoFi");
                webView.setWebChromeClient(new CaptivePortalWebChromeClient());
                webView.setWebViewClient(webViewClient);

                webView.loadData(StreamUtils.getAsset(getInstrumentation().getContext(), page),
                        "text/html", "UTF-8");
            }
        });

        try {
            latch.await();
        } catch (InterruptedException ignored) {}

        return bypassed.get();
    }
}
