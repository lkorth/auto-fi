package com.lukekorth.auto_fi.utilities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.SystemClock;

import com.lukekorth.auto_fi.R;
import com.lukekorth.mailable_log.MailableLog;

import java.io.IOException;

public class LogReporting {

    private Context mContext;
    private ProgressDialog mLoading;
    private Intent mEmailIntent;

    public LogReporting(Context context) {
        mContext = context;
    }

    public void collectAndSendLogs() {
        mLoading = ProgressDialog.show(mContext, "", mContext.getString(R.string.loading), true);
        new GenerateLogFile().execute();
    }

    private class GenerateLogFile extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... args) {
            try {
                mEmailIntent = MailableLog.buildEmailIntent(mContext, "auto-fi@lukekorth.com",
                        "Auto-Fi Debug Log", "auto-fi-debug.log", null);
            } catch (IOException e) {
                Logger.error(e);
            }

            // Ensure we show the spinner and don't just flash the screen
            SystemClock.sleep(1000);

            return null;
        }

        @Override
        protected void onPostExecute(Void args) {
            if (mLoading != null && mLoading.isShowing()) {
                mLoading.cancel();
            }

            mContext.startActivity(mEmailIntent);
        }
    }
}
