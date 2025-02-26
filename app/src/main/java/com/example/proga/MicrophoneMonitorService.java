package com.example.proga;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.AudioRecordingConfiguration;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import java.util.List;
import android.app.ActivityManager;

public class MicrophoneMonitorService extends Service {
    private static final String TAG = "MicrophoneMonitorService";
    private static final String CHANNEL_ID = "MicrophoneMonitorChannel";
    private static final int NOTIFICATION_ID = 1;
    
    private boolean isRunning = false;
    private Thread monitorThread;
    private Handler mainHandler;
    private NotificationManager notificationManager;
    private AudioManager audioManager;
    private static final String ACTION_UPDATE = "com.example.proga.ACTION_UPDATE";
    private String lastStatus = "";
    private String lastAppName = "";

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
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
                checkMicrophoneUsage();
                try {
                    Thread.sleep(1000); // Проверка каждую секунду
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        monitorThread.start();

        startForeground(NOTIFICATION_ID, buildNotification("Мониторинг запущен", ""));
    }

    private void checkMicrophoneUsage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            List<AudioRecordingConfiguration> recordings = audioManager.getActiveRecordingConfigurations();
            if (!recordings.isEmpty()) {
                String status = "Микрофон используется";
                if (!status.equals(lastStatus)) {
                    updateNotification(status, "");
                    broadcastUpdate(status, "");
                    lastStatus = status;
                    lastAppName = "";
                }
            } else {
                String status = "Микрофон не используется";
                if (!status.equals(lastStatus)) {
                    updateNotification(status, "");
                    broadcastUpdate(status, "");
                    lastStatus = status;
                    lastAppName = "";
                }
            }
        } else {
            boolean isMicrophoneInUse = isMicrophoneInUse();
            String status = isMicrophoneInUse ? 
                "Микрофон используется" : 
                "Микрофон не используется";
            
            if (!status.equals(lastStatus)) {
                updateNotification(status, "");
                broadcastUpdate(status, "");
                lastStatus = status;
                lastAppName = "";
            }
        }
    }

    private boolean isMicrophoneInUse() {
        try {
            // Пытаемся создать MediaRecorder
            MediaRecorder recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            recorder.setOutputFile("/dev/null");
            
            try {
                recorder.prepare();
                recorder.start();
                recorder.stop();
                recorder.release();
                return false; // Микрофон свободен
            } catch (Exception e) {
                recorder.release();
                return true; // Микрофон используется
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking microphone state", e);
            return false;
        }
    }

    private String getAppNameFromPackage(String packageName) {
        PackageManager pm = getPackageManager();
        String appName = "Неизвестное приложение";
        
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            appName = pm.getApplicationLabel(ai).toString();
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting app name", e);
        }
        
        return appName;
    }

    private void broadcastUpdate(String status, String appName) {
        Intent intent = new Intent(ACTION_UPDATE);
        intent.putExtra("status", status);
        intent.putExtra("appName", appName);
        sendBroadcast(intent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Мониторинг Микрофона",
                    NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String status, String appName) {
        Intent notificationIntent = new Intent(this, MicrophoneMonitorActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        String contentText = appName.isEmpty() ? status : status + " (" + appName + ")";

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Мониторинг микрофона")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_mic)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void updateNotification(String status, String appName) {
        mainHandler.post(() -> {
            Notification notification = buildNotification(status, appName);
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