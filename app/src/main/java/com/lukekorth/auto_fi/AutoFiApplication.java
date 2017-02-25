package com.lukekorth.auto_fi;

import android.app.Application;

import com.lukekorth.auto_fi.models.DataMigrations;
import com.lukekorth.auto_fi.utilities.DebugUtils;
import com.lukekorth.mailable_log.MailableLog;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class AutoFiApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            MailableLog.init(this, true, "%date{HH:mm:ss.SSS} %msg%n");
        } else {
            MailableLog.init(this, false, "%date{MMM dd | HH:mm:ss.SSS} %-5level %msg%n");
        }

        DebugUtils.setup(this);

        Realm.init(this);
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                .name("auto-fi.realm")
                .schemaVersion(2)
                .migration(new DataMigrations())
                .build();
        Realm.setDefaultConfiguration(realmConfiguration);

        DebugUtils.setStrictMode();
    }
}
