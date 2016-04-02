package com.lukekorth.auto_fi;

import android.app.Application;

import com.lukekorth.auto_fi.utilities.PRNGFixes;

public class AutoFiApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        PRNGFixes.apply();
    }
}
