package com.lukekorth.auto_fi.utilities;

import android.content.Context;
import android.content.Intent;

import com.lukekorth.auto_fi.openvpn.OpenVpnConfiguration;
import com.lukekorth.auto_fi.services.OpenVpnConfigurationIntentService;
import com.lukekorth.auto_fi.services.VpnService;

public class VpnHelper {

    public static boolean isVpnEnabled(Context context) {
        Intent intent = VpnService.prepare(context);
        return intent == null;
    }

    public static void startVpn(Context context) {
        if (!OpenVpnConfiguration.isSetup(context)) {
            context.startService(new Intent(context, OpenVpnConfigurationIntentService.class));
        }

        context.startService(new Intent(context, VpnService.class));
    }
}
