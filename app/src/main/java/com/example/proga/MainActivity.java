package com.example.proga;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.POST_NOTIFICATIONS
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(v -> checkPermissionsAndStartService());

        Button stopButton = findViewById(R.id.stopButton);
        stopButton.setOnClickListener(v -> stopMonitoringService());
    }

    private void checkPermissionsAndStartService() {
        boolean allPermissionsGranted = true;
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
        } else {
            startMonitoringService();
        }
    }

    private void startMonitoringService() {
        try {
            Intent serviceIntent = new Intent(this, MicrophoneMonitorService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Toast.makeText(this, "Мониторинг микрофона запущен", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error starting service", e);
            Toast.makeText(this, "Ошибка запуска сервиса: " + e.getMessage(), 
                Toast.LENGTH_LONG).show();
        }
    }

    private void stopMonitoringService() {
        Intent serviceIntent = new Intent(this, MicrophoneMonitorService.class);
        stopService(serviceIntent);
        Toast.makeText(this, "Мониторинг остановлен", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, 
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                startMonitoringService();
            } else {
                Toast.makeText(this, 
                    "Необходимы разрешения для работы приложения", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }
}