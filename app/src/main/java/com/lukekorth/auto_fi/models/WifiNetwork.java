package com.lukekorth.auto_fi.models;

import io.realm.Realm;
import io.realm.RealmObject;

public class WifiNetwork extends RealmObject {

    private String ssid;
    private boolean blacklisted;

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

    public static WifiNetwork findOrCreate(Realm realm, String ssid) {
        WifiNetwork wifiNetwork = realm.where(WifiNetwork.class).contains("ssid", ssid).findFirst();
        if (wifiNetwork == null) {
            realm.beginTransaction();
            wifiNetwork = realm.createObject(WifiNetwork.class);
            wifiNetwork.setSSID(ssid);
            realm.commitTransaction();
        }

        return wifiNetwork;
    }

    public static boolean isBlacklisted(String ssid) {
        Realm realm = Realm.getDefaultInstance();
        WifiNetwork wifiNetwork = findOrCreate(realm, ssid);
        boolean blacklisted = wifiNetwork.isBlacklisted();
        realm.close();
        return blacklisted;
    }
}
