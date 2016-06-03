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

public class WifiConnectionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Settings.isEnabled(context)) {
            return;
        }

        WifiHelper wifiHelper = new WifiHelper(context);

        if (wifiHelper.isConnectedToWifi()) {
            WifiConfiguration configuration =
                    wifiHelper.getWifiNetwork((WifiInfo) intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO));
            if (wifiHelper.isWifiUnsecured(configuration)) {
                if (VpnHelper.isVpnEnabled(context)) {
                    Logger.info("Connected to unsecured wifi network, checking connectivity");
                    context.startService(new Intent(context, ConnectivityCheckIntentService.class));
                } else {
                    displayVpnNotEnabledNotification(context);

                    if (WifiNetwork.isAutoconnectedNetwork(wifiHelper.getCurrentNetwork())) {
                        wifiHelper.disconnectFromCurrentWifiNetwork();
                    }
                }
            }
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
