package com.lukekorth.auto_fi.services;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import com.lukekorth.auto_fi.MainActivity;
import com.lukekorth.auto_fi.R;
import com.lukekorth.auto_fi.interfaces.Vpn;
import com.lukekorth.auto_fi.interfaces.VpnServiceInterface;
import com.lukekorth.auto_fi.openvpn.OpenVpn;
import com.lukekorth.auto_fi.utilities.Logger;

public class VpnService extends android.net.VpnService implements VpnServiceInterface {

    private static final int NOTIFICATION_ID = 1;
    private static final String DISCONNECT_VPN = "com.lukekorth.auto_fi.DISCONNECT_VPN";

    private Vpn mVpn;
    private BroadcastReceiver mDisconnectReceiver;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // the intent is null when the service has been restarted
        if (intent == null) {
            Logger.info("Restarting OpenVPN Service after crash or being killed");
        }

        registerDisconnectReceiver();
        setNotificationMessage(getString(R.string.state_connecting));

        mVpn = new OpenVpn(this, this);
        mVpn.start();

        return START_STICKY;
    }

    @Override
    public Builder getBuilder() {
        return new Builder();
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
    public void onDestroy() {
        unregisterReceiver(mDisconnectReceiver);
    }

    private void stopVpn() {
        mVpn.stop();
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void setNotificationMessage(String message) {
        Intent disconnectVPN = new Intent(DISCONNECT_VPN);
        PendingIntent disconnectPendingIntent = PendingIntent.getBroadcast(this, 0, disconnectVPN, 0);

        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(message)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_MIN)
                .setContentIntent(MainActivity.getStartPendingIntent(this))
                .setSmallIcon(R.mipmap.ic_launcher)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                        getString(R.string.cancel_connection), disconnectPendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            lollipopNotificationExtras(builder);
        }

        startForeground(NOTIFICATION_ID, builder.build());
    }

    private void registerDisconnectReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(DISCONNECT_VPN);
        mDisconnectReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                stopVpn();
            }
        };
        registerReceiver(mDisconnectReceiver, filter);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void lollipopNotificationExtras(Notification.Builder builder) {
        builder.setCategory(Notification.CATEGORY_SERVICE)
                .setLocalOnly(true);
    }
}
