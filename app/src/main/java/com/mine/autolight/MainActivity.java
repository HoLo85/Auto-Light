package com.mine.autolight;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class MainActivity extends Activity {
    private MySettings mySettings;
    private EditText etL1, etL2, etL3, etL4, etB1, etB2, etB3, etB4;
    private RadioGroup rgWorkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mySettings = new MySettings(this);
        initViews();
        loadUISettings();

        // 1. START / STOP BUTTON
        findViewById(R.id.btn_start_stop).setOnClickListener(v -> toggleService());

        // 2. SAVE BUTTON
        findViewById(R.id.btn_save_settings).setOnClickListener(v -> {
            saveUISettings();
            Toast.makeText(this, "Settings Saved & Applied", Toast.LENGTH_SHORT).show();
            // Restart service to apply new math points immediately
            if (isServiceRunning()) startAutoLightService();
        });
    }

    private void initViews() {
        etL1 = findViewById(R.id.et_sensor_value_1);
        etL2 = findViewById(R.id.et_sensor_value_2);
        etL3 = findViewById(R.id.et_sensor_value_3);
        etL4 = findViewById(R.id.et_sensor_value_4);
        etB1 = findViewById(R.id.et_brightness_value_1);
        etB2 = findViewById(R.id.et_brightness_value_2);
        etB3 = findViewById(R.id.et_brightness_value_3);
        etB4 = findViewById(R.id.et_brightness_value_4);
        rgWorkMode = findViewById(R.id.rg_work_mode);
    }

    private void loadUISettings() {
        etL1.setText(String.valueOf(mySettings.l1));
        etL2.setText(String.valueOf(mySettings.l2));
        etL3.setText(String.valueOf(mySettings.l3));
        etL4.setText(String.valueOf(mySettings.l4));
        etB1.setText(String.valueOf(mySettings.b1));
        etB2.setText(String.valueOf(mySettings.b2));
        etB3.setText(String.valueOf(mySettings.b3));
        etB4.setText(String.valueOf(mySettings.b4));

        if (mySettings.mode == Constants.WORK_MODE_ALWAYS) rgWorkMode.check(R.id.rb_work_always);
        else if (mySettings.mode == Constants.WORK_MODE_PORTRAIT) rgWorkMode.check(R.id.rb_work_portrait);
        else if (mySettings.mode == Constants.WORK_MODE_LANDSCAPE) rgWorkMode.check(R.id.rb_work_landscape);
        else if (mySettings.mode == Constants.WORK_MODE_UNLOCK) rgWorkMode.check(R.id.rb_work_unlock);
    }

    private void saveUISettings() {
        mySettings.l1 = Integer.parseInt(etL1.getText().toString());
        mySettings.l2 = Integer.parseInt(etL2.getText().toString());
        mySettings.l3 = Integer.parseInt(etL3.getText().toString());
        mySettings.l4 = Integer.parseInt(etL4.getText().toString());
        mySettings.b1 = Integer.parseInt(etB1.getText().toString());
        mySettings.b2 = Integer.parseInt(etB2.getText().toString());
        mySettings.b3 = Integer.parseInt(etB3.getText().toString());
        mySettings.b4 = Integer.parseInt(etB4.getText().toString());

        int checkedId = rgWorkMode.getCheckedRadioButtonId();
        if (checkedId == R.id.rb_work_always) mySettings.mode = Constants.WORK_MODE_ALWAYS;
        else if (checkedId == R.id.rb_work_portrait) mySettings.mode = Constants.WORK_MODE_PORTRAIT;
        else if (checkedId == R.id.rb_work_landscape) mySettings.mode = Constants.WORK_MODE_LANDSCAPE;
        else if (checkedId == R.id.rb_work_unlock) mySettings.mode = Constants.WORK_MODE_UNLOCK;

        mySettings.save();
    }

    private void toggleService() {
        if (isServiceRunning()) {
            stopService(new Intent(this, LightService.class));
            Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show();
        } else {
            startAutoLightService();
        }
    }

    private void startAutoLightService() {
        if (Settings.System.canWrite(this)) {
            Intent intent = new Intent(this, LightService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
            else startService(intent);
        } else {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    // Simple helper to check if service is alive (you can also use a static boolean in LightService)
    private boolean isServiceRunning() {
        // For simplicity in this example, we assume toggle logic; 
        // in a real app, you might use ActivityManager or a static flag.
        return true; 
    }
}
