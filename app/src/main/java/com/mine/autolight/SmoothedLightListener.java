package com.mine.autolight;

import android.content.ContentResolver;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.provider.Settings;
import android.util.Log;

public class SmoothedLightListener implements SensorEventListener {

    private static final String TAG = "AutoLightListener";

    private final ContentResolver contentResolver;
    
    // SMOOTHING CONFIGURATION
    // Alpha determines the weight of new data vs old data.
    // 0.2f = 20% new data, 80% history. Lower = smoother/slower. Higher = twitchier/faster.
    private static final float ALPHA = 0.2f;
    private float mSmoothedLux = 0.0f;

    // UPDATE THRESHOLD
    // Minimum change in brightness level (0-255) required to trigger a system update.
    // Prevents spamming system settings for invisible changes (e.g., level 100 vs 101).
    private static final int MIN_CHANGE_THRESHOLD = 3;
    private int mLastSetBrightness = -1;

    public SmoothedLightListener(Context context) {
        this.contentResolver = context.getContentResolver();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float currentRawLux = event.values[0];

            // 1. APPLY LOW-PASS FILTER (Smoothing)
            // If this is the very first reading, jump straight to it so we don't fade in from 0.
            if (mSmoothedLux == 0.0f) {
                mSmoothedLux = currentRawLux;
            } else {
                mSmoothedLux = (mSmoothedLux * (1.0f - ALPHA)) + (currentRawLux * ALPHA);
            }

            // 2. CALCULATE TARGET BRIGHTNESS
            // Uses the Segmented Logarithmic Algorithm from the previous step
            int targetBrightness = BrightnessAlgorithm.calculateBrightness(mSmoothedLux);

            // 3. APPLY TO SYSTEM (With Hysteresis/Threshold)
            // Only write to settings if the change is significant enough to notice.
            if (Math.abs(targetBrightness - mLastSetBrightness) > MIN_CHANGE_THRESHOLD) {
                applyBrightness(targetBrightness);
            }
        }
    }

    private void applyBrightness(int brightness) {
        try {
            // Ensure we are within valid bounds (0-255)
            // Note: 0 might turn the screen off on some devices, so MIN_BRIGHTNESS in Algorithm handles this.
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
            
            // Update state
            mLastSetBrightness = brightness;
            Log.d(TAG, "Applied Brightness: " + brightness + " (Lux: " + mSmoothedLux + ")");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to set system brightness", e);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Usually not needed for Light Sensors
    }
}