package com.mine.autolight;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

public class MainActivity extends Activity {
    private MySettings mySettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mySettings = new MySettings(this);

        // Map the buttons to the logic
        // If your XML IDs are different, change "run", "stop", and "apply" below
        setupButton("run", v -> startServiceWithPermission());
        setupButton("stop", v -> {
            stopService(new Intent(this, LightService.class));
            Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show();
        });
        setupButton("apply", v -> {
            mySettings.save(); // This updates the static Map the algorithm uses
            startServiceWithPermission(); // Restarts/Updates the service
            Toast.makeText(this, "Settings Applied", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupButton(String idName, android.view.View.OnClickListener listener) {
        int id = getResources().getIdentifier(idName, "id", getPackageName());
        if (id != 0 && findViewById(id) != null) {
            findViewById(id).setOnClickListener(listener);
        }
    }

    private void startServiceWithPermission() {
        if (Settings.System.canWrite(this)) {
            Intent intent = new Intent(this, LightService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        } else {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }
}
