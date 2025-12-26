package com.mine.autolight;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

/**
 * Handles the actual sensor logic and brightness adjustments.
 * This class is managed by LightService.
 */
public class LightControl implements SensorEventListener {
    private static final String TAG = "AutoLight_Control";
    private final Context context;
    private final SensorManager sensorManager;
    private final Sensor lightSensor;
    private final MySettings settings;
    private final ServiceReceiver receiver;

    public LightControl(Context context) {
        this.context = context;
        this.settings = new MySettings(context);
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        this.receiver = new ServiceReceiver();
    }

    /**
     * Called by LightService onStartCommand.
     * Starts listening to the sensor and internal broadcasts.
     */
    public void register() {
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
            Log.d(TAG, "Sensor listener registered.");
        } else {
            Log.e(TAG, "Light sensor not found on this device.");
        }

        // Register receiver to listen for settings updates from MainActivity
        IntentFilter filter = new IntentFilter(Constants.SERVICE_INTENT_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires Exported/NotExported flags for receivers
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(receiver, filter);
        }
    }

    /**
     * Called by LightService onDestroy.
     * Stops all listeners to save battery.
     */
    public void unregister() {
        sensorManager.unregisterListener(this);
        try {
            context.unregisterReceiver(receiver);
        } catch (Exception e) {
            Log.w(TAG, "Receiver was not registered or already unregistered.");
        }
        Log.d(TAG, "Listeners unregistered.");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float lux = event.values[0];
            adjustBrightness(lux);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for light sensing
    }

    private void adjustBrightness(float lux) {
        int targetBrightness = calculateBrightness(lux);
        
        try {
            // Check if we still have WRITE_SETTINGS permission
            if (Settings.System.canWrite(context)) {
                Settings.System.putInt(context.getContentResolver(), 
                        Settings.System.SCREEN_BRIGHTNESS, targetBrightness);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to update brightness: " + e.getMessage());
        }
    }

    /**
     * Determines target brightness based on user-defined thresholds.
     */
    private int calculateBrightness(float lux) {
        if (lux <= settings.l1) return settings.b1;
        if (lux <= settings.l2) return settings.b2;
        if (lux <= settings.l3) return settings.b3;
        return settings.b4;
    }

    /**
     * Inner class to handle messages sent from MainActivity (like "Save Settings" or "Ping").
     */
    private class ServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int payload = intent.getIntExtra(Constants.SERVICE_INTENT_EXTRA, 0);
            
            if (payload == Constants.SERVICE_INTENT_PAYLOAD_SET) {
                // User clicked "Save" in UI, reload thresholds from SharedPreferences
                settings.load();
                Log.d(TAG, "Settings reloaded in service.");
            } else if (payload == Constants.SERVICE_INTENT_PAYLOAD_PING) {
                Log.d(TAG, "Service pinged by Activity.");
            }
        }
    }
}
