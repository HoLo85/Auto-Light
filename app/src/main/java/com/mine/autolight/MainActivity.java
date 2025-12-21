package com.mine.autolight;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private MySettings mySettings;
    private EditText etL1, etL2, etL3, etL4, etB1, etB2, etB3, etB4;
    private RadioGroup rgWorkMode;
    private TextView tvServiceState;
    private Button btnStartStop;

    // Receiver to catch data from the background process
    private final BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int lux = intent.getIntExtra("lux", 0);
            int bri = intent.getIntExtra("bri", 0);
            tvServiceState.setText("Sensor: " + lux + " lx | Target: " + bri);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mySettings = new MySettings(this);
        initViews();
        loadUISettings();

        btnStartStop.setOnClickListener(v -> toggleService());

        // SAVE BUTTON: Saves and tells Service to reload
        findViewById(R.id.btn_save_settings).setOnClickListener(v -> {
            saveUISettings();
            if (isServiceRunning()) {
                Intent intent = new Intent(this, LightService.class);
                intent.putExtra("command", "reload");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
                else startService(intent);
            }
            Toast.makeText(this, "Settings Applied", Toast.LENGTH_SHORT).show();
        });

        // REQUEST BUTTON: Battery Optimization
        findViewById(R.id.btn_request).setOnClickListener(v -> {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Already Exempted", Toast.LENGTH_SHORT).show();
            }
        });

        // GET DATA: Updates UI manually (The receiver also does this live)
        findViewById(R.id.btn_get_state).setOnClickListener(v -> updateUIState());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUIState();
        registerReceiver(dataReceiver, new IntentFilter("COM_MINE_AUTOLIGHT_UPDATE"), Context.RECEIVER_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(dataReceiver);
    }

    private void initViews() {
        tvServiceState = findViewById(R.id.tv_service_state);
        btnStartStop = findViewById(R.id.btn_start_stop);
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

    private void updateUIState() {
        if (isServiceRunning()) {
            tvServiceState.setText("Service: RUNNING");
            btnStartStop.setText("STOP");
        } else {
            tvServiceState.setText("Service: STOPPED");
            btnStartStop.setText("START");
        }
    }

    private void toggleService() {
        if (isServiceRunning()) {
            stopService(new Intent(this, LightService.class));
        } else {
            startAutoLightService();
        }
        tvServiceState.postDelayed(this::updateUIState, 300);
    }

    private void startAutoLightService() {
        if (!Settings.System.canWrite(this)) {
            startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName())));
            return;
        }
        Intent intent = new Intent(this, LightService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
        else startService(intent);
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LightService.class.getName().equals(service.service.getClassName())) return true;
        }
        return false;
    }

    private void loadUISettings() {
        mySettings.load();
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

        int id = rgWorkMode.getCheckedRadioButtonId();
        if (id == R.id.rb_work_always) mySettings.mode = Constants.WORK_MODE_ALWAYS;
        else if (id == R.id.rb_work_portrait) mySettings.mode = Constants.WORK_MODE_PORTRAIT;
        else if (id == R.id.rb_work_landscape) mySettings.mode = Constants.WORK_MODE_LANDSCAPE;
        else if (id == R.id.rb_work_unlock) mySettings.mode = Constants.WORK_MODE_UNLOCK;
        mySettings.save();
    }
}
