package com.mine.autolight;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.os.Build;

public class AutoStart extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent pushIntent = new Intent(context, LightService.class);
            
            // Android 8.0 (API 26) and above requires startForegroundService
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(pushIntent);
            } else {
                context.startService(pushIntent);
            }
        }
    }
}
