package com.lukekorth.auto_fi.network;

import java.net.Inet6Address;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.TreeSet;

public class NetworkSpace {

    private TreeSet<IPAddress> mIPAddresses = new TreeSet<>();

    public void addIP(CIDRIP cidrIp, boolean include) {
        mIPAddresses.add(new IPAddress(cidrIp, include));
    }

    public void addIPv6(Inet6Address address, int mask, boolean included) {
        mIPAddresses.add(new IPAddress(address, mask, included));
    }

    public List<IPAddress> getNetworks(boolean included) {
        List<IPAddress> ips = new ArrayList<>();
        for (IPAddress ip : mIPAddresses) {
            if (ip.isIncluded() == included)
                ips.add(ip);
        }
        return ips;
    }

    public void clear() {
        mIPAddresses.clear();
    }

    public List<IPAddress> getPositiveIPList() {
        TreeSet<IPAddress> ipsSorted = generateIPList();

        List<IPAddress> ips = new ArrayList<>();
        for (IPAddress ia : ipsSorted) {
            if (ia.isIncluded())
                ips.add(ia);
        }

        return ips;
    }

    private TreeSet<IPAddress> generateIPList() {
        PriorityQueue<IPAddress> networks = new PriorityQueue<>(mIPAddresses);
        TreeSet<IPAddress> ipsDone = new TreeSet<>();

        IPAddress currentNet = networks.poll();
        if (currentNet == null) {
            return ipsDone;
        }

        while (currentNet != null) {
            // Check if it and the next of it are compatible
            IPAddress nextNet = networks.poll();

            if (nextNet == null || currentNet.getLastAddress().compareTo(nextNet.getFirstAddress()) == -1) {
                // Everything good, no overlapping nothing to do
                ipsDone.add(currentNet);
                currentNet = nextNet;
            } else {
                // This network is smaller or equal to the next but has the same base address
                if (currentNet.getFirstAddress().equals(nextNet.getFirstAddress()) && currentNet.getNetworkMask() >= nextNet.getNetworkMask()) {
                    if (currentNet.isIncluded() == nextNet.isIncluded()) {
                        // Included in the next next and same type
                        // Simply forget our current network
                        currentNet = nextNet;
                    } else {
                        // our currentNet is included in next and types differ. Need to split the next network
                        IPAddress[] newNets = nextNet.split();

                        // TODO: The contains method of the Priority is stupid linear search

                        // First add the second half to keep the order in networks
                        if (!networks.contains(newNets[1])) {
                            networks.add(newNets[1]);
                        }

                        // Don't add the lower half that would conflict with currentNet
                        if (!newNets[0].getLastAddress().equals(currentNet.getLastAddress())) {
                            if (!networks.contains(newNets[0])) {
                                networks.add(newNets[0]);
                            }
                        }
                        // Keep currentNet as is
                    }
                } else {
                    // This network is bigger than the next and last ip of current >= next

                    // Next network is in included in our network with the same type,
                    // simply ignore the next and move on
                    if (currentNet.isIncluded() != nextNet.isIncluded()) {
                        // We need to split our network
                        IPAddress[] newNets = currentNet.split();

                        if (newNets[1].getNetworkMask() == nextNet.getNetworkMask()) {
                            networks.add(nextNet);
                        } else {
                            // Add the smaller network first
                            networks.add(newNets[1]);
                            networks.add(nextNet);
                        }
                        currentNet = newNets[0];
                    }
                }
            }

        }

        return ipsDone;
    }
}
