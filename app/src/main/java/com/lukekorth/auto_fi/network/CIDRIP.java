package com.lukekorth.auto_fi.network;

import android.annotation.SuppressLint;

/**
 * Classless Inter-Domain Routing, an IP addressing scheme that replaces the older system based on
 * classes A, B, and C. A single IP address can be used to designate many unique IP addresses with
 * CIDR. A CIDR IP address looks like a normal IP address except that it ends with a slash followed
 * by a number, called the IP network prefix.
 */
public class CIDRIP {

    private String mIp;
    private int mLength;

    public CIDRIP(String address, int prefixLength) {
        mIp = address;
        mLength = prefixLength;
    }

    public CIDRIP(String ip, String mask) {
        mIp = ip;

        long netmask = getInt(mask);
        netmask += 1L << 32; // Add 33. bit to ensure the loop terminates

        int lenZeros = 0;
        while ((netmask & 0x1) == 0) {
            lenZeros++;
            netmask = netmask >> 1;
        }

        if (netmask != (0x1ffffffffL >> lenZeros)) { // Check if rest of netmask is only 1s
            mLength = 32; // Asume no CIDR, set /32
        } else {
            mLength = 32 - lenZeros;
        }
    }

    public String getIp() {
        return mIp;
    }

    public int getLength() {
        return mLength;
    }

    public void setLength(int length) {
        mLength = length;
    }

    @SuppressLint("DefaultLocale")
    public boolean normalize() {
        long ip = getInt(mIp);

        long newIp = ip & (0xffffffffL << (32 - mLength));
        if (newIp != ip) {
            mIp = String.format("%d.%d.%d.%d", (newIp & 0xff000000) >> 24, (newIp & 0xff0000) >> 16,
                    (newIp & 0xff00) >> 8, newIp & 0xff);
            return true;
        } else {
            return false;
        }
    }

    public static long getInt(String ipaddr) {
        String[] ipt = ipaddr.split("\\.");
        long ip = 0;

        ip += Long.parseLong(ipt[0]) << 24;
        ip += Integer.parseInt(ipt[1]) << 16;
        ip += Integer.parseInt(ipt[2]) << 8;
        ip += Integer.parseInt(ipt[3]);

        return ip;
    }

    public long getInt() {
        return getInt(mIp);
    }

    @Override
    public String toString() {
        return mIp + "/" + mLength;
    }
}