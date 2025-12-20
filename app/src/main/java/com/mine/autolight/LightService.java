package com.mine.autolight;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

public class LightService extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor lightSensor;
    
    // Smoothing constants
    private static final float ALPHA = 0.2f; // Smoothing factor
    private float mSmoothedLux = -1.0f;
    
    // Change threshold (prevents battery drain from tiny adjustments)
    private static final int MIN_CHANGE_THRESHOLD = 5; 
    private int mLastAppliedBrightness = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float currentLux = event.values[0];

            // 1. Apply Smoothing (Low-pass filter)
            if (mSmoothedLux == -1.0f) {
                mSmoothedLux = currentLux; // First reading
            } else {
                mSmoothedLux = (mSmoothedLux * (1.0f - ALPHA)) + (currentLux * ALPHA);
            }

            // 2. Calculate Brightness using our new Logarithmic Algorithm
            int targetBrightness = BrightnessAlgorithm.calculateBrightness(mSmoothedLux);

            // 3. Only update if the change is significant (Hysteresis)
            if (Math.abs(targetBrightness - mLastAppliedBrightness) >= MIN_CHANGE_THRESHOLD) {
                updateSystemBrightness(targetBrightness);
            }
        }
    }

    private void updateSystemBrightness(int brightness) {
        try {
            Settings.System.putInt(getContentResolver(), 
                    Settings.System.SCREEN_BRIGHTNESS, brightness);
            mLastAppliedBrightness = brightness;
            Log.d("AutoLight", "Brightness updated to: " + brightness + " for Lux: " + mSmoothedLux);
        } catch (Exception e) {
            Log.e("AutoLight", "Error writing settings. Check WRITE_SETTINGS permission.");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}