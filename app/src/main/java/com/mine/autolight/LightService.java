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
import android.widget.Toast;

public class LightService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "AutoLightServiceChannel";

    private MySettings settings;
    private LightControl lightControl;

    // RESTORED: This receiver now correctly handles mode switching
    private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            // Handle UI Communication (GET DATA / PING)
            if (Constants.SERVICE_INTENT_ACTION.equals(action)) {
                int payload = intent.getIntExtra(Constants.SERVICE_INTENT_EXTRA, -1);
                handleUiCommand(payload);
                return;
            }

            // Update landscape state for LightControl
            boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
            lightControl.setLandscape(isLandscape);

            // MODE LOGIC RESTORED
            if (Intent.ACTION_USER_PRESENT.equals(action)) {
                if (settings.mode == Constants.WORK_MODE_UNLOCK) {
                    lightControl.onScreenUnlock();
                }
            } else if (Intent.ACTION_CONFIGURATION_CHANGED.equals(action)) {
                // If mode is Landscape or Portrait, we need to re-evaluate
                if (settings.mode == Constants.WORK_MODE_LANDSCAPE || settings.mode == Constants.WORK_MODE_PORTRAIT) {
                    lightControl.startListening(); 
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        settings = new MySettings(this);
        lightControl = new LightControl(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        filter.addAction(Constants.SERVICE_INTENT_ACTION); // Allow UI to talk to Service
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(eventReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(eventReceiver, filter);
        }
    }

    private void handleUiCommand(int payload) {
        if (payload == Constants.SERVICE_INTENT_PAYLOAD_PING) {
            // RESTORED: Feedback for the "GET DATA" button
            String status = "Lux: " + lightControl.getLastSensorValue() + 
                            " | Bright: " + lightControl.getSetBrightness();
            Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
        } else if (payload == Constants.SERVICE_INTENT_PAYLOAD_SET) {
            settings.load();
            lightControl.reconfigure();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, getNotification());

        // Initial check for Always mode
        if (settings.mode == Constants.WORK_MODE_ALWAYS) {
            lightControl.startListening();
        }
        
        return START_STICKY;
    }

    private Notification getNotification() {
        Notification.Builder builder = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ? 
                new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);

        return builder
                .setContentTitle("Auto Light Active")
                .setContentText("Mode: " + getModeName())
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true)
                .build();
    }

    private String getModeName() {
        if (settings.mode == Constants.WORK_MODE_ALWAYS) return "Always";
        if (settings.mode == Constants.WORK_MODE_UNLOCK) return "Unlock";
        if (settings.mode == Constants.WORK_MODE_LANDSCAPE) return "Landscape";
        if (settings.mode == Constants.WORK_MODE_PORTRAIT) return "Portrait";
        return "Unknown";
    }

    // Existing helper methods...
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
    
    private void startForeground(int id, Notification notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            super.startForeground(id, notification);
        }
    }
}
