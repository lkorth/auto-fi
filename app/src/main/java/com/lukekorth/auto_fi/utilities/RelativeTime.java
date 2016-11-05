package com.lukekorth.auto_fi.utilities;

import android.content.Context;
import android.text.format.DateUtils;

public class RelativeTime {

    public static String get(Context context, long timestamp) {
        return DateUtils.getRelativeTimeSpanString(context, timestamp, false).toString();
    }
}
