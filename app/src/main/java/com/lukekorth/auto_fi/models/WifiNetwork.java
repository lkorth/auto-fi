package com.lukekorth.auto_fi.models;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.support.annotation.Nullable;

import com.lukekorth.auto_fi.utilities.RelativeTime;

import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;

public class WifiNetwork extends RealmObject {

    private String ssid;
    private boolean autoconnected;
    private boolean connectedToVpn;
    private long connectedTimestamp;
    private long blacklistedTimestamp;
    private boolean neverUse;

    public String getSSID() {
        return ssid;
    }

    public void setSSID(String ssid) {
        this.ssid = ssid;
    }

    public boolean isAutoconnected() {
        return autoconnected;
    }

    public void setAutoconnected(boolean autoconnected) {
        this.autoconnected = autoconnected;
    }

    public boolean isConnectedToVpn() {
        return connectedToVpn;
    }

    public void setConnectedToVpn(boolean connectedToVpn) {
        this.connectedToVpn = connectedToVpn;
    }

    public long getConnectedTimestamp() {
        return connectedTimestamp;
    }

    public void setConnectedTimestamp(long connectedTimestamp) {
        this.connectedTimestamp = connectedTimestamp;
    }

    public long getBlacklistedTimestamp() {
        return blacklistedTimestamp;
    }

    public void setBlacklistedTimestamp(long blacklistedTimestamp) {
        this.blacklistedTimestamp = blacklistedTimestamp;
    }

    public boolean shouldNeverUse() {
        return neverUse;
    }

    public void setNeverUse(boolean neverUse) {
        this.neverUse = neverUse;
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

    public static boolean isAutoconnectedNetwork(@Nullable WifiConfiguration wifiConfiguration) {
        if (wifiConfiguration == null) {
            return false;
        }

        Realm realm = Realm.getDefaultInstance();
        WifiNetwork wifiNetwork = find(realm, wifiConfiguration.SSID);
        return wifiNetwork != null && wifiNetwork.isAutoconnected();
    }

    public static void blacklist(String ssid) {
        Realm realm = Realm.getDefaultInstance();
        WifiNetwork wifiNetwork = WifiNetwork.findOrCreate(realm, ssid);
        realm.beginTransaction();
        wifiNetwork.setBlacklistedTimestamp(System.currentTimeMillis());
        realm.commitTransaction();
        realm.close();
    }

    public static boolean isBlacklisted(String ssid) {
        Realm realm = Realm.getDefaultInstance();
        WifiNetwork wifiNetwork = findOrCreate(realm, ssid);
        boolean blacklisted = wifiNetwork.getBlacklistedTimestamp() + TimeUnit.DAYS.toMillis(7) > System.currentTimeMillis();
        realm.close();

        return blacklisted;
    }

    public static String blacklistedUntil(Context context, WifiNetwork network) {
        return RelativeTime.get(context, network.getBlacklistedTimestamp() + TimeUnit.DAYS.toMillis(7));
    }

    public static boolean shouldNeverUse(String ssid) {
        Realm realm = Realm.getDefaultInstance();
        WifiNetwork wifiNetwork = findOrCreate(realm, ssid);
        boolean shouldNeverUse = wifiNetwork.shouldNeverUse();
        realm.close();

        return shouldNeverUse;
    }

    public static void setAllAutoConnectedNetworksDisconnected() {
        Realm realm = Realm.getDefaultInstance();

        RealmResults<WifiNetwork> networks = realm.where(WifiNetwork.class)
                .equalTo("autoconnected", true)
                .findAll();

        realm.beginTransaction();

        for (WifiNetwork network : networks) {
            network.setAutoconnected(false);
        }

        realm.commitTransaction();
        realm.close();
    }
}
