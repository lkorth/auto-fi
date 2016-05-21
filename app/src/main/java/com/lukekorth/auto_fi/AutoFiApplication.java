package com.lukekorth.auto_fi;

import android.app.Application;
import android.content.Context;

import com.lukekorth.auto_fi.utilities.DebugUtils;
import com.lukekorth.auto_fi.utilities.PRNGFixes;
import com.lukekorth.mailable_log.MailableLog;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class AutoFiApplication extends Application {

    private static Context sContext;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = this;

        DebugUtils.setup(this);

        MailableLog.init(this, BuildConfig.DEBUG, "%date{MMM dd | HH:mm:ss.SSS} %highlight(%-5level) %msg%n");

        PRNGFixes.apply();

        Realm.setDefaultConfiguration(new RealmConfiguration.Builder(this).build());
    }

    public static Context getContext() {
        return sContext;
    }
}
