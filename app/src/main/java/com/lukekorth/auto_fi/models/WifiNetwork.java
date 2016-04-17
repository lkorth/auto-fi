package com.lukekorth.auto_fi.models;

import android.support.annotation.Nullable;

import io.realm.Realm;
import io.realm.RealmObject;

public class WifiNetwork extends RealmObject {

    private String ssid;
    private boolean blacklisted;
    private long blacklistedTimestamp;

    public String getSSID() {
        return ssid;
    }

    public void setSSID(String ssid) {
        this.ssid= ssid;
    }

    public boolean isBlacklisted() {
        return blacklisted;
    }

    public void setBlacklisted(boolean blacklisted) {
        this.blacklisted = blacklisted;
    }

    public long getBlacklistedTimestamp() {
        return blacklistedTimestamp;
    }

    public void setBlacklistedTimestamp(long blacklistedTimestamp) {
        this.blacklistedTimestamp = blacklistedTimestamp;
    }

    @Nullable
    public static WifiNetwork find(Realm realm, String ssid) {
        return realm.where(WifiNetwork.class).contains("ssid", ssid).findFirst();
    }

    public static WifiNetwork findOrCreate(Realm realm, String ssid) {
        WifiNetwork wifiNetwork = find(realm, ssid);
        if (wifiNetwork == null) {
            realm.beginTransaction();
            wifiNetwork = realm.createObject(WifiNetwork.class);
            wifiNetwork.setSSID(ssid);
            realm.commitTransaction();
        }

        return wifiNetwork;
    }

    public static void blacklist(String ssid) {
        Realm realm = Realm.getDefaultInstance();
        WifiNetwork wifiNetwork = WifiNetwork.findOrCreate(realm, ssid);
        realm.beginTransaction();
        wifiNetwork.setBlacklisted(true);
        wifiNetwork.setBlacklistedTimestamp(System.currentTimeMillis());
        realm.commitTransaction();
        realm.close();
    }

    public static boolean isBlacklisted(String ssid) {
        Realm realm = Realm.getDefaultInstance();
        WifiNetwork wifiNetwork = findOrCreate(realm, ssid);

        // blacklisting lasts for 1 week
        boolean blacklisted = (wifiNetwork.isBlacklisted() &&
                (wifiNetwork.blacklistedTimestamp + 604800000 > System.currentTimeMillis()));
        if (!blacklisted) {
            realm.beginTransaction();
            wifiNetwork.setBlacklisted(false);
            realm.commitTransaction();
        }

        realm.close();

        return blacklisted;
    }
}
