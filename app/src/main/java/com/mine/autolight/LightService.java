package com.mine.autolight;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

public class LightService extends Service implements SensorEventListener {

    private static final String TAG = "AutoLightService";
    private static final String CHANNEL_ID = "AutoLightChannel";
    
    private SensorManager sensorManager;
    private Sensor lightSensor;

    // Smoothing & Logic Constants
    private static final float ALPHA = 0.2f; 
    private float mSmoothedLux = -1.0f;
    private int mLastAppliedBrightness = -1;
    private static final int MIN_CHANGE_THRESHOLD = 3; 

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Start Foreground to comply with Android background policies
        startForegroundServiceCompat();

        // Initialize Light Sensor
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            if (lightSensor != null) {
                sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    private void startForegroundServiceCompat() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Auto-Brightness Service", 
                    NotificationManager.IMPORTANCE_LOW);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        Notification notification = builder
                .setContentTitle("Auto-Light Active")
                .setContentText("Adjusting screen brightness automatically")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .build();

        startForeground(1, notification);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float currentLux = event.values[0];

            // 1. Smoothing logic
            if (mSmoothedLux == -1.0f) {
                mSmoothedLux = currentLux;
            } else {
                mSmoothedLux = (mSmoothedLux * (1.0f - ALPHA)) + (currentLux * ALPHA);
            }

            // 2. Calculate using your Algorithm and MySettings points
            int targetBrightness = BrightnessAlgorithm.calculateBrightness(mSmoothedLux);

            // 3. Apply if change is significant
            if (Math.abs(targetBrightness - mLastAppliedBrightness) > MIN_CHANGE_THRESHOLD) {
                applyBrightness(targetBrightness);
            }
        }
    }

    private void applyBrightness(int brightness) {
        try {
            Settings.System.putInt(getContentResolver(), 
                    Settings.System.SCREEN_BRIGHTNESS, brightness);
            mLastAppliedBrightness = brightness;
            Log.d(TAG, "Applied: " + brightness + " | Lux: " + mSmoothedLux);
        } catch (Exception e) {
            Log.e(TAG, "Error writing settings: " + e.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        super.onDestroy();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
