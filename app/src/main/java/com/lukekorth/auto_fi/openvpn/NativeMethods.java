package com.lukekorth.auto_fi.openvpn;

class NativeMethods {

    static native String[] getIfconfig() throws IllegalArgumentException;

    static native void jniclose(int fdint);

    static native String getNativeAPI();

    static {
        System.loadLibrary("opvpnutil");
    }
}
