package com.example.proga;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.StatFs;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class SystemMonitorService extends Service {
    private static final String TAG = "SystemMonitorService";
    private static final String CHANNEL_ID = "SystemMonitorChannel";
    private static final int NOTIFICATION_ID = 3;
    
    private boolean isRunning = false;
    private Thread monitorThread;
    private Handler mainHandler;
    private NotificationManager notificationManager;
    private ActivityManager activityManager;
    private static final String ACTION_UPDATE = "com.example.proga.ACTION_SYSTEM_UPDATE";
    private String cpuStatus = "";
    private String ramStatus = "";
    private String batteryStatus = "";
    private String storageStatus = "";

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning) {
            startMonitoring();
        }
        return START_STICKY; // Сервис будет перезапущен системой если будет убит
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        startService(restartServiceIntent);
        super.onTaskRemoved(rootIntent);
    }

    private void startMonitoring() {
        if (isRunning) return;

        isRunning = true;
        monitorThread = new Thread(() -> {
            while (isRunning) {
                checkSystemStats();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        monitorThread.start();

        startForeground(NOTIFICATION_ID, buildNotification("Мониторинг запущен"));
    }

    private void checkSystemStats() {
        // RAM
        MemoryInfo mi = new MemoryInfo();
        activityManager.getMemoryInfo(mi);
        long availableMegs = mi.availMem / 1048576L;
        long totalMegs = mi.totalMem / 1048576L;
        float ramPercentage = ((float)(totalMegs - availableMegs) / totalMegs) * 100;
        ramStatus = String.format("RAM: %.1f%%", ramPercentage);
        broadcastUpdate("RAM", String.format("%d MB / %d MB", totalMegs - availableMegs, totalMegs), ramPercentage);

        // CPU
        float cpuFreq = getCpuFrequency();
        float cpuPercentage = (cpuFreq / 3000.0f) * 100;
        cpuStatus = String.format("CPU: %.1f%%", cpuPercentage);
        broadcastUpdate("CPU", String.format("%.1f MHz", cpuFreq), cpuPercentage);

        // Battery
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        
        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPct = level * 100 / (float)scale;
            this.batteryStatus = String.format("Battery: %.1f%%", batteryPct);
            
            int temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            float tempC = temperature / 10f;
            broadcastUpdate("Battery", String.format("%.1f%% (%.1f°C)", batteryPct, tempC), batteryPct);
        }

        // Storage
        StatFs stat = new StatFs(getDataDirectory().getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        
        long totalSize = totalBlocks * blockSize / (1024 * 1024);
        long availableSize = availableBlocks * blockSize / (1024 * 1024);
        float storagePercentage = ((float)(totalSize - availableSize) / totalSize) * 100;
        storageStatus = String.format("Storage: %.1f%%", storagePercentage);
        broadcastUpdate("Storage", String.format("%d MB / %d MB", totalSize - availableSize, totalSize), storagePercentage);

        // Обновляем уведомление
        updateNotification();
    }

    private float getCpuFrequency() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq"));
            String line = reader.readLine();
            reader.close();
            return Float.parseFloat(line) / 1000; // Конвертируем в МГц
        } catch (IOException e) {
            return 0;
        }
    }

    private File getDataDirectory() {
        return android.os.Environment.getDataDirectory();
    }

    private void broadcastUpdate(String type, String value, float percentage) {
        Intent intent = new Intent(ACTION_UPDATE);
        intent.putExtra("type", type);
        intent.putExtra("value", value);
        intent.putExtra("percentage", percentage);
        sendBroadcast(intent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Системный Мониторинг",
                    NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void updateNotification() {
        String status = String.format("%s | %s | %s | %s", 
            cpuStatus, ramStatus, batteryStatus, storageStatus);
        Notification notification = buildNotification(status);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private Notification buildNotification(String status) {
        Intent notificationIntent = new Intent(this, SystemMonitorActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Системный мониторинг")
                .setContentText(status)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(status))
                .setSmallIcon(R.drawable.ic_system)
                .setContentIntent(pendingIntent)
                .build();
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (monitorThread != null) {
            monitorThread.interrupt();
            try {
                monitorThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping monitor thread", e);
            }
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
} 