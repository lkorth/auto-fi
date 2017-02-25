package com.lukekorth.auto_fi.models;

import io.realm.Realm;
import io.realm.RealmObject;

public class DataUsage extends RealmObject {

    private long kilobytes;

    public long getKilobytes() {
        return kilobytes;
    }

    public void setKilobytes(long kilobytes) {
        this.kilobytes = kilobytes;
    }

    public static DataUsage getUsage(Realm realm) {
        DataUsage dataUsage = realm.where(DataUsage.class).findFirst();
        if (dataUsage == null) {
            realm.beginTransaction();
            dataUsage = realm.createObject(DataUsage.class);
            dataUsage.setKilobytes(0);
            realm.commitTransaction();
        }

        return dataUsage;
    }

    public static void addUsage(long kilobytes) {
        Realm realm = Realm.getDefaultInstance();

        DataUsage dataUsage = getUsage(realm);
        realm.beginTransaction();
        dataUsage.setKilobytes(dataUsage.getKilobytes() + kilobytes);
        realm.commitTransaction();

        realm.close();
    }
}
