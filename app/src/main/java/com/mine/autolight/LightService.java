package com.mine.autolight;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

/**
 * Service that runs in the foreground to monitor ambient light.
 * Optimized for Android 15 (API 35).
 */
public class LightService extends Service {
    private static final String CHANNEL_ID = "AutoLightChannel";
    private LightControl lightControl;

    // Static boolean allows MainActivity to check status instantly
    public static boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        lightControl = new LightControl(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Build the required Foreground Notification
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Auto-Light Active")
                .setContentText("Monitoring light levels...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();

        // Handle Foreground Service Types for Android 14+
        int type = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            type = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
        }

        try {
            // ServiceCompat handles the internal plumbing for different Android versions
            ServiceCompat.startForeground(this, 1, notification, type);
        } catch (Exception e) {
            // If the service fails to start as foreground, we shouldn't keep it alive
            isRunning = false;
            stopSelf();
        }

        if (lightControl != null) {
            lightControl.register();
        }

        // START_STICKY ensures the OS attempts to restart the service if it's killed for memory
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, 
                    "Light Service Channel",
                    NotificationManager.IMPORTANCE_LOW);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (lightControl != null) {
            lightControl.unregister();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
