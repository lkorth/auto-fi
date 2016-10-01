package com.lukekorth.auto_fi;

import android.app.Application;

import com.lukekorth.auto_fi.utilities.DebugUtils;
import com.lukekorth.mailable_log.MailableLog;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class AutoFiApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        MailableLog.init(this, BuildConfig.DEBUG, "%date{MMM dd | HH:mm:ss.SSS} %highlight(%-5level) %msg%n");
        DebugUtils.setup(this);

        Realm.init(this);
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder().build();
        Realm.setDefaultConfiguration(realmConfiguration);
    }
}
