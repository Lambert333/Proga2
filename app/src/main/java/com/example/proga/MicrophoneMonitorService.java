package com.example.proga;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecordingConfiguration;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.List;

import android.Manifest;

public class MicrophoneMonitorService extends Service {
    private static final String TAG = "MicrophoneMonitorService";
    private static final String CHANNEL_ID = "MicrophoneMonitor";
    private static final int NOTIFICATION_ID = 1;
    private static final int FOREGROUND_SERVICE_TYPE_MICROPHONE = 128;
    private AudioManager audioManager;
    private Handler handler;
    private boolean lastMicrophoneState = false;
    private AudioManager.AudioRecordingCallback recordingCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            Log.d(TAG, "Service onCreate");
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            handler = new Handler(Looper.getMainLooper());
            
            createNotificationChannel();
            
            // Создаем callback для отслеживания записи
            recordingCallback = new AudioManager.AudioRecordingCallback() {
                @Override
                public void onRecordingConfigChanged(List<AudioRecordingConfiguration> configs) {
                    boolean isMicrophoneInUse = !configs.isEmpty();
                    Log.d(TAG, "Recording config changed. Active recordings: " + configs.size());
                    
                    if (isMicrophoneInUse != lastMicrophoneState) {
                        lastMicrophoneState = isMicrophoneInUse;
                        String message = isMicrophoneInUse ? 
                            "Микрофон используется" : 
                            "Мониторинг микрофона активен";
                        Log.d(TAG, "Microphone state changed: " + message);
                        updateNotification(message);
                    }
                }
            };
            
            // Регистрируем callback
            audioManager.registerAudioRecordingCallback(recordingCallback, handler);
            
            // Запускаем сервис на переднем плане
            Notification notification = createNotification("Мониторинг микрофона активен");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
            
            Log.d(TAG, "Service started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            stopSelf();
        }
    }

    private void createNotificationChannel() {
        try {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Мониторинг микрофона",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setSound(null, null);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification channel", e);
        }
    }

    private Notification createNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Мониторинг микрофона")
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOnlyAlertOnce(true)
                .build();
    }

    private void updateNotification(String text) {
        try {
            Log.d(TAG, "Updating notification: " + text);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_ID, createNotification(text));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating notification", e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        try {
            Log.d(TAG, "Service onDestroy");
            if (audioManager != null && recordingCallback != null) {
                audioManager.unregisterAudioRecordingCallback(recordingCallback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy", e);
        }
        super.onDestroy();
    }
} 