package com.mine.autolight;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputFilter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    private Button btnStart;

    private EditText etSensor1, etSensor2, etSensor3, etSensor4;
    private EditText etBrightness1, etBrightness2, etBrightness3, etBrightness4;

    private MySettings sett;

    private boolean isDialogShown = false;

    private Timer refreshTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sett = new MySettings(this);

        btnStart = findViewById(R.id.btn_start_stop);

        btnStart.setOnClickListener(v -> {
            if (isServiceRunning()) {
                setServiceEnabledPref(false);
                killService();
                displayServiceStatus(0);
            } else {
                setServiceEnabledPref(true);
                runService();
                displayServiceStatus(-1);
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

        Switch swWAlways = findViewById(R.id.sw_work_0);
        Switch swWPortrait = findViewById(R.id.sw_work_1);
        Switch swWLandscape = findViewById(R.id.sw_work_2);
        Switch swWUnlock = findViewById(R.id.sw_work_3);

        switch (sett.mode) {
            case Constants.WORK_MODE_ALWAYS -> {
                swWAlways.setActivated(true);
                swWAlways.setChecked(true);
            }
            case Constants.WORK_MODE_PORTRAIT -> {
                swWAlways.setActivated(true);
                swWPortrait.setChecked(true);
            }
            case Constants.WORK_MODE_LANDSCAPE -> {
                swWAlways.setActivated(true);
                swWLandscape.setChecked(true);
            }
            default -> {
                swWAlways.setActivated(true);
                swWUnlock.setChecked(true);
            }
        }

        swWAlways.setOnCheckedChangeListener((compoundButton, b) -> {
            if (!b && swWAlways.isActivated()) {
                swWAlways.setChecked(true);
            }

            if (b && !swWAlways.isActivated()) {
                sett.mode = Constants.WORK_MODE_ALWAYS;

                swWAlways.setActivated(true);
                swWPortrait.setActivated(false); swWPortrait.setChecked(false);
                swWLandscape.setActivated(false); swWLandscape.setChecked(false);
                swWUnlock.setActivated(false); swWUnlock.setChecked(false);

                sett.save();
                sendBroadcastToService(Constants.SERVICE_INTENT_PAYLOAD_SET);
            }
        });

        swWPortrait.setOnCheckedChangeListener((compoundButton, b) -> {
            if (!b && swWPortrait.isActivated()) {
                swWPortrait.setChecked(true);
            }

            if (b && !swWPortrait.isActivated()) {
                sett.mode = Constants.WORK_MODE_PORTRAIT;

                swWPortrait.setActivated(true);
                swWAlways.setActivated(false); swWAlways.setChecked(false);
                swWLandscape.setActivated(false); swWLandscape.setChecked(false);
                swWUnlock.setActivated(false); swWUnlock.setChecked(false);

                sett.save();
                sendBroadcastToService(Constants.SERVICE_INTENT_PAYLOAD_SET);
            }
        });

        swWLandscape.setOnCheckedChangeListener((compoundButton, b) -> {
            if (!b && swWLandscape.isActivated()) {
                swWLandscape.setChecked(true);
            }

            if (b) {
                sett.mode = Constants.WORK_MODE_LANDSCAPE;

                swWLandscape.setActivated(true);
                swWPortrait.setActivated(false); swWPortrait.setChecked(false);
                swWAlways.setActivated(false); swWAlways.setChecked(false);
                swWUnlock.setActivated(false); swWUnlock.setChecked(false);

                sett.save();
                sendBroadcastToService(Constants.SERVICE_INTENT_PAYLOAD_SET);
            }
        });

        swWUnlock.setOnCheckedChangeListener((compoundButton, b) -> {
            if (!b && swWUnlock.isActivated()) {
                swWUnlock.setChecked(true);
            }

            if (b) {
                sett.mode = Constants.WORK_MODE_UNLOCK;

                swWUnlock.setActivated(true);
                swWPortrait.setActivated(false); swWPortrait.setChecked(false);
                swWLandscape.setActivated(false); swWLandscape.setChecked(false);
                swWAlways.setActivated(false); swWAlways.setChecked(false);

                sett.save();
                sendBroadcastToService(Constants.SERVICE_INTENT_PAYLOAD_SET);
            }
        });
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
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (checkAndRequestPermissions()) {
            if (!isServiceRunning() && getServiceEnabledPref()) {
                runService();
            }
            displayServiceStatus(isServiceRunning() ? 1 : 0);
        }

        refreshTimer = new Timer();
        refreshBrightnessStatus();
    }

    @Override
    public void onPause() {
        super.onPause();
        refreshTimer.cancel();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        refreshTimer.purge();
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

    private void sendBroadcastToService(int payload) {
        Intent i = new Intent(Constants.SERVICE_INTENT_ACTION);
        i.setPackage(getPackageName());
        i.putExtra(Constants.SERVICE_INTENT_EXTRA, payload);
        sendBroadcast(i);
    }

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
    private void refreshBrightnessStatus() {
        refreshTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (LightService.isRunning && LightService.lightControl != null) {
                        final String[] brightnessData = LightService.lightControl.getLiveBrightnessData();

                        TextView txtCurAmbience = findViewById(R.id.txt_current_ambient);
                        TextView txtCurDisplay = findViewById(R.id.txt_current_display);

                        txtCurAmbience.setText(brightnessData[0]);
                        txtCurDisplay.setText(brightnessData[1]);
                        displayServiceStatus(1);
                    }
                }
            }, 0, 500);
    }
}