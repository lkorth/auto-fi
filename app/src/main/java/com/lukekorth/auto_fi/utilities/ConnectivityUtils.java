package com.lukekorth.auto_fi.utilities;

import android.content.Context;
import android.net.Network;
import android.support.annotation.WorkerThread;

import com.google.firebase.analytics.FirebaseAnalytics;
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
        NO_CONNECTIVITY
    }

    @WorkerThread
    public static ConnectivityState checkConnectivity(Context context) {
        WifiHelper wifiHelper = new WifiHelper(context);
        HttpURLConnection connection = null;
        try {
            Network network = wifiHelper.bindToCurrentNetwork();

            connection = getConnection(network);
            connection.setUseCaches(false);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();

            Logger.info("Received " + responseCode + " response code from " + wifiHelper.getCurrentNetworkName());
            FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(context);
            analytics.logEvent("network_response_code_" + responseCode, null);
            analytics.logEvent(wifiHelper.getCurrentNetworkName().replace(" ", "_"), null);

            if (responseCode == HTTP_OK) {
                if (StreamUtils.readStream(connection.getInputStream()).contains("E1A304E5-E244-4846-B613-6290055A211D")) {
                    Logger.info(wifiHelper.getCurrentNetworkName() + " has connectivity");
                    return ConnectivityState.CONNECTED;
                } else {
                    Logger.info("Received 200 response code, but invalid body. Returning redirected.");
                    return ConnectivityState.REDIRECTED;
                }
            } else if (responseCode == HTTP_MOVED_PERM || responseCode == HTTP_MOVED_TEMP || responseCode == 307) {
                return ConnectivityState.REDIRECTED;
            }
        } catch (IOException e) {
            Logger.info("Connectivity check failed. " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }

            wifiHelper.unbindFromCurrentNetwork();
        }

        Logger.info("Unable to make a connection to " + wifiHelper.getCurrentNetworkName());
        return ConnectivityState.NO_CONNECTIVITY;
    }

    private static HttpURLConnection getConnection(Network network) throws IOException {
        if (Version.isAtLeastLollipop() && network != null) {
            return ((HttpURLConnection) network.openConnection(new URL(CONNECTIVITY_CHECK_URL)));
        } else {
            return (HttpURLConnection) new URL(CONNECTIVITY_CHECK_URL).openConnection();
        }
    }
}
