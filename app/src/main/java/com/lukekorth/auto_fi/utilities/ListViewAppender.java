package com.lukekorth.auto_fi.utilities;

import android.os.Handler;
import android.os.Looper;

import com.lukekorth.auto_fi.adapters.LogAdapter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class ListViewAppender extends AppenderBase<ILoggingEvent> {

    private LogAdapter mLogAdapter;
    private Handler mMainThreadHandler;

    public ListViewAppender(LogAdapter logAdapter) {
        mLogAdapter = logAdapter;
        mMainThreadHandler = new Handler(Looper.getMainLooper());
    }

    public void append(final ILoggingEvent event) {
        if (!isStarted()) {
            return;
        }

        if (event.getLevel().levelInt != Level.OFF_INT) {
            mMainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    mLogAdapter.add(event.getMessage());
                }
            });
        }
    }
}
