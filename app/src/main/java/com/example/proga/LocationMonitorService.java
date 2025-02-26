package com.example.proga;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import android.util.Log;

public class LocationMonitorService extends Service {
    private static final String TAG = "LocationMonitorService";
    private static final String CHANNEL_ID = "LocationMonitorChannel";
    private static final int NOTIFICATION_ID = 2;
    
    private boolean isRunning = false;
    private Thread monitorThread;
    private Handler mainHandler;
    private NotificationManager notificationManager;
    private LocationManager locationManager;
    private static final String ACTION_UPDATE = "com.example.proga.ACTION_LOCATION_UPDATE";
    private String lastStatus = "";

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startMonitoring();
        return START_STICKY;
    }

    private void startMonitoring() {
        if (isRunning) return;

        isRunning = true;
        monitorThread = new Thread(() -> {
            while (isRunning) {
                checkLocationUsage();
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

    private void checkLocationUsage() {
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        
        String status;
        if (isGpsEnabled || isNetworkEnabled) {
            status = "Геолокация используется";
        } else {
            status = "Геолокация не используется";
        }

        if (!status.equals(lastStatus)) {
            updateNotification(status);
            broadcastUpdate(status);
            lastStatus = status;
        }
    }

    private void broadcastUpdate(String status) {
        Intent intent = new Intent(ACTION_UPDATE);
        intent.putExtra("status", status);
        sendBroadcast(intent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Мониторинг Геолокации",
                    NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String status) {
        Intent notificationIntent = new Intent(this, LocationMonitorActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Мониторинг геолокации")
                .setContentText(status)
                .setSmallIcon(R.drawable.ic_location)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void updateNotification(String status) {
        mainHandler.post(() -> {
            Notification notification = buildNotification(status);
            notificationManager.notify(NOTIFICATION_ID, notification);
        });
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