package com.lukekorth.auto_fi;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.lukekorth.auto_fi.utilities.TextViewAppender;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;

public class LogActivity extends AppCompatActivity implements TextWatcher {

    private boolean mAutoScroll;
    private ScrollView mScrollView;
    private Logger mLogger;
    private TextViewAppender mAppender;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mAutoScroll = true;
        mScrollView = findViewById(R.id.scroll_view);

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%date{HH:mm:ss} %-5level %msg%n");
        encoder.start();

        TextView textView = findViewById(R.id.logs);
        textView.addTextChangedListener(this);
        mAppender = new TextViewAppender(textView);
        mAppender.setContext(loggerContext);
        mAppender.setEncoder(encoder);
        mAppender.start();

        mLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        mLogger.addAppender(mAppender);
    }

    @Override
    public void afterTextChanged(Editable editable) {
        if (mAutoScroll) {
            mScrollView.post(new Runnable() {
                public void run() {
                    mScrollView.fullScroll(View.FOCUS_DOWN);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mLogger.detachAppender(mAppender);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.log_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.auto_scroll) {
            item.setChecked(!item.isChecked());
            mAutoScroll = item.isChecked();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
}
