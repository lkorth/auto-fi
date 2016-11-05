package com.lukekorth.auto_fi.migrations;

import io.realm.DynamicRealm;
import io.realm.RealmSchema;

public class DataMigrations implements io.realm.RealmMigration {

    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
        RealmSchema schema = realm.getSchema();

        if (oldVersion == 1) {
            schema.get("WifiNetwork")
                    .removeField("blacklisted")
                    .addField("connectedTimestamp", long.class)
                    .addField("neverUse", boolean.class);

            oldVersion++;
        }
    }
}
