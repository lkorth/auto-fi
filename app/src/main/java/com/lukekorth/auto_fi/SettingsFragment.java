package com.lukekorth.auto_fi;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.lukekorth.auto_fi.models.DataUsage;
import com.lukekorth.auto_fi.models.WifiNetwork;
import com.lukekorth.auto_fi.utilities.VpnHelper;

import java.util.Locale;

import io.realm.Realm;

public class SettingsFragment extends PreferenceFragment {

    private Realm mRealm;
    private Preference mWifiNetworksUsed;
    private Preference mDataUsage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        mRealm = Realm.getDefaultInstance();
        mWifiNetworksUsed = findPreference("wifi_networks_used");
        mDataUsage = findPreference("data_usage");

        if (!BuildConfig.DEBUG) {
            getPreferenceScreen().removePreference(findPreference("debug_options"));
        } else {
            findPreference("connect_to_vpn").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    VpnHelper.startVpn(SettingsFragment.this.getActivity());
                    return false;
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        long wifiNetworksUsed = mRealm.where(WifiNetwork.class)
                .equalTo("connectedToVpn", true)
                .count();
        mWifiNetworksUsed.setSummary(Long.toString(wifiNetworksUsed));

        mDataUsage.setSummary(humanReadableByteCount(DataUsage.getUsage(mRealm).getKilobytes()));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }

    private static String humanReadableByteCount(long kilobytes) {
        if (kilobytes == 0) {
            return "0 B";
        }

        long bytes = kilobytes * 1024;
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = String.valueOf(("KMGTPE").charAt(exp - 1));
        return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
