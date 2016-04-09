package com.lukekorth.auto_fi;

import android.app.Application;

import com.lukekorth.auto_fi.utilities.PRNGFixes;
import com.lukekorth.mailable_log.MailableLog;

public class AutoFiApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        MailableLog.init(this, BuildConfig.DEBUG, "%date{MMM dd | HH:mm:ss.SSS} %highlight(%-5level) %msg%n");

        PRNGFixes.apply();
    }
}
