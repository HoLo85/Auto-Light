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

public class LightService extends Service {
    private static final String CHANNEL_ID = "AutoLightChannel";
    private static final int NOTIFICATION_ID = 1;
    private LightControl lightControl;

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize your control logic
        lightControl = new LightControl(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 1. Build the Notification (Required for Android 8+)
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Auto-Light Active")
                .setContentText("Monitoring ambient light levels")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW) // Minimal noise for user
                .setOngoing(true)
                .build();

        // 2. Start Foreground with compatibility for Android 14 (UPSIDE_DOWN_CAKE)
        int foregroundType = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            foregroundType = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
        }

        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, foregroundType);
        } catch (Exception e) {
            // Log error if needed: Service failed to start in foreground
        }

        // 3. Register your existing sensor logic
        lightControl.register();

        // START_STICKY ensures the OS attempts to restart the service if killed
        return START_STICKY;
    }

    private void createNotificationChannel() {
        // Notification Channels are required for Android 8.0 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Auto-Light Background Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Enables automatic brightness adjustment in the background.");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        // Clean up sensors to save battery
        if (lightControl != null) {
            lightControl.unregister();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // This is a started service, not a bound one
    }
}
