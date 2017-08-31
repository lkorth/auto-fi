package com.lukekorth.auto_fi.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.lukekorth.auto_fi.R;

import java.util.ArrayList;

public class LogAdapter extends ArrayAdapter<String> {

    public LogAdapter(@NonNull Context context) {
        super(context, R.layout.log_line, new ArrayList<String>());
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        if (position % 2 == 1) {
            view.setBackgroundResource(R.color.alternating_list_background);
        } else {
            view.setBackgroundResource(0);
        }

        return view;
    }
}
