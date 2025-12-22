package com.mine.autolight;

import android.content.ContentResolver;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.provider.Settings;

public class LightControl implements SensorEventListener {
    private final SensorManager sMgr;
    private final Sensor lightSensor;
    private final MySettings sett;
    private final ContentResolver cResolver;
    private final Context mContext;

    private final Handler delayer = new Handler();
    private final long pause = 2000;

    private boolean onListen = false;
    private boolean landscape = false;

    private float lux = 0;
    private int tempBrightness = 0;

    LightControl(Context context) {
        mContext = context;
        sett = new MySettings(context);
        cResolver = context.getContentResolver();
        sMgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sMgr.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // Not used for light sensor
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            lux = event.values[0];
            setBrightness((int) lux);
        }
    }

    private void scheduleSuspend() {
        if (sett.mode == Constants.WORK_MODE_ALWAYS)
            return;

        if (sett.mode == Constants.WORK_MODE_LANDSCAPE && landscape)
            return;

        if (sett.mode == Constants.WORK_MODE_PORTRAIT && !landscape)
            return;

        // If not in a "keep alive" mode, schedule a stop after 'pause' milliseconds
        delayer.postDelayed(new Runner(), pause);
    }

    private class Runner implements Runnable {
        public void run() {
            // Phone state checking removed for privacy and IzzyOnDroid compliance
            stopListening();
        }
    }

    public void startListening() {
        if (!onListen && lightSensor != null) {
            sMgr.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
            onListen = true;
        }
        scheduleSuspend();
    }

    public void stopListening() {
        if (onListen) {
            sMgr.unregisterListener(this);
        }
        delayer.removeCallbacksAndMessages(null);
        onListen = false;
    }

    /**
     * Interpolates brightness based on lux using log10 domain for better human perception.
     */
    private void setBrightness(int lux) {
        int brightness;
        if (lux <= sett.l1) {
            brightness = sett.b1;
        } else if (lux >= sett.l4) {
            brightness = sett.b4;
        } else {
            float x1, y1, x2, y2;
            if (lux <= sett.l2) {
                x1 = sett.l1; x2 = sett.l2;
                y1 = sett.b1; y2 = sett.b2;
            } else if (lux <= sett.l3) {
                x1 = sett.l2; x2 = sett.l3;
                y1 = sett.b2; y2 = sett.b3;
            } else {
                x1 = sett.l3; x2 = sett.l4;
                y1 = sett.b3; y2 = sett.b4;
            }

            double lx = Math.log10((double) lux + 1.0);
            double lx1 = Math.log10((double) x1 + 1.0);
            double lx2 = Math.log10((double) x2 + 1.0);

            double t = 0.0;
            if (Double.compare(lx2, lx1) != 0) {
                t = (lx - lx1) / (lx2 - lx1);
                if (t < 0.0) t = 0.0;
                if (t > 1.0) t = 1.0;
            }

            brightness = (int) Math.round(y1 + (y2 - y1) * t);
        }

        tempBrightness = brightness;

        try {
            int currentBrightness = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, 0);
            if (currentBrightness != brightness) {
                Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
            }
        } catch (Exception ex) {
            // Fails silently if WRITE_SETTINGS permission isn't granted yet
        }
    }

    public void reconfigure() {
        stopListening();
        sett.load();
        startListening();
    }

    public void setLandscape(boolean land) {
        landscape = land;
    }

    public void onScreenUnlock() {
        // Ensure manual mode is set so our app can control brightness
        try {
            Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        } catch (Exception e) {
            // Fallback for restricted environments
        }
        startListening();
    }

    public int getLastSensorValue() {
        return (int) lux;
    }

    public int getSetBrightness() {
        return tempBrightness;
    }
}
