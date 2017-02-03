package com.lukekorth.auto_fi.models;

import io.realm.DynamicRealm;
import io.realm.RealmSchema;

public class DataMigrations implements io.realm.RealmMigration {

    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
        RealmSchema schema = realm.getSchema();

        if (oldVersion == 1) {
            schema.create("CaptivePortalPage")
                    .addField("html", String.class);

            oldVersion++;
        }
    }
}
