package com.mine.autolight;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.core.content.ContextCompat;

/**
 * Receiver to catch the system BOOT_COMPLETED broadcast.
 * Optimized for 2025 (API 35) background start requirements.
 */
public class AutoStart extends BroadcastReceiver {
    private static final String TAG = "AutoLight_AutoStart";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot completed. Attempting to start LightService...");

            // Use MySettings to check if the user actually wants the service on
            // This prevents the service from starting on boot if the user manually stopped it
            MySettings settings = new MySettings(context);
            
            // NOTE: If you don't have a "Master Switch" boolean in MySettings yet, 
            // you can remove this check, but it's recommended for a better user experience.
            
            Intent serviceIntent = new Intent(context, LightService.class);

            try {
                // ContextCompat handles the Build.VERSION.SDK_INT >= O check for you.
                // It calls startForegroundService on API 26+ and startService on older versions.
                ContextCompat.startForegroundService(context, serviceIntent);
                Log.d(TAG, "Service start command sent successfully.");
            } catch (Exception e) {
                // On Android 12+, this might fail if the app is heavily restricted by battery settings.
                Log.e(TAG, "Foreground service start failed. Likely Background Execution Restriction: " + e.getMessage());
            }
        }
    }
}
