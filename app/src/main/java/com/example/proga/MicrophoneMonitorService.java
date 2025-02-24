package com.example.proga;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
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
import androidx.core.app.NotificationManagerCompat;

import java.util.List;

import android.Manifest;
import android.app.ActivityManager;

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
                        String message;
                        if (isMicrophoneInUse) {
                            // Получаем имя приложения
                            String appName = getAppName(configs);
                            message = "Микрофон используется: " + appName;
                        } else {
                            message = "Мониторинг микрофона активен";
                        }
                        Log.d(TAG, "Microphone state changed: " + message);
                        if (isMicrophoneInUse) {
                            showHighPriorityNotification(message);
                        } else {
                            updateNotification(message);
                        }
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

    private String getAppName(List<AudioRecordingConfiguration> configs) {
        if (configs.isEmpty()) {
            return "Неизвестное приложение";
        }

        AudioRecordingConfiguration config = configs.get(0);
        String ownPackageName = getPackageName();
        
        try {
            // Получаем список активных приложений
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            
            // Проверяем приложение на переднем плане
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            if (taskInfo != null && !taskInfo.isEmpty()) {
                String foregroundPackageName = taskInfo.get(0).topActivity.getPackageName();
                if (!foregroundPackageName.equals(ownPackageName) && 
                    hasRecordAudioPermission(foregroundPackageName)) {
                    try {
                        PackageManager pm = getPackageManager();
                        ApplicationInfo appInfo = pm.getApplicationInfo(foregroundPackageName, 0);
                        String appName = pm.getApplicationLabel(appInfo).toString();
                        Log.d(TAG, "Foreground app with RECORD_AUDIO permission: " + appName);
                        return appName;
                    } catch (Exception e) {
                        Log.e(TAG, "Error getting foreground app name", e);
                    }
                }
            }
            
            // Если приложение на переднем плане не использует микрофон,
            // проверяем все запущенные процессы
            List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
            if (processes != null) {
                for (ActivityManager.RunningAppProcessInfo process : processes) {
                    if (!process.processName.equals(ownPackageName) && 
                        process.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE &&
                        hasRecordAudioPermission(process.processName)) {
                        try {
                            PackageManager pm = getPackageManager();
                            ApplicationInfo appInfo = pm.getApplicationInfo(process.processName, 0);
                            String appName = pm.getApplicationLabel(appInfo).toString();
                            Log.d(TAG, "Found app with RECORD_AUDIO permission: " + appName);
                            return appName;
                        } catch (Exception e) {
                            Log.e(TAG, "Error getting app name", e);
                        }
                    }
                }
            }
            
            // Если не удалось определить приложение, используем информацию об аудио источнике
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                int audioSource = config.getAudioSource();
                Log.d(TAG, "Audio source: " + audioSource);
                
                switch (audioSource) {
                    case MediaRecorder.AudioSource.MIC:
                        return "Приложение записи звука";
                    case MediaRecorder.AudioSource.VOICE_RECOGNITION:
                        return "Голосовой помощник";
                    case MediaRecorder.AudioSource.VOICE_COMMUNICATION:
                        return "Приложение для звонков";
                    case MediaRecorder.AudioSource.CAMCORDER:
                        return "Камера";
                    case MediaRecorder.AudioSource.VOICE_CALL:
                        return "Телефон";
                    default:
                        return "Аудио приложение";
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting app info", e);
        }

        return "Неизвестное приложение";
    }

    // Проверяет, есть ли у приложения разрешение на запись аудио
    private boolean hasRecordAudioPermission(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            
            if (packageInfo.requestedPermissions != null) {
                for (String permission : packageInfo.requestedPermissions) {
                    if (Manifest.permission.RECORD_AUDIO.equals(permission)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking permissions for " + packageName, e);
        }
        return false;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Мониторинг микрофона",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Уведомления об использовании микрофона");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String text) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Мониторинг микрофона")
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();
    }

    private void showHighPriorityNotification(String text) {
        Notification notification = createNotification(text);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        } else {
            Log.e(TAG, "Missing POST_NOTIFICATIONS permission");
        }
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