package com.lukekorth.auto_fi.openvpn;

import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;

import com.lukekorth.auto_fi.utilities.FileUtils;
import com.lukekorth.auto_fi.utilities.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class OpenVpnSetup {

    private static final String CONFIGURATION_FILE = "open_vpn_client_configuration.ovpn";
    private static final String MININONPIEVPN = "nopie_openvpn";
    private static final String MINIPIEVPN = "pie_openvpn";

    public static void writeConfigurationFile(Context context) throws IOException {
        FileUtils.writeAssetFileToDisk(context, CONFIGURATION_FILE, false);
    }

    @Nullable
    public static String[] buildOpenVpnArgv(Context context) {
        String[] args = new String[3];

        String binaryName = getOpenVpnExecutable(context);
        if(binaryName == null) {
            Logger.error("Error writing minivpn binary");
            return null;
        }

        args[0] = binaryName;
        args[1] = "--config";
        args[2] = context.getFilesDir().getAbsolutePath() + "/" + CONFIGURATION_FILE;

        return args;
    }

    public static String[] replacePieWithNoPie(String[] mArgv) {
        mArgv[0] = mArgv[0].replace(MINIPIEVPN, MININONPIEVPN);
        return mArgv;
    }

    @Nullable
    private static String getOpenVpnExecutable(Context context) {
        String[] abis;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            abis = Build.SUPPORTED_ABIS;
        } else {
            abis = new String[] { Build.CPU_ABI, Build.CPU_ABI2 };
        }

        String nativeAPI = NativeUtils.getNativeAPI();
        if (!nativeAPI.equals(abis[0])) {
            Logger.warn("Preferred native ABI precedence of this device (" + Arrays.toString(abis) + ") and ABI " +
                    "reported by native libraries (" + nativeAPI + ") mismatch");
            abis = new String[] { nativeAPI };
        }

        for (String abi: abis) {
            if (!FileUtils.isFileAvailable(context, getMiniVpnExecutableName() + "." + abi)) {
                try {
                    File file = FileUtils.writeAssetFileToDisk(context, getMiniVpnExecutableName() + "." + abi, true);
                    if (file != null) {
                        return file.getAbsolutePath();
                    }
                } catch (IOException e) {
                    Logger.error(e);
                }
            }
        }

        return null;
    }

    private static String getMiniVpnExecutableName() {
        if (Build.VERSION.SDK_INT  >= Build.VERSION_CODES.JELLY_BEAN) {
            return MINIPIEVPN;
        } else {
            return MININONPIEVPN;
        }
    }
}
