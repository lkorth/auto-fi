package com.lukekorth.auto_fi.utilities;

import android.os.Build;

public class Version {

    public static boolean isAtLeastMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }
}
