package com.example.proga;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SystemMonitorActivity extends AppCompatActivity {
    private TextView statusText, cpuText, ramText, batteryText, storageText;
    private Button startButton, stopButton;
    private LineChart cpuChart, ramChart, batteryChart, storageChart;
    private BroadcastReceiver updateReceiver;
    private static final String ACTION_UPDATE = "com.example.proga.ACTION_SYSTEM_UPDATE";
    private Map<String, LineChart> charts;
    private Map<String, Integer> chartColors;
    private static final int MAX_ENTRIES = 60; // 1 минута данных

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_monitor);

        setupToolbar();
        initializeViews();
        setupCharts();
        setupUpdateReceiver();
        setupButtons();
        
        // Проверяем, запущен ли сервис
        if (isServiceRunning(SystemMonitorService.class)) {
            updateStatus(true);
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Системный Мониторинг");
    }

    private void initializeViews() {
        statusText = findViewById(R.id.statusText);
        cpuText = findViewById(R.id.cpuText);
        ramText = findViewById(R.id.ramText);
        batteryText = findViewById(R.id.batteryText);
        storageText = findViewById(R.id.storageText);
        
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        
        cpuChart = findViewById(R.id.cpuChart);
        ramChart = findViewById(R.id.ramChart);
        batteryChart = findViewById(R.id.batteryChart);
        storageChart = findViewById(R.id.storageChart);

        charts = new HashMap<>();
        charts.put("CPU", cpuChart);
        charts.put("RAM", ramChart);
        charts.put("Battery", batteryChart);
        charts.put("Storage", storageChart);

        chartColors = new HashMap<>();
        chartColors.put("CPU", Color.RED);
        chartColors.put("RAM", Color.BLUE);
        chartColors.put("Battery", Color.GREEN);
        chartColors.put("Storage", Color.MAGENTA);
    }

    private void setupChart(LineChart chart, String label) {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(false);
        chart.setDragEnabled(false);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setDrawGridBackground(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setEnabled(false);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMaximum(100f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setGridColor(Color.GRAY);

        chart.getAxisRight().setEnabled(false);
        
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);
        chart.setData(data);

        chart.getLegend().setEnabled(false);
    }

    private void setupCharts() {
        for (Map.Entry<String, LineChart> entry : charts.entrySet()) {
            setupChart(entry.getValue(), entry.getKey());
        }
    }

    private void setupUpdateReceiver() {
        updateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String type = intent.getStringExtra("type");
                String value = intent.getStringExtra("value");
                float percentage = intent.getFloatExtra("percentage", 0f);
                
                updateChart(type, percentage);
                updateText(type, value);
            }
        };
    }

    private void updateChart(String type, float percentage) {
        LineChart chart = charts.get(type);
        if (chart != null) {
            LineData data = chart.getData();
            ILineDataSet set = data.getDataSetByIndex(0);

            if (set == null) {
                set = createSet(type);
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount(), percentage), 0);

            if (set.getEntryCount() > MAX_ENTRIES) {
                set.removeEntry(0);
            }

            data.notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.moveViewToX(data.getEntryCount());
        }
    }

    private void updateText(String type, String value) {
        switch (type) {
            case "CPU":
                cpuText.setText(value);
                break;
            case "RAM":
                ramText.setText(value);
                break;
            case "Battery":
                batteryText.setText(value);
                break;
            case "Storage":
                storageText.setText(value);
                break;
        }
    }

    private LineDataSet createSet(String type) {
        LineDataSet set = new LineDataSet(null, type);
        set.setColor(chartColors.get(type));
        set.setLineWidth(2f);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawFilled(true);
        set.setFillAlpha(50);
        set.setFillColor(chartColors.get(type));
        return set;
    }

    private void setupButtons() {
        startButton.setOnClickListener(v -> {
            startMonitoring();
            updateStatus(true);
        });

        stopButton.setOnClickListener(v -> {
            stopMonitoring();
            updateStatus(false);
        });
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(updateReceiver, new IntentFilter(ACTION_UPDATE));
        if (isServiceRunning(SystemMonitorService.class)) {
            updateStatus(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(updateReceiver);
        } catch (IllegalArgumentException e) {
            // Приемник может быть уже не зарегистрирован
        }
    }

    private void startMonitoring() {
        Intent serviceIntent = new Intent(this, SystemMonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        registerReceiver(updateReceiver, new IntentFilter(ACTION_UPDATE));
    }

    private void stopMonitoring() {
        Intent serviceIntent = new Intent(this, SystemMonitorService.class);
        stopService(serviceIntent);
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