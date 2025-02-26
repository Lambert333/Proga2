package com.example.proga;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MicrophoneEvent {
    private final long timestamp;
    private final String status;
    private final String appName;

    public MicrophoneEvent(String status, String appName) {
        this.timestamp = System.currentTimeMillis();
        this.status = status;
        this.appName = appName;
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    @Override
    public String toString() {
        if (appName.isEmpty()) {
            return String.format("[%s] %s", getFormattedTime(), status);
        } else {
            return String.format("[%s] %s (%s)", getFormattedTime(), status, appName);
        }
    }
} 