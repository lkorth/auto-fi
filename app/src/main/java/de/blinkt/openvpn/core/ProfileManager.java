/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ProfileManager {

    private static final String PREFS_NAME = "VPNList";

    private static ProfileManager instance;

    private static VpnProfile mLastConnectedVpn = null;
    private HashMap<String, VpnProfile> profiles = new HashMap<>();
    private static VpnProfile tmpprofile = null;


    private static VpnProfile get(String key) {
        if (tmpprofile != null && tmpprofile.getUUIDString().equals(key))
            return tmpprofile;

        if (instance == null)
            return null;
        return instance.profiles.get(key);

    }


    private ProfileManager() {
    }

    private static void checkInstance(Context context) {
        if (instance == null) {
            instance = new ProfileManager();
            instance.loadVPNList(context);
        }
    }

    synchronized public static ProfileManager getInstance(Context context) {
        checkInstance(context);
        return instance;
    }

    public static VpnProfile getProfile(Context context) {
        try {
            ConfigParser configParser = new ConfigParser();
            configParser.parseConfig(new InputStreamReader(context.getAssets().open("client.ovpn")));
            return configParser.convertProfile();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void setConnectedVpnProfile(Context c, VpnProfile connectedrofile) {
        mLastConnectedVpn = connectedrofile;
    }

    public Collection<VpnProfile> getProfiles() {
        return profiles.values();
    }

    public void saveProfileList(Context context) {
        SharedPreferences sharedprefs = context.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        Editor editor = sharedprefs.edit();
        editor.putStringSet("vpnlist", profiles.keySet());

        // For reasing I do not understand at all
        // Android saves my prefs file only one time
        // if I remove the debug code below :(
        int counter = sharedprefs.getInt("counter", 0);
        editor.putInt("counter", counter + 1);
        editor.apply();

    }

    private void loadVPNList(Context context) {
        profiles = new HashMap<>();
        SharedPreferences listpref = context.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        Set<String> vlist = listpref.getStringSet("vpnlist", null);
        if (vlist == null) {
            vlist = new HashSet<>();
        }

        for (String vpnentry : vlist) {
            try {
                ObjectInputStream vpnfile = new ObjectInputStream(context.openFileInput(vpnentry + ".vp"));
                VpnProfile vp = ((VpnProfile) vpnfile.readObject());

                // Sanity check
                if (vp == null || vp.getUUID() == null)
                    continue;

                vp.upgradeProfile();
                profiles.put(vp.getUUID().toString(), vp);

            } catch (IOException | ClassNotFoundException e) {
                VpnStatus.logException("Loading VPN List", e);
            }
        }
    }


    public static VpnProfile get(Context context, String profileUUID) {
        checkInstance(context);
        return get(profileUUID);
    }

    public static VpnProfile getLastConnectedVpn() {
        return mLastConnectedVpn;
    }

}
