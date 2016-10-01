package com.lukekorth.auto_fi;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.lukekorth.auto_fi.services.VpnService;
import com.lukekorth.auto_fi.utilities.VpnHelper;

public class AppIntroActivity extends AppIntro2 {

    public static final String APP_INTRO_COMPLETE = "com.lukekorth.auto_fi.APP_INTRO_COMPLETE";

    private static final int VPN_SLIDE = 3;

    @Override
    public void init(@Nullable Bundle savedInstanceState) {
        int backgroundColor = getResources().getColor(R.color.colorPrimary);

        addSlide(AppIntroFragment.newInstance(getString(R.string.app_name),
                getString(R.string.welcome), R.drawable.ic_phonelink_lock, backgroundColor));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !locationPermissionGranted()) {
            addSlide(AppIntroFragment.newInstance(getString(R.string.location_permission),
                    getString(R.string.location_permission_explanation), R.drawable.ic_place,
                    backgroundColor));
            askForPermissions(new String[] { Manifest.permission.ACCESS_COARSE_LOCATION }, 2);
        }

        addSlide(AppIntroFragment.newInstance(getString(R.string.vpn),
                getString(R.string.vpn_explanation), R.drawable.ic_vpn_key, backgroundColor));
        addSlide(AppIntroFragment.newInstance(getString(R.string.setup),
                getString(R.string.setup_message), R.drawable.ic_wifi_lock, backgroundColor));

        FirebaseAnalytics.getInstance(this).logEvent("intro_started", null);
    }

    @Override
    public void onDonePressed() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(AppIntroActivity.APP_INTRO_COMPLETE, true)
                .apply();

        FirebaseAnalytics.getInstance(this).logEvent("intro_completed", null);

        finish();
    }

    @Override
    public void onSlideChanged() {
        if (pager.getCurrentItem() == VPN_SLIDE && !VpnHelper.isVpnEnabled(this)) {
            startActivityForResult(VpnService.prepare(this), 1);
        }
    }

    @Override
    public void onBackPressed() {
        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(AppIntroActivity.APP_INTRO_COMPLETE, false)) {
            super.onBackPressed();
        }
    }

    private boolean locationPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onNextPressed() {}
}
