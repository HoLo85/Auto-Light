package com.mine.autolight;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputFilter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private Button btnStart;
    private TextView txtCurAmbience, txtCurDisplay;

    private EditText etSensor1, etSensor2, etSensor3, etSensor4;
    private EditText etBrightness1, etBrightness2, etBrightness3, etBrightness4;

    private MySettings sett;

    private boolean isDialogShown;
    public static boolean debugEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sett = new MySettings(this);

        btnStart = findViewById(R.id.btn_start_stop);
        txtCurAmbience = findViewById(R.id.txt_current_ambient);
        txtCurDisplay = findViewById(R.id.txt_current_display);

        btnStart.setOnClickListener(v -> {
            if (isServiceRunning()) {
                setServiceEnabledPref(false);
                killService();
                displayServiceStatus(Constants.SERVICE_STATUS_STOPPED);
            } else {
                setServiceEnabledPref(true);
                runService();
                displayServiceStatus(Constants.SERVICE_STATUS_RUNNING);
            }
        });

        // init input fields for ambient light values, limit allowed input to 1-25000
        etSensor1 = findViewById(R.id.et_sensor_value_1);
        etSensor1.setFilters(new InputFilter[]{new InputFilterMinMax(1, 25000)});
        etSensor2 = findViewById(R.id.et_sensor_value_2);
        etSensor2.setFilters(new InputFilter[]{new InputFilterMinMax(1, 25000)});
        etSensor3 = findViewById(R.id.et_sensor_value_3);
        etSensor3.setFilters(new InputFilter[]{new InputFilterMinMax(1, 25000)});
        etSensor4 = findViewById(R.id.et_sensor_value_4);
        etSensor4.setFilters(new InputFilter[]{new InputFilterMinMax(1, 25000)});

        // init input fields for display brightness, limit allowed input values to 0-100%
        etBrightness1 = findViewById(R.id.et_brightness_value_1);
        etBrightness1.setFilters(new InputFilter[]{new InputFilterMinMax(1, 100)});
        etBrightness2 = findViewById(R.id.et_brightness_value_2);
        etBrightness2.setFilters(new InputFilter[]{new InputFilterMinMax(1, 100)});
        etBrightness3 = findViewById(R.id.et_brightness_value_3);
        etBrightness3.setFilters(new InputFilter[]{new InputFilterMinMax(1, 100)});
        etBrightness4 = findViewById(R.id.et_brightness_value_4);
        etBrightness4.setFilters(new InputFilter[]{new InputFilterMinMax(1, 100)});

        refillUserSettings();

        Button btnSave = findViewById(R.id.btn_save_settings);
        btnSave.setOnClickListener(v -> {
            try {
                sett.l1 = Integer.parseInt(etSensor1.getText().toString());
                sett.l2 = Integer.parseInt(etSensor2.getText().toString());
                sett.l3 = Integer.parseInt(etSensor3.getText().toString());
                sett.l4 = Integer.parseInt(etSensor4.getText().toString());

                sett.b1 = Integer.parseInt(etBrightness1.getText().toString());
                sett.b2 = Integer.parseInt(etBrightness2.getText().toString());
                sett.b3 = Integer.parseInt(etBrightness3.getText().toString());
                sett.b4 = Integer.parseInt(etBrightness4.getText().toString());

                sett.save();
                sendBroadcastToService(Constants.SERVICE_INTENT_PAYLOAD_SET);

                // hide keyboard on save
                if (this.getCurrentFocus() != null) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
                }

                Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
            }
        });

        final Switch swWAlways = findViewById(R.id.sw_work_0);
        final Switch swWPortrait = findViewById(R.id.sw_work_1);
        final Switch swWLandscape = findViewById(R.id.sw_work_2);
        final Switch swWUnlock = findViewById(R.id.sw_work_3);

        switch (sett.mode) {
            case UNLOCK -> {
                swWAlways.setActivated(true);
                swWAlways.setChecked(true);
            }
            case PORTRAIT -> {
                swWPortrait.setActivated(true);
                swWPortrait.setChecked(true);
            }
            case LANDSCAPE -> {
                swWLandscape.setActivated(true);
                swWLandscape.setChecked(true);
            }
            case ALWAYS -> {
                swWUnlock.setActivated(true);
                swWUnlock.setChecked(true);
            }
            default -> {
                // no default for now
            }
        }

        setupOnCheckChangeListener(new Switch[]{swWAlways, swWPortrait, swWLandscape, swWUnlock});
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            openAppSettings();
            return true;
        }
        if (item.getItemId() == R.id.action_debug) {
            debugEnabled = !item.isChecked();
            item.setChecked(debugEnabled);
            sendBroadcastToService(Constants.SERVICE_INTENT_DEBUG_SET);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (checkAndRequestPermissions()) {
            if (!isServiceRunning() && getServiceEnabledPref()) {
                runService();
            }
            displayServiceStatus(isServiceRunning() ? Constants.SERVICE_STATUS_RUNNING : Constants.SERVICE_STATUS_STOPPED);
        }

        // register receiver for messages from lightService
        registerReceiver(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        registerReceiver(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        registerReceiver(false);
    }

    // setup listeners and states for switches
    // if activated, deactivate other switches
    // if already enabled keep active state of current switch
    private void setupOnCheckChangeListener(final Switch[] switches) {
        for (int sw = 0; sw < switches.length; sw++) {
            final int enabledSwitch = sw;
            switches[enabledSwitch].setOnCheckedChangeListener((compoundButton, b) -> {
                if (!b && switches[enabledSwitch].isActivated()) {
                    switches[enabledSwitch].setChecked(true);
                }

                if (b) {
                    sett.mode = Constants.WORK_MODE.values()[enabledSwitch];

                    switches[enabledSwitch].setActivated(true);

                    for (int disabledSwitch = 0; disabledSwitch < switches.length; disabledSwitch++) {
                        if (disabledSwitch != enabledSwitch) {
                            switches[disabledSwitch].setActivated(false);
                            switches[disabledSwitch].setChecked(false);
                        }
                    }

                    sett.save();
                    sendBroadcastToService(Constants.SERVICE_INTENT_PAYLOAD_SET);
                }
            });
        }
    }

    @SuppressWarnings("unused")
    private void requestNotificationPermission() {
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 123);
        }
    }

    private void setServiceEnabledPref(boolean enabled) {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(Constants.PREF_ENABLED_KEY, enabled).apply();
    }

    private boolean getServiceEnabledPref() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(Constants.PREF_ENABLED_KEY, true);
    }

    private void runService() {
        Intent serviceIntent = new Intent(this, LightService.class);
        startForegroundService(serviceIntent);
    }

    private void killService() {
        stopService(new Intent(this, LightService.class));
    }

    private boolean isServiceRunning() {
        return LightService.isRunning;
    }

    private void registerReceiver(boolean register) {
        try {
            if (register) {
                IntentFilter light = new IntentFilter();
                light.addAction(Constants.SERVICE_INTENT_SENSOR);
                light.addAction(Constants.SERVICE_INTENT_STATUS);
                registerReceiver(sensorBroadcastReceiver, light, Context.RECEIVER_NOT_EXPORTED);
            } else {
                unregisterReceiver(sensorBroadcastReceiver);
            }
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Problem while de-/registering auto light receiver, receiver may have been already de-/registered.");
        }
    }

    private void sendBroadcastToService(String payload) {
        Intent i = new Intent(Constants.SERVICE_INTENT_ACTION);
        i.setPackage(getPackageName());
        i.putExtra(Constants.SERVICE_INTENT_EXTRA, payload);
        sendBroadcast(i);
    }

    // receive updates from light service
    private final BroadcastReceiver sensorBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constants.SERVICE_INTENT_SENSOR.equals(intent.getAction())) {
                final String[] payload = intent.getStringArrayExtra(Constants.SERVICE_INTENT_SENSOR);

                if (payload != null && payload.length > 0) {
                    refreshCurrentLevels(payload);
                }
            }
            if (Constants.SERVICE_INTENT_STATUS.equals(intent.getAction())) {
                final int payload = intent.getIntExtra(Constants.SERVICE_INTENT_STATUS, Constants.SERVICE_STATUS_UNKNOWN);
                refreshServiceStatus(payload);
            }
        }
    };

    private boolean checkAndRequestPermissions() {
        if (Settings.System.canWrite(this)) {
            return true;
        } else {
            if (isDialogShown) return false;

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.permission_request);
            builder.setPositiveButton(R.string.settings, (dialog, id) -> {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 0);
                isDialogShown = false;
            });
            builder.setCancelable(false).show();

            isDialogShown = true;
            return false;
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void displayServiceStatus(int status) {
        switch (status) {
            case 0 -> {
                btnStart.setText(R.string.service_stopped);
                btnStart.setTextColor(getResources().getColor(android.R.color.holo_red_dark, null));
                txtCurAmbience.setText("---");
                txtCurDisplay.setText("---");
            }
            case 1 -> {
                btnStart.setText(R.string.service_started);
                btnStart.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
            }
            default -> btnStart.setText(R.string.starting_service);
        }
    }

    private void refillUserSettings() {
        etSensor1.setText(String.valueOf(sett.l1));
        etSensor2.setText(String.valueOf(sett.l2));
        etSensor3.setText(String.valueOf(sett.l3));
        etSensor4.setText(String.valueOf(sett.l4));

        etBrightness1.setText(String.valueOf(sett.b1));
        etBrightness2.setText(String.valueOf(sett.b2));
        etBrightness3.setText(String.valueOf(sett.b3));
        etBrightness4.setText(String.valueOf(sett.b4));
    }

    /**
     * Refresh view for current ambient light sensor and current display brightness setting.
     * The brightness is read directly from the system.
     * If auto brightness is active this setting may be incorrect!
     */
    private void refreshCurrentLevels(final String[] brightnessData) {
        if (LightService.isRunning && LightService.lightControl != null) {
            txtCurAmbience.setText(brightnessData[0]);
            txtCurDisplay.setText(brightnessData[1]);
            if (debugEnabled) {
                Log.d(TAG, String.format("Refresh: %s:%s", brightnessData[0], brightnessData[1]));
            }
        }
    }

    private void refreshServiceStatus(final int status) {
        displayServiceStatus(status);
        if (debugEnabled) {
            Log.d(TAG, String.format("Status: %s", status));
        }
    }
}