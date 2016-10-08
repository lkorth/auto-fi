package com.lukekorth.auto_fi;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.lukekorth.auto_fi.models.WifiNetwork;
import com.lukekorth.auto_fi.utilities.VpnHelper;

import io.realm.Realm;

public class SettingsFragment extends PreferenceFragment {

    private Realm mRealm;
    private Preference mWifiNetworksUsed;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        mRealm = Realm.getDefaultInstance();
        mWifiNetworksUsed = findPreference("wifi_networks_used");

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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }
}
