package com.mine.autolight;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.core.content.ContextCompat;

/**
 * Receiver to catch the system BOOT_COMPLETED and LOCKED_BOOT_COMPLETED broadcasts.
 * This ensures the LightService restarts automatically after a device reboot,
 * even before the user unlocks the device for the first time.
 */
public class AutoStart extends BroadcastReceiver {
    private static final String TAG = "AutoLight_AutoStart";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        
        // Handle both the standard boot and the encrypted "locked" boot state
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || 
            "android.intent.action.LOCKED_BOOT_COMPLETED".equals(action)) {
            
            Log.d(TAG, "Boot signal detected: " + action + ". Attempting to start LightService...");

            // Intent pointing to your Foreground Service
            Intent serviceIntent = new Intent(context, LightService.class);

            try {
                // ContextCompat.startForegroundService is the safest way to start 
                // a foreground service across all Android versions (API 26 to 35).
                ContextCompat.startForegroundService(context, serviceIntent);
                Log.d(TAG, "LightService start command sent successfully.");
            } catch (Exception e) {
                // Log the error. On Android 12+, this can fail if the app 
                // is restricted by the user's battery optimization settings.
                Log.e(TAG, "Failed to start LightService on boot: " + e.getMessage());
            }
        }
    }
}
