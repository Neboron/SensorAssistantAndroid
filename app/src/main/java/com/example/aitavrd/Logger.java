package com.example.aitavrd;

import android.widget.ScrollView;
import android.widget.TextView;

public class Logger {

    private TextView logTextView;
    private ScrollView scrollViewLog;

    public Logger(TextView logTextView, ScrollView scrollViewLog) {
        this.logTextView = logTextView;
        this.scrollViewLog = scrollViewLog;
    }

    public void log(String message) {
        if (logTextView != null && scrollViewLog != null) {
            logTextView.post(() -> {
                logTextView.append(message + "\n");
                scrollViewLog.post(() -> scrollViewLog.fullScroll(ScrollView.FOCUS_DOWN));
            });
        }
    }
}
