package com.lukekorth.auto_fi.network;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;

import java.math.BigInteger;
import java.net.Inet6Address;

public class IPAddress implements Comparable<IPAddress> {

    private BigInteger mNetAddress;
    private BigInteger mFirstAddress;
    private BigInteger mLastAddress;
    private int mNetworkMask;
    private boolean mIncluded;
    private boolean mIPV4;

    public IPAddress(CIDRIP ip, boolean included) {
        mNetAddress = BigInteger.valueOf(ip.getInt());
        mNetworkMask = ip.getLength();
        mIncluded = included;
        mIPV4 = true;
    }

    public IPAddress(BigInteger address, int mask, boolean included, boolean ipv4) {
        mNetAddress = address;
        mNetworkMask = mask;
        mIncluded = included;
        mIPV4 = ipv4;
    }

    public IPAddress(Inet6Address address, int mask, boolean include) {
        mNetAddress = BigInteger.ZERO;
        mNetworkMask = mask;
        mIncluded = include;

        int s = 128;
        for (byte b : address.getAddress()) {
            s -= 8;
            mNetAddress = mNetAddress.add(BigInteger.valueOf((b & 0xFF)).shiftLeft(s));
        }
    }

    public int getNetworkMask() {
        return mNetworkMask;
    }

    public boolean isIncluded() {
        return mIncluded;
    }

    public BigInteger getLastAddress() {
        if (mLastAddress == null) {
            mLastAddress = getMaskedAddress(true);
        }

        return mLastAddress;
    }

    public BigInteger getFirstAddress() {
        if (mFirstAddress == null) {
            mFirstAddress = getMaskedAddress(false);
        }

        return mFirstAddress;
    }

    @SuppressLint("DefaultLocale")
    public String getIPv4Address() {
        long ip = mNetAddress.longValue();
        return String.format("%d.%d.%d.%d", (ip >> 24) % 256, (ip >> 16) % 256, (ip >> 8) % 256,
                ip % 256);
    }

    public String getIPv6Address() {
        BigInteger address = mNetAddress;

        String ipv6 = null;
        boolean lastPart = true;
        while (address.compareTo(BigInteger.ZERO) == 1) {
            long part = address.mod(BigInteger.valueOf(0x10000)).longValue();
            if (ipv6 != null || part != 0) {
                if (ipv6 == null && !lastPart) {
                    ipv6 = ":";
                }

                if (lastPart) {
                    ipv6 = String.format("%x", part, ipv6);
                } else {
                    ipv6 = String.format("%x:%s", part, ipv6);
                }
            }

            address = address.shiftRight(16);
            lastPart = false;
        }

        if (ipv6 == null) {
            return "::";
        } else {
            return ipv6;
        }
    }

    public IPAddress[] split() {
        IPAddress firstHalf = new IPAddress(getFirstAddress(), mNetworkMask + 1, mIncluded, mIPV4);
        IPAddress secondHalf = new IPAddress(firstHalf.getLastAddress().add(BigInteger.ONE),
                mNetworkMask + 1, mIncluded, mIPV4);
        return new IPAddress[] { firstHalf, secondHalf };
    }

    public boolean containsNet(IPAddress network) {
        BigInteger ourFirst = getFirstAddress();
        BigInteger ourLast = getLastAddress();
        BigInteger networkFirst = network.getFirstAddress();
        BigInteger networkLast = network.getLastAddress();

        boolean a = ourFirst.compareTo(networkFirst) != 1;
        boolean b = ourLast.compareTo(networkLast) != -1;
        return a && b;
    }

    private BigInteger getMaskedAddress(boolean one) {
        BigInteger numberAddress = mNetAddress;

        int numBits;
        if (mIPV4) {
            numBits = 32 - mNetworkMask;
        } else {
            numBits = 128 - mNetworkMask;
        }

        for (int i = 0; i < numBits; i++) {
            if (one) {
                numberAddress = numberAddress.setBit(i);
            } else {
                numberAddress = numberAddress.clearBit(i);
            }
        }

        return numberAddress;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        if (mIPV4) {
            return String.format("%s/%d", getIPv4Address(), mNetworkMask);
        } else {
            return String.format("%s/%d", getIPv6Address(), mNetworkMask);
        }
    }

    /**
     * sorts the networks with following criteria:
     *     1. compares first 1 of the network
     *     2. smaller networks are returned as smaller
     */
    @Override
    public int compareTo(@NonNull IPAddress ipAddress) {
        int compare = getFirstAddress().compareTo(ipAddress.getFirstAddress());
        if (compare != 0) {
            return compare;
        }

        if (mNetworkMask > ipAddress.mNetworkMask) {
            return -1;
        } else if (ipAddress.mNetworkMask == mNetworkMask) {
            return 0;
        } else {
            return 1;
        }
    }

    /**
     * Warning: ignores the mIncluded integer
     *
     * @param object the object to compare this instance with.
     */
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof IPAddress)) {
            return super.equals(object);
        }

        IPAddress ipAddress = (IPAddress) object;
        return mNetworkMask == ipAddress.mNetworkMask &&
                ipAddress.getFirstAddress().equals(getFirstAddress());
    }
}
