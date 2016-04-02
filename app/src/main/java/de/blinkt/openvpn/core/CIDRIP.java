package de.blinkt.openvpn.core;

import java.util.Locale;

class CIDRIP {

    private String mIp;
    private int mLength;

    public CIDRIP(String ip, String mask) {
        mIp = ip;
        long netmask = getInt(mask);

        // Add 33. bit to ensure the loop terminates
        netmask += 1l << 32;

        int lenZeros = 0;
        while ((netmask & 0x1) == 0) {
            lenZeros++;
            netmask = netmask >> 1;
        }

        // Check if rest of netmask is only 1s
        if (netmask != (0x1ffffffffl >> lenZeros)) {
            // Asume no CIDR, set /32
            mLength = 32;
        } else {
            mLength = 32 - lenZeros;
        }
    }

    public CIDRIP(String address, int prefixLength) {
        mIp = address;
        mLength = prefixLength;
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

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "%s/%d", mIp, mLength);
    }

    public boolean normalize() {
        long ip = getInt(mIp);

        long newIp = ip & (0xffffffffl << (32 - mLength));
        if (newIp != ip) {
            mIp = String.format("%d.%d.%d.%d", (newIp & 0xff000000) >> 24, (newIp & 0xff0000) >> 16, (newIp & 0xff00) >> 8, newIp & 0xff);
            return true;
        } else {
            return false;
        }
    }

    static long getInt(String ipaddr) {
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
}