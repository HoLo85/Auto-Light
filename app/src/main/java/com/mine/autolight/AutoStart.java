package com.mine.autolight;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

public class AutoStart extends BroadcastReceiver {
    private static final String TAG = "AutoStartReceiver";
    
    private static final String PREF_ENABLED = "service_enabled_by_user";
    private static final String PREFS_NAME = "AutoLightPrefs";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            boolean isEnabled = prefs.getBoolean(PREF_ENABLED, true); // Default to true for first-time use

            if (!isEnabled) {
                Log.d(TAG, "Boot detected, but service is disabled by user. Doing nothing.");
                return;
            }

            Log.d(TAG, "Boot completed detected and service is enabled. Starting LightService...");

            Intent serviceIntent = new Intent(context, LightService.class);

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to start service on boot: " + e.getMessage());
            }
        }
    }
}
