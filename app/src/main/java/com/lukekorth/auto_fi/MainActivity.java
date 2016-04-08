package com.lukekorth.auto_fi;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.lukekorth.auto_fi.openvpn.ConfigurationGenerator;
import com.lukekorth.auto_fi.utilities.VpnHelper;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int START_VPN = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void startVpn(View v) {
        try {
            ConfigurationGenerator.createConfigurationFile(this);
        } catch (IOException ignored) {}

        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, START_VPN);
        } else {
            onActivityResult(START_VPN, Activity.RESULT_OK, null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == START_VPN) {
            if (resultCode == Activity.RESULT_OK) {
                VpnHelper.startVpn(getApplicationContext());
            } else {
                // user did not consent to VPN
            }
        }
    }

    public static PendingIntent getStartPendingIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        return PendingIntent.getActivity(context, 0, intent, 0);
    }
}
