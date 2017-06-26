package com.lukekorth.auto_fi.services;

import android.app.IntentService;
import android.content.Intent;
import android.util.Base64;

import com.lukekorth.auto_fi.BuildConfig;
import com.lukekorth.auto_fi.models.CaptivePortalPage;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;

import io.realm.Realm;

import static java.net.HttpURLConnection.HTTP_OK;

public class CaptivePortalPageUploadService extends IntentService {

    public CaptivePortalPageUploadService() {
        super("CaptivePortalPageUploadService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();

        for (CaptivePortalPage page : CaptivePortalPage.getStoredPages()) {
            boolean success = uploadPage(new String(Base64.encode(page.getHtml().getBytes(),
                    Base64.DEFAULT)));

            if (success) {
                page.deleteFromRealm();
            }
        }

        realm.commitTransaction();
        realm.close();
    }

    private boolean uploadPage(String page) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL("http://" + BuildConfig.SERVER_IP + "/captive-portal-page/")
                    .openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            Writer out = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
            out.write(page, 0, page.length());
            out.flush();
            out.close();

            return connection.getResponseCode() == HTTP_OK;
        } catch (IOException e) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
