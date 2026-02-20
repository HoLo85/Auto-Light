package com.mine.autolight;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;

import java.util.ArrayDeque;

public class LightControl implements SensorEventListener {

    private static final String TAG = "LightControl";

    private final LightService lightService;
    private final SensorManager sMgr;
    private final DisplayManager dMgr;
    private final Sensor lightSensor;
    private final MySettings sett;
    private final ContentResolver cResolver;

    // Used only to schedule stopListening after "pause" in non-always modes
    private final Handler delayer = new Handler(Looper.getMainLooper());
    private static final long PAUSE = 2500;

    private boolean onListen = false;
    private boolean landscape = false;
    private boolean needsImmediateUpdate = false;

    private float lastLux = 0;
    private float rawLux = 0;

    // Window smoothing settings
    private final ArrayDeque<SensorReading> buffer = new ArrayDeque<>();
    private static final long WINDOW_MS = 2000;

    // Hysteresis
    private static final float HYSTERESIS_THRESHOLD = 0.15f;

    private float lastAppliedLux = -1f;
    private float rollingSum = 0f;

    LightControl(LightService service) {
        lightService = service;
        sett = new MySettings(service.getApplicationContext());
        cResolver = service.getApplicationContext().getContentResolver();
        sMgr = (SensorManager) service.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sMgr.getDefaultSensor(Sensor.TYPE_LIGHT);
        dMgr = (DisplayManager) service.getApplicationContext().getSystemService(Context.DISPLAY_SERVICE);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (dMgr.getDisplay(0).getState() != Display.STATE_ON ||
            event.sensor.getType() != Sensor.TYPE_LIGHT) {
            return;
        }

        rawLux = event.values[0];
        long now = SystemClock.elapsedRealtime();

        sendBroadcastToService(getLiveBrightnessData());

        buffer.add(new SensorReading(now, rawLux));
        rollingSum += rawLux;

        while (!buffer.isEmpty() && (now - buffer.peekFirst().time) > WINDOW_MS) {
            rollingSum -= buffer.removeFirst().value;
        }

        if (needsImmediateUpdate || sett.mode == Constants.WORK_MODE.UNLOCK) {
            lastLux = rawLux;
            setBrightness((int) lastLux);

            if (sett.mode == Constants.WORK_MODE.UNLOCK) {
                needsImmediateUpdate = false;
                stopListening();
            } else {
                needsImmediateUpdate = false;
            }
            return;
        }

        processSmoothedLux();
    }

    public void prepareForScreenOn() {
        needsImmediateUpdate = true;
        lastAppliedLux = -1f;

        buffer.clear();
        rollingSum = 0f;

        startListening();
    }

    public void startListening() {

        boolean shouldActivate = switch (sett.mode) {
            case ALWAYS, UNLOCK -> true;
            case LANDSCAPE -> landscape || needsImmediateUpdate;
            case PORTRAIT -> !landscape || needsImmediateUpdate;
        };

        if (!shouldActivate) {
            stopListening();
        } else {
            delayer.removeCallbacksAndMessages(null);

            if (!onListen && lightSensor != null) {
                if (sett.mode == Constants.WORK_MODE.UNLOCK || needsImmediateUpdate) {
                    lastAppliedLux = -1f;
                    buffer.clear();
                    rollingSum = 0f;
                }
                sMgr.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
                onListen = true;
            }

            scheduleSuspend();
        }
    }

    public void stopListening() {
        if (onListen) {
            sMgr.unregisterListener(this);
            onListen = false;
        }
        delayer.removeCallbacksAndMessages(null);
    }

    public void reconfigure() {
        stopListening();
        sett.load();
        startListening();
    }

    public void setLandscape(boolean land) {
        this.landscape = land;
    }

    public void onScreenUnlock() {
        try {
            Settings.System.putInt(
                    cResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            );
        } catch (Exception ignored) { }

        needsImmediateUpdate = true;
        startListening();
    }

    /**
     * Get last stored lux value used for calculating brightness.
     * @return last lux level
     */
    public int getLastSensorValue() { return (int) lastLux; }

    /**
     * Get current lux value read from sensor
     * @return raw lux level
     */
    public int getRawSensorValue() { return (int) rawLux; }

    /**
     * Read current display brightnes set in system.
     * Can return wrong values if auto brightness is active!
     * @return current display brightness in percent
     */
    public int getDisplayBrightness() {
        int displayBrightness = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, -1);
        return convertFromPWM(displayBrightness);
    }

    //Post token to observers
    public String[] getLiveBrightnessData() {
        return new String[]{
                String.valueOf(getRawSensorValue()),
                String.valueOf(getDisplayBrightness())
        };
    }

    private void processSmoothedLux() {
        if (buffer.isEmpty()) return;

        // First apply: use last sample immediately
        if (lastAppliedLux == -1f) {
            lastLux = buffer.peekLast().value;
            applyAndRecord(lastLux);
            return;
        }

        float averageLux = rollingSum / buffer.size();
        float diff = Math.abs(averageLux - lastAppliedLux);

        // update if diff > 15% of lastAppliedLux OR diff > 5
        if (diff > (lastAppliedLux * HYSTERESIS_THRESHOLD) || diff > 5f) {
            lastLux = averageLux;
            applyAndRecord(lastLux);
        }
    }

    private void applyAndRecord(float luxVal) {
        setBrightness((int) luxVal);
        lastAppliedLux = luxVal;
    }

    private void scheduleSuspend() {
        if (sett.mode == Constants.WORK_MODE.ALWAYS) return;
        if (sett.mode == Constants.WORK_MODE.PORTRAIT && !landscape) return;
        if (sett.mode == Constants.WORK_MODE.LANDSCAPE && landscape) return;

        delayer.removeCallbacksAndMessages(null);
        delayer.postDelayed(this::stopListening, PAUSE);
    }

    private void setBrightness(int luxValue) {
        int brightness;

        if (luxValue <= sett.l1) brightness = sett.b1;
        else if (luxValue >= sett.l4) brightness = sett.b4;
        else {
            float x1, y1, x2, y2;

            if (luxValue <= sett.l2) { x1 = sett.l1; x2 = sett.l2; y1 = sett.b1; y2 = sett.b2; }
            else if (luxValue <= sett.l3) { x1 = sett.l2; x2 = sett.l3; y1 = sett.b2; y2 = sett.b3; }
            else { x1 = sett.l3; x2 = sett.l4; y1 = sett.b3; y2 = sett.b4; }

            double lx = Math.log10((double) luxValue + 1.0);
            double lx1 = Math.log10((double) x1 + 1.0);
            double lx2 = Math.log10((double) x2 + 1.0);

            double t = (lx2 - lx1 == 0) ? 0 : (lx - lx1) / (lx2 - lx1);
            t = Math.max(0.0, Math.min(1.0, t));

            brightness = (int) Math.round(y1 + (y2 - y1) * t);
        }

        try {
            Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, convertToPWM(brightness));
        } catch (Exception ignored) { }

        if (MainActivity.debugEnabled) {
            Log.d(TAG, String.format("Updating: %s:%s", luxValue, brightness));
        }
    }

    private static class SensorReading {
        final long time;
        final float value;

        SensorReading(long time, float value) {
            this.time = time;
            this.value = value;
        }
    }

    /**
     * Convert brightness from percent to pwm values
     * @param userSetBrightness display brightness 0-100
     * @return brightness converted to pwm values
     */
    private int convertToPWM(int userSetBrightness) {
        if (userSetBrightness < 1) {
            userSetBrightness = 1;
        }
        if (userSetBrightness > 100) {
            userSetBrightness = 100;
        }
        return Math.round((float)255/100*userSetBrightness);
    }

    /**
     * Convert display pwm values for brightness to percent
     * @param displayBrightness pwm value 0-255
     * @return brightness in percent
     */
    private int convertFromPWM(int displayBrightness) {
        if (displayBrightness < 1) {
            displayBrightness = 1;
        }
        if (displayBrightness > 255) {
            displayBrightness = 255;
        }
        return Math.round((float)100/255*displayBrightness);
    }

    private void sendBroadcastToService(String[] sensorData) {
        Intent i = new Intent(Constants.SERVICE_INTENT_SENSOR);
        i.setPackage(lightService.getPackageName());
        i.putExtra(Constants.SERVICE_INTENT_SENSOR, sensorData);
        lightService.sendBroadcast(i);
    }
}
