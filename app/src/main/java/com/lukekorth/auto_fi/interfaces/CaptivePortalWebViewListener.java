package com.lukekorth.auto_fi.interfaces;

import android.support.annotation.MainThread;

public interface CaptivePortalWebViewListener {

    @MainThread
    void onComplete(boolean successfullyBypassed);
}
