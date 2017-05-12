package com.lukekorth.auto_fi.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.support.annotation.MainThread;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.lukekorth.auto_fi.MainActivity;
import com.lukekorth.auto_fi.R;
import com.lukekorth.auto_fi.interfaces.Vpn;
import com.lukekorth.auto_fi.interfaces.VpnServiceInterface;
import com.lukekorth.auto_fi.models.WifiNetwork;
import com.lukekorth.auto_fi.openvpn.OpenVpn;
import com.lukekorth.auto_fi.utilities.Logger;
import com.lukekorth.auto_fi.utilities.WifiHelper;

import io.realm.Realm;

public class VpnService extends android.net.VpnService implements VpnServiceInterface {

    public static final String DISCONNECT_VPN_INTENT_ACTION = "com.lukekorth.auto_fi.DISCONNECT_VPN";

    private static final int NOTIFICATION_ID = 1;

    private Vpn mVpn;
    private WifiHelper mWifiHelper;
    private BroadcastReceiver mDisconnectReceiver;

    @Override
    @MainThread
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Logger.info("Restarting VPNService after crash or being killed");
        }

        mWifiHelper = new WifiHelper(this);

        if (!mWifiHelper.isConnected()) {
            Logger.warn("No wifi networked connected, stopping VPNService");
            stopSelf();
            return START_NOT_STICKY;
        }

        mWifiHelper.bindToCurrentNetwork();

        registerDisconnectReceiver();
        setNotificationMessage(R.string.state_connecting);

        mVpn = new OpenVpn(this, this);
        mVpn.start();

        FirebaseAnalytics.getInstance(this).logEvent("vpn_started", null);

        return START_STICKY;
    }

    @Override
    public WifiHelper getWifiHelper() {
        return mWifiHelper;
    }

    @Override
    public Builder getBuilder() {
        return new Builder();
    }

    @Override
    public void successfullyConnected() {
        Realm realm = Realm.getDefaultInstance();

        WifiNetwork network = WifiNetwork.find(realm, mWifiHelper.getCurrentNetworkName());
        if (network != null) {
            realm.beginTransaction();
            network.setConnectedToVpn(true);
            realm.commitTransaction();
        }

        realm.close();
    }

    @Override
    public void shutdown() {
        stopVpn();
    }

    @Override
    public void onRevoke() {
        Logger.info("VPN permission revoked by OS, stopping");
        stopVpn();
    }

    @Override
    public void setNotificationMessage(int message) {
        Logger.debug(getString(message));

        Intent disconnectVPN = new Intent(DISCONNECT_VPN_INTENT_ACTION);
        PendingIntent disconnectPendingIntent = PendingIntent.getBroadcast(this, 0, disconnectVPN, 0);

        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(message))
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setLocalOnly(true)
                .setPriority(Notification.PRIORITY_MIN)
                .setContentIntent(MainActivity.getStartPendingIntent(this))
                .setSmallIcon(R.drawable.ic_vpn_key)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                        getString(R.string.cancel_connection), disconnectPendingIntent);

        startForeground(NOTIFICATION_ID, builder.build());
    }

    private void registerDisconnectReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(DISCONNECT_VPN_INTENT_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mDisconnectReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(DISCONNECT_VPN_INTENT_ACTION)) {
                    stopVpn();
                } else if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION) &&
                        !mWifiHelper.isConnected()) {
                    stopVpn();
                }
            }
        };
        registerReceiver(mDisconnectReceiver, filter);
    }

    private void stopVpn() {
        mVpn.stop();
        stopForeground(true);

        unregisterDisconnectionReceiver();

        mWifiHelper.unbindFromCurrentNetwork();

        if (WifiNetwork.isAutoconnectedNetwork(mWifiHelper.getCurrentNetwork())) {
            mWifiHelper.disconnectFromCurrentNetwork();
        }

        stopSelf();
    }

    private void unregisterDisconnectionReceiver() {
        try {
            unregisterReceiver(mDisconnectReceiver);
        } catch (IllegalArgumentException ignored) {}
    }
}
