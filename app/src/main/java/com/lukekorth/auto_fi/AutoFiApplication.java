package com.lukekorth.auto_fi;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.lukekorth.auto_fi.models.DataMigrations;
import com.lukekorth.auto_fi.openvpn.OpenVpnConfiguration;
import com.lukekorth.auto_fi.services.OpenVpnConfigurationIntentService;
import com.lukekorth.auto_fi.utilities.DebugUtils;
import com.lukekorth.mailable_log.MailableLog;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class AutoFiApplication extends Application {

    private static final String VERSION = "version";

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

        handleUpdate();
    }

    private void handleUpdate() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getString(VERSION, "").equals(BuildConfig.VERSION_NAME)) {
            prefs.edit()
                    .putString(VERSION, BuildConfig.VERSION_NAME)
                    .apply();

            OpenVpnConfiguration.clearKeyPair(this);
            startService(new Intent(this, OpenVpnConfigurationIntentService.class));
        }
    }
}
