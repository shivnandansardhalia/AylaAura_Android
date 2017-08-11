package com.aylanetworks.aura.test;
/*
 * Aura_Android
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.aylanetworks.aura.MainActivity;
import com.aylanetworks.aura.R;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestRunnerLogAdapter extends
        RecyclerView.Adapter <TestRunnerLogAdapter.LogItemViewHolder> {

    private List<LogEntry> _logEntryList;
    private DateFormat _dateFormat = DateFormat.getDateTimeInstance();
    private Handler _loggerHandler = new Handler(Looper.getMainLooper());
    private int _maxLogEntries = 1000;

    public TestRunnerLogAdapter(List<LogEntry> logEntries) {
        if (logEntries != null) {
            _logEntryList = new ArrayList<>(logEntries);
            Collections.reverse(_logEntryList);
        } else {
            _logEntryList = new ArrayList<>();
        }
    }

    public void addEntry(final LogEntry entry) {
        _loggerHandler.post(new Runnable() {
            @Override
            public void run() {
                while (_logEntryList.size() >= _maxLogEntries) {
                    _logEntryList.remove(_logEntryList.size() - 1);
                }
                _logEntryList.add(0, entry);
                notifyDataSetChanged();
            }
        });
    }

    public void clear() {
        _loggerHandler.post(new Runnable() {
            @Override
            public void run() {
                _logEntryList.clear();
                notifyDataSetChanged();
            }
        });
    }

    public int getMaxLogEntries() {
        return _maxLogEntries;
    }

    public void setMaxLogEntries(int maxLogEntries) {
        _maxLogEntries = maxLogEntries;
    }

    @Override
    public LogItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) MainActivity.sharedInstance()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.log_item_cardview, parent, false);
        return new LogItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(LogItemViewHolder holder, int position) {
        LogEntry entry = _logEntryList.get(position);

        // Icon
        holder.iconView.setVisibility(entry.getLogType() == LogEntry.LogType.Message ?
                View.INVISIBLE : View.VISIBLE);
        Drawable iconDrawable = null;
        Context context = MainActivity.sharedInstance();

        switch(entry.getLogType()) {
            case Message:
                // Not shown
                break;

            case Error:
                iconDrawable = ContextCompat.getDrawable(context, R.drawable.log_error);
                break;

            case Fail:
                iconDrawable = ContextCompat.getDrawable(context, R.drawable.log_fail);
                break;

            case Info:
                iconDrawable = ContextCompat.getDrawable(context, R.drawable.log_info);
                break;

            case Pass:
                iconDrawable = ContextCompat.getDrawable(context, R.drawable.log_pass);
                break;

            case Warning:
                iconDrawable = ContextCompat.getDrawable(context, R.drawable.log_warning);
                break;
        }

        if (iconDrawable != null) {
            holder.iconView.setImageDrawable(iconDrawable);
        }

        holder.timestampView.setText(_dateFormat.format(entry.getTimestamp()));
        holder.messageView.setText(entry.getMessage());
    }

    @Override
    public int getItemCount() {
        return _logEntryList.size();
    }

    public class LogItemViewHolder extends RecyclerView.ViewHolder {
        public ImageView iconView;
        public TextView timestampView;
        public TextView messageView;

        public LogItemViewHolder(View itemView) {
            super(itemView);
            iconView = (ImageView)itemView.findViewById(R.id.icon_image);
            timestampView = (TextView)itemView.findViewById(R.id.timestamp);
            messageView = (TextView)itemView.findViewById(R.id.message_textview);
        }
    }
}
