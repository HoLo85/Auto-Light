package com.mine.autolight;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
    private MySettings mySettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mySettings = new MySettings(this);

        // START BUTTON
        findViewById(R.id.btnStart).setOnClickListener(v -> {
            if (Settings.System.canWrite(this)) {
                startForegroundService(new Intent(this, LightService.class));
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        });

        // STOP BUTTON
        findViewById(R.id.btnStop).setOnClickListener(v -> {
            stopService(new Intent(this, LightService.class));
        });
        
        // SAVE/APPLY BUTTON (Example)
        findViewById(R.id.btnSave).setOnClickListener(v -> {
            // Update mySettings variables from UI here...
            mySettings.save();
            // Refresh service
            startForegroundService(new Intent(this, LightService.class));
        });
    }
}
