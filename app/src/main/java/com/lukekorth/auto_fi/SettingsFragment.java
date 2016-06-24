package com.lukekorth.auto_fi;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.lukekorth.auto_fi.utilities.VpnHelper;

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

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
}
