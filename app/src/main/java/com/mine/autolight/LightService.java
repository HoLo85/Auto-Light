package com.mine.autolight;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.IBinder;

public class LightService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "AutoLightServiceChannel";

    private MySettings settings;
    private LightControl lightControl;

    private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            
            // Handle Screen Unlock
            if (Intent.ACTION_USER_PRESENT.equals(action)) {
                if (settings.mode == Constants.WORK_MODE_UNLOCK_ROTATE) {
                    lightControl.onScreenUnlock();
                }
            } 
            // Handle Rotation
            else if (Intent.ACTION_CONFIGURATION_CHANGED.equals(action)) {
                boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
                lightControl.setLandscape(isLandscape);
                
                if (settings.mode == Constants.WORK_MODE_UNLOCK_ROTATE) {
                    lightControl.onScreenUnlock(); // Reuse unlock logic to refresh brightness on rotate
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        settings = new MySettings(this);
        lightControl = new LightControl(this);

        // Register for system events
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(eventReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(eventReceiver, filter);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        Notification notification = builder
                .setContentTitle("Auto Light Active")
                .setContentText("Adjusting brightness based on light")
                .setSmallIcon(android.R.drawable.ic_menu_compass) 
                .setOngoing(true)
                .build();

        // Android 14+ requirement
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        // Start listening based on the mode
        if (settings.mode == Constants.WORK_MODE_ALWAYS) {
            lightControl.startListening();
        }

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Light Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        lightControl.stopListening();
        unregisterReceiver(eventReceiver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
