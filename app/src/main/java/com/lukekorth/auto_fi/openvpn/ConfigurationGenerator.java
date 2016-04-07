package com.lukekorth.auto_fi.openvpn;

import android.content.Context;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConfigurationGenerator {

    public static void createConfigurationFile(Context context) throws IOException {
        BufferedReader bufferedReader =
                new BufferedReader(new InputStreamReader(context.getAssets().open("client.ovpn")));
        FileWriter fileWriter = new FileWriter(getConfigurationFilePath(context));

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            fileWriter.write(line + "\n");
        }

        fileWriter.flush();
        fileWriter.close();
    }

    public static String getConfigurationFilePath(Context context) {
        return context.getCacheDir().getAbsolutePath() + "/client-configuration.ovpn";
    }
}
