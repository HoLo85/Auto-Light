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

    private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            // FIX 1: Restore "GET DATA" and "SAVE" communication
            if (Constants.SERVICE_INTENT_ACTION.equals(action)) {
                int payload = intent.getIntExtra(Constants.SERVICE_INTENT_EXTRA, -1);
                if (payload == Constants.SERVICE_INTENT_PAYLOAD_PING) {
                    String status = "Lux: " + lightControl.getLastSensorValue() + 
                                    " | Brightness: " + lightControl.getSetBrightness();
                    Toast.makeText(context, status, Toast.LENGTH_SHORT).show();
                } else if (payload == Constants.SERVICE_INTENT_PAYLOAD_SET) {
                    lightControl.reconfigure();
                }
                return;
            }

            // FIX 2: Update Orientation State
            boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
            lightControl.setLandscape(isLandscape);

            // FIX 3: Restore Mode Logic
            if (Intent.ACTION_USER_PRESENT.equals(action)) {
                if (settings.mode == Constants.WORK_MODE_UNLOCK) {
                    lightControl.onScreenUnlock();
                }
            } else if (Intent.ACTION_CONFIGURATION_CHANGED.equals(action)) {
                // Trigger a refresh when the phone rotates
                lightControl.startListening();
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
        filter.addAction(Constants.SERVICE_INTENT_ACTION); // Listen for UI buttons
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(eventReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(eventReceiver, filter);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        
        Notification.Builder builder = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ? 
                new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);

        Notification notification = builder
                .setContentTitle("Auto Light Active")
                .setContentText("Monitoring light levels")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        // Start listening if Always mode is active
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
