package com.lukekorth.auto_fi;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.lukekorth.auto_fi.adapters.WifiNetworksAdapter;
import com.lukekorth.auto_fi.models.Settings;
import com.lukekorth.auto_fi.models.WifiNetwork;

import io.realm.Realm;

public class WifiNetworksActivity extends AppCompatActivity {

    private static final String SORT_ORDER = "wifi_network_sort_order";

    private WifiNetworksAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_networks);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mAdapter = new WifiNetworksAdapter(this);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAdapter.teardown();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.wifi_networks_activity, menu);

        if (!shouldSortByTime()) {
            sortNetworks(menu.findItem(R.id.sort));
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sort:
                Settings.getPrefs(this).edit()
                        .putBoolean(SORT_ORDER, !shouldSortByTime())
                        .apply();
                sortNetworks(item);
                return true;
            case android.R.id.home:
                finish();
                return true;
        }

        return false;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Realm realm = Realm.getDefaultInstance();
        if (item.getTitle().equals(getString(R.string.use))) {
            WifiNetwork network = mAdapter.getNetwork(item.getItemId());
            realm.beginTransaction();
            network.setNeverUse(false);
            network.setBlacklistedTimestamp(0);
            realm.commitTransaction();
            realm.close();

            return true;
        } else if (item.getTitle().equals(getString(R.string.never_use))) {
            WifiNetwork network = mAdapter.getNetwork(item.getItemId());
            realm.beginTransaction();
            network.setNeverUse(true);
            realm.commitTransaction();
            realm.close();

            return true;
        }

        return false;
    }

    private boolean shouldSortByTime() {
        return Settings.getPrefs(this).getBoolean(SORT_ORDER, true);
    }

    private void sortNetworks(MenuItem item) {
        if (shouldSortByTime()) {
            item.setIcon(R.drawable.ic_time);
            mAdapter.sortByTime();
        } else {
            item.setIcon(R.drawable.ic_alpha);
            mAdapter.sortBySSID();
        }
    }
}
