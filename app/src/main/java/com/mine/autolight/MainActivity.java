package com.mine.autolight;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();
    }

    private void checkPermissions() {
        // Special permission required to modify system brightness on Android 6.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 1000);
                Toast.makeText(this, "Please allow permission to modify settings", Toast.LENGTH_LONG).show();
            } else {
                startAutoLightService();
            }
        } else {
            startAutoLightService();
        }
    }

    private void startAutoLightService() {
        Intent intent = new Intent(this, LightService.class);
        startService(intent);
        Toast.makeText(this, "Auto-Light Service Started", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1000) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.System.canWrite(this)) {
                    startAutoLightService();
                }
            }
        }
    }
}