package com.example.proga;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.util.ArrayList;
import java.util.List;

public class MicrophoneMonitorActivity extends AppCompatActivity {
    private TextView statusText;
    private TextView logText;
    private Button startButton;
    private Button stopButton;
    private List<MicrophoneEvent> eventHistory;
    private BroadcastReceiver updateReceiver;
    private static final String ACTION_UPDATE = "com.example.proga.ACTION_UPDATE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_microphone_monitor);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Мониторинг Микрофона");

        statusText = findViewById(R.id.statusText);
        logText = findViewById(R.id.logText);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        eventHistory = new ArrayList<>();

        startButton.setOnClickListener(v -> {
            startMonitoring();
            updateStatus(true);
        });

        stopButton.setOnClickListener(v -> {
            stopMonitoring();
            updateStatus(false);
        });

        setupUpdateReceiver();
    }

    private void setupUpdateReceiver() {
        updateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String status = intent.getStringExtra("status");
                String appName = intent.getStringExtra("appName");
                addEventToHistory(new MicrophoneEvent(status, appName));
            }
        };
    }

    private void addEventToHistory(MicrophoneEvent event) {
        eventHistory.add(event);
        updateLogText();
    }

    private void updateLogText() {
        StringBuilder sb = new StringBuilder();
        for (MicrophoneEvent event : eventHistory) {
            sb.append(event.toString()).append("\n");
        }
        logText.setText(sb.toString());
    }

    private void startMonitoring() {
        MainActivity.checkPermissionsAndStartService(this);
        registerReceiver(updateReceiver, new IntentFilter(ACTION_UPDATE));
    }

    private void stopMonitoring() {
        MainActivity.stopMonitoringService(this);
        try {
            unregisterReceiver(updateReceiver);
        } catch (IllegalArgumentException e) {
            // Приемник может быть уже не зарегистрирован
        }
    }

    private void updateStatus(boolean isActive) {
        statusText.setText("Статус: " + (isActive ? "Активен" : "Не активен"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(updateReceiver);
        } catch (IllegalArgumentException e) {
            // Приемник может быть уже не зарегистрирован
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 