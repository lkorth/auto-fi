package com.lukekorth.auto_fi.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.lukekorth.auto_fi.R;
import com.lukekorth.auto_fi.models.WifiNetwork;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class WifiNetworksAdapter extends RecyclerView.Adapter<WifiNetworksAdapter.ViewHolder>
        implements RealmChangeListener<RealmResults<WifiNetwork>> {

    private Context mContext;
    private LayoutInflater mLayoutInflater;
    private Realm mRealm;
    private RealmResults<WifiNetwork> mWifiNetworks;

    public WifiNetworksAdapter(Context context) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(mContext);
        mRealm = Realm.getDefaultInstance();
        sortByTime();
    }

    public void sortByTime() {
        getWifiNetworks("connectedTimestamp", Sort.DESCENDING);
    }

    public void sortBySSID() {
        getWifiNetworks("ssid", Sort.ASCENDING);
    }

    public void teardown() {
        mWifiNetworks.removeChangeListener(this);
        mRealm.close();
    }

    private void getWifiNetworks(String sortingField, Sort sortOrder) {
        if (mWifiNetworks != null) {
            mWifiNetworks.removeChangeListener(this);
        }

        mWifiNetworks = mRealm.where(WifiNetwork.class)
                .not()
                .equalTo("ssid", "\"\"")
                .equalTo("connectedToVpn", true)
                .or()
                .greaterThan("blacklistedTimestamp", 0)
                .findAllSorted(sortingField, sortOrder);

        mWifiNetworks.addChangeListener(this);
        notifyDataSetChanged();
    }

    @Override
    public void onChange(RealmResults<WifiNetwork> element) {
        notifyDataSetChanged();
    }

    @Override
    public WifiNetworksAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(mLayoutInflater.inflate(R.layout.wifi_network_card, parent, false));
    }

    @Override
    public void onBindViewHolder(WifiNetworksAdapter.ViewHolder holder, int position) {
        WifiNetwork network = mWifiNetworks.get(position);

        holder.icon.setImageResource(WifiNetwork.isBlacklisted(network.getSSID()) ? R.drawable.ic_wifi_off : R.drawable.ic_wifi);
        holder.ssid.setText(network.getSSID().replace("\"", ""));

        if (WifiNetwork.isBlacklisted(network.getSSID()) && !network.shouldNeverUse()) {
            holder.blacklisted.setVisibility(VISIBLE);
            holder.blacklisted.setText(mContext.getString(R.string.blacklisted_until,
                    WifiNetwork.blacklistedUntil(mContext, network)));
        } else {
            holder.blacklisted.setVisibility(GONE);
        }

        holder.neverConnect.setVisibility(network.shouldNeverUse() ? VISIBLE : GONE);
    }

    public WifiNetwork getNetwork(int position) {
        return mWifiNetworks.get(position);
    }

    @Override
    public int getItemCount() {
        return mWifiNetworks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {

        ImageView icon;
        TextView ssid;
        TextView blacklisted;
        TextView neverConnect;

        public ViewHolder(View view) {
            super(view);

            view.setOnCreateContextMenuListener(this);
            icon = view.findViewById(R.id.icon);
            ssid = view.findViewById(R.id.ssid);
            blacklisted = view.findViewById(R.id.blacklisted);
            neverConnect = view.findViewById(R.id.never_connect);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View view,
                                        ContextMenu.ContextMenuInfo contextMenuInfo) {
            menu.add(0, getAdapterPosition(), 0, R.string.use);
            menu.add(0, getAdapterPosition(), 1, R.string.never_use);
        }
    }
}
