package com.lukekorth.auto_fi.utilities;

import android.support.annotation.WorkerThread;

import com.lukekorth.auto_fi.BuildConfig;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_OK;

public class ConnectivityUtils {

    public static final String CONNECTIVITY_CHECK_URL = "http://" + BuildConfig.SERVER_ADDRESS + "/connectivity-check/";

    public enum ConnectivityState {
        CONNECTED,
        REDIRECTED,
        NO_CONNECTIVITY;
    }

    @WorkerThread
    public static ConnectivityState checkConnectivity() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(CONNECTIVITY_CHECK_URL).openConnection();
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HTTP_OK) {
                if (StreamUtils.readStream(connection.getInputStream()).contains("E1A304E5-E244-4846-B613-6290055A211D")) {
                    Logger.info("Wifi network has connectivity");
                    return ConnectivityState.CONNECTED;
                }
            } else if (responseCode == HTTP_MOVED_PERM || responseCode == HTTP_MOVED_TEMP) {
                Logger.info("Received an http redirect, this network likely requires user interaction before using");
                return ConnectivityState.REDIRECTED;
            }
        } catch (IOException e) {
            Logger.info("Connectivity check failed. " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        Logger.info("Unable to make a connection");
        return ConnectivityState.NO_CONNECTIVITY;
    }
}
