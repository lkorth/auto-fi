package com.lukekorth.auto_fi;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.lukekorth.auto_fi.openvpn.OpenVpnSetup;
import com.lukekorth.auto_fi.utilities.LogReporting;
import com.lukekorth.auto_fi.utilities.VpnHelper;

import java.io.IOException;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;

public class MainActivity extends AppCompatActivity {

    private static final int START_VPN = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // required to wifi scan results
        if (ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ ACCESS_COARSE_LOCATION}, 1);
        }
    }

    public void startVpn(View v) {
        try {
            OpenVpnSetup.writeConfigurationFile(this);
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
                VpnHelper.startVpn(this);
            } else {
                // user did not consent to VPN
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.contact_developer) {
            new LogReporting(this).collectAndSendLogs();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static PendingIntent getStartPendingIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        return PendingIntent.getActivity(context, 0, intent, 0);
    }
}
