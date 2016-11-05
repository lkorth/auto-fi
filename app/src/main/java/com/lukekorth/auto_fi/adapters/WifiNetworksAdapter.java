package com.lukekorth.auto_fi.adapters;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lukekorth.auto_fi.R;
import com.lukekorth.auto_fi.databinding.WifiNetworkCardBinding;
import com.lukekorth.auto_fi.models.WifiNetwork;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;

public class WifiNetworksAdapter extends RecyclerView.Adapter<WifiNetworksAdapter.ViewHolder>
        implements RealmChangeListener<RealmResults<WifiNetwork>> {

    private Realm mRealm;
    private RealmResults<WifiNetwork> mWifiNetworks;

    public WifiNetworksAdapter() {
        mRealm = Realm.getDefaultInstance();
        sortByTime();
    }

    public void sortByTime() {
        getWifiNetworks("connectedTimestamp");
    }

    public void sortBySSID() {
        getWifiNetworks("ssid");
    }

    public void teardown() {
        mWifiNetworks.removeChangeListener(this);
        mRealm.close();
    }

    private void getWifiNetworks(String sortingField) {
        if (mWifiNetworks != null) {
            mWifiNetworks.removeChangeListener(this);
        }

        mWifiNetworks = mRealm.where(WifiNetwork.class)
                .not()
                .equalTo("ssid", "\"\"")
                .equalTo("connectedToVpn", true)
                .or()
                .greaterThan("blacklistedTimestamp", 0)
                .findAllSorted(sortingField);

        mWifiNetworks.addChangeListener(this);
        notifyDataSetChanged();
    }

    @Override
    public void onChange(RealmResults<WifiNetwork> element) {
        notifyDataSetChanged();
    }

    @Override
    public WifiNetworksAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder((WifiNetworkCardBinding) DataBindingUtil.inflate(
                LayoutInflater.from(parent.getContext()), R.layout.wifi_network_card, parent, false));
    }

    @Override
    public void onBindViewHolder(WifiNetworksAdapter.ViewHolder holder, int position) {
        holder.binding.setNetwork(mWifiNetworks.get(position));
    }

    public WifiNetwork getNetwork(int position) {
        return mWifiNetworks.get(position);
    }

    @Override
    public int getItemCount() {
        return mWifiNetworks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {

        public WifiNetworkCardBinding binding;

        public ViewHolder(WifiNetworkCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.getRoot().setOnCreateContextMenuListener(this);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View view,
                                        ContextMenu.ContextMenuInfo contextMenuInfo) {
            menu.add(0, getAdapterPosition(), 0, R.string.use);
            menu.add(0, getAdapterPosition(), 1, R.string.never_use);
        }
    }
}
