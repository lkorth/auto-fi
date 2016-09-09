package com.lukekorth.auto_fi;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.FrameLayout;

import com.lukekorth.auto_fi.interfaces.CaptivePortalWebViewListener;
import com.lukekorth.auto_fi.webview.CaptivePortalWebView;

public class CaptivePortalBypassActivity extends AppCompatActivity
        implements CaptivePortalWebViewListener {

    private CaptivePortalWebView mWebView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWebView = new CaptivePortalWebView(this);
        ((FrameLayout) findViewById(android.R.id.content)).addView(mWebView);

        mWebView.attemptBypass(getApplication(), this);
    }

    @Override
    public void onComplete(boolean successfullyBypassed) {
        new AlertDialog.Builder(this)
                .setTitle(successfullyBypassed ? R.string.captive_portal_bypassed : R.string.captive_portal_bypass_failed)
                .setMessage(successfullyBypassed ? R.string.captive_portal_bypassed_message : R.string.captive_portal_bypass_failed_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        CaptivePortalBypassActivity.this.finish();
                    }
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWebView.tearDown();
    }
}
