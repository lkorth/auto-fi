package com.lukekorth.auto_fi.receivers;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.lukekorth.auto_fi.MainActivity;
import com.lukekorth.auto_fi.R;
import com.lukekorth.auto_fi.models.Settings;
import com.lukekorth.auto_fi.models.WifiNetwork;
import com.lukekorth.auto_fi.services.ConnectivityCheckIntentService;
import com.lukekorth.auto_fi.utilities.Logger;
import com.lukekorth.auto_fi.utilities.VpnHelper;
import com.lukekorth.auto_fi.utilities.WifiHelper;

import io.realm.Realm;

public class WifiConnectionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        WifiHelper wifiHelper = new WifiHelper(context);
        if (wifiHelper.isConnectedToWifi()) {
            WifiConfiguration configuration =
                    wifiHelper.getWifiNetwork((WifiInfo) intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO));
            if (wifiHelper.isWifiUnsecured(configuration)) {
                setConnectionTimestamp(configuration);

                if (Settings.autoConnectToVpn(context) && !VpnHelper.isVpnEnabled(context)) {
                    displayVpnNotEnabledNotification(context);

                    if (WifiNetwork.isAutoconnectedNetwork(wifiHelper.getCurrentNetwork())) {
                        wifiHelper.disconnectFromCurrentWifiNetwork();
                    }
                } else if (!ConnectivityCheckIntentService.sIsRunning) {
                    Logger.info("Connected to unsecured wifi network " + wifiHelper.getCurrentNetworkName() +
                            ", checking connectivity");
                    context.startService(new Intent(context, ConnectivityCheckIntentService.class));
                } else {
                    Logger.info("ConnectivityCheckIntentService is already running");
                }
            }
        }
    }

    private void setConnectionTimestamp(WifiConfiguration configuration) {
        if (configuration != null) {
            Realm realm = Realm.getDefaultInstance();

            WifiNetwork network = WifiNetwork.findOrCreate(realm, configuration.SSID);

            realm.beginTransaction();
            network.setConnectedTimestamp(System.currentTimeMillis());
            realm.commitTransaction();

            realm.close();
        }
    }

    private void displayVpnNotEnabledNotification(Context context) {
        Logger.info("Vpn is disabled, creating notification");

        Notification notification = new Notification.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.vpn_permission_revoked))
                .setContentText(context.getString(R.string.vpn_permission_revoked_summary))
                .setContentIntent(MainActivity.getStartPendingIntent(context))
                .setAutoCancel(true)
                .build();

        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(0, notification);
    }
}
