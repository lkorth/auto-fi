package com.lukekorth.auto_fi.openvpn;

import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;

import com.lukekorth.auto_fi.BuildConfig;
import com.lukekorth.auto_fi.utilities.FileUtils;
import com.lukekorth.auto_fi.utilities.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class OpenVpnSetup {

    private static final String CONFIGURATION_FILE = "open_vpn_client_configuration.ovpn";
    private static final String VPN_EXECUTABLE = "pie_openvpn";

    public static boolean isSetup(Context context) {
        return FileUtils.isFileAvailable(context, CONFIGURATION_FILE);
    }

    public static void writeConfigurationFile(Context context, String publicKey, String privateKey)
            throws IOException {
        InputStream in = context.getAssets().open(CONFIGURATION_FILE);
        byte[] buffer = new byte[in.available()];
        in.read(buffer);
        in.close();

        String configuration = new String(buffer);
        configuration = configuration.replace("<- server here ->", BuildConfig.SERVER_ADDRESS);
        configuration = configuration.replace("<- management string here ->",
                context.getCacheDir().getAbsolutePath() + "/mgmtsocket unix");
        configuration = configuration.replace("<- public key here ->\n", publicKey);
        configuration = configuration.replace("<- private key here ->\n", privateKey);

        FileUtils.writeToDisk(context, configuration, CONFIGURATION_FILE);
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
            if (!FileUtils.isFileAvailable(context, VPN_EXECUTABLE + "." + abi)) {
                try {
                    File file = FileUtils.writeAssetFileToDisk(context, VPN_EXECUTABLE + "." + abi, true);
                    if (file != null) {
                        return file.getAbsolutePath();
                    }
                } catch (IOException e) {
                    Logger.error(e);
                }
            } else {
                return FileUtils.getFile(context, VPN_EXECUTABLE + "." + abi).getAbsolutePath();
            }
        }

        return null;
    }
}
