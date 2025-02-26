package com.example.proga;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LocationEvent {
    private final long timestamp;
    private final String status;

    public LocationEvent(String status) {
        this.timestamp = System.currentTimeMillis();
        this.status = status;
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", getFormattedTime(), status);
    }
} 