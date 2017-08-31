package com.lukekorth.auto_fi;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

import com.lukekorth.auto_fi.adapters.LogAdapter;
import com.lukekorth.auto_fi.utilities.ListViewAppender;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

public class LogActivity extends AppCompatActivity {

    private ListView mListView;
    private Logger mLogger;
    private ListViewAppender mAppender;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        LogAdapter arrayAdapter = new LogAdapter(this);

        mListView = findViewById(R.id.logs);
        mListView.setAdapter(arrayAdapter);

        mAppender = new ListViewAppender(arrayAdapter);
        mAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        mAppender.start();

        mLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        mLogger.addAppender(mAppender);
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

            if (item.isChecked()) {
                mListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
            } else {
                mListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
            }

            return true;
        } else if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
