package com.example.proga;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SystemEvent {
    private final long timestamp;
    private final String type;
    private final String value;
    private final float percentage;

    public SystemEvent(String type, String value, float percentage) {
        this.timestamp = System.currentTimeMillis();
        this.type = type;
        this.value = value;
        this.percentage = percentage;
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public float getPercentage() {
        return percentage;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        if (percentage >= 0) {
            return String.format("[%s] %s: %s (%.1f%%)", getFormattedTime(), type, value, percentage);
        } else {
            return String.format("[%s] %s: %s", getFormattedTime(), type, value);
        }
    }
} 