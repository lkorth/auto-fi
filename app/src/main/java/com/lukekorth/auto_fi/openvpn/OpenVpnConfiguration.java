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

public class OpenVpnConfiguration {

    private static final String PUBLIC_KEY_FILE = "open_vpn_public_key.crt";
    private static final String PRIVATE_KEY_FILE = "open_vpn_private_key.key";
    private static final String CONFIGURATION_FILE = "open_vpn_client_configuration.ovpn";
    private static final String VPN_EXECUTABLE = "openvpn_executable";

    public static boolean isSetup(Context context) {
        return FileUtils.isFileAvailable(context, PUBLIC_KEY_FILE) &&
                FileUtils.isFileAvailable(context, PRIVATE_KEY_FILE);
    }

    public static void writeKeyPair(Context context, String publicKey, String privateKey)
            throws IOException {
        FileUtils.writeToDisk(context, publicKey, PUBLIC_KEY_FILE);
        FileUtils.writeToDisk(context, privateKey, PRIVATE_KEY_FILE);
    }

    @Nullable
    static String[] getOpenVpnCommand(Context context) {
        String[] command = new String[3];

        String binaryName = getOpenVpnExecutable(context);
        if(binaryName == null) {
            Logger.error("Error writing OpenVPN executable");
            return null;
        }

        try {
            writeConfigurationFile(context);
        } catch (IOException e) {
            Logger.error("Error writing OpenVPN configuration file");
            return null;
        }

        command[0] = binaryName;
        command[1] = "--config";
        command[2] = context.getFilesDir().getAbsolutePath() + "/" + CONFIGURATION_FILE;

        return command;
    }

    private static void writeConfigurationFile(Context context) throws IOException {
        InputStream in = context.getAssets().open(CONFIGURATION_FILE);
        byte[] buffer = new byte[in.available()];
        in.read(buffer);
        in.close();

        String publicKey = FileUtils.readFile(context, PUBLIC_KEY_FILE);
        String privateKey = FileUtils.readFile(context, PRIVATE_KEY_FILE);

        String configuration = new String(buffer);
        configuration = configuration.replace("<- server here ->", BuildConfig.SERVER_IP);
        configuration = configuration.replace("<- management string here ->",
                context.getCacheDir().getAbsolutePath() + "/mgmtsocket unix");
        configuration = configuration.replace("<- public key here ->\n", publicKey);
        configuration = configuration.replace("<- private key here ->\n", privateKey);

        FileUtils.writeToDisk(context, configuration, CONFIGURATION_FILE);
    }

    @Nullable
    private static String getOpenVpnExecutable(Context context) {
        String[] abis = Build.SUPPORTED_ABIS;

        String nativeAPI = NativeMethods.getNativeAPI();
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
