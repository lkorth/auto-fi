package com.lukekorth.auto_fi.interfaces;

import android.support.annotation.MainThread;

public interface Vpn {
    @MainThread
    void start();

    @MainThread
    void stop();
}
