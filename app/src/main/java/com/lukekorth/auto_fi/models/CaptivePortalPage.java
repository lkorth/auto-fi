package com.lukekorth.auto_fi.models;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;

public class CaptivePortalPage extends RealmObject {

    private String html;

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public static boolean hasStoredPages() {
        Realm realm = Realm.getDefaultInstance();

        boolean hasStoredPages = realm.where(CaptivePortalPage.class)
                .findAll()
                .size() > 0;

        realm.close();

        return hasStoredPages;
    }

    public static RealmResults<CaptivePortalPage> getStoredPages() {
        Realm realm = Realm.getDefaultInstance();

        RealmResults<CaptivePortalPage> pages = realm.where(CaptivePortalPage.class)
                .findAll();

        realm.close();

        return pages;
    }
}
