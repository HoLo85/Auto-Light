package com.mine.autolight;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.core.content.ContextCompat; // Ensure this is in your build.gradle

public class MainActivity extends Activity {

    private Button btnStart;
    private TextView tvState;
    private EditText etSensor1, etSensor2, etSensor3, etSensor4;
    private EditText etBrightness1, etBrightness2, etBrightness3, etBrightness4;
    private MySettings sett;
    private boolean isExpanded = false;
    private boolean isDialogShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sett = new MySettings(this);

        // --- Collapsible Logic ---
        Button btnExpand = findViewById(R.id.btn_expand);
        LinearLayout llHidden = findViewById(R.id.ll_hidden_settings);
        llHidden.setVisibility(View.GONE);
        
        btnExpand.setOnClickListener(v -> {
            isExpanded = !isExpanded;
            llHidden.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            btnExpand.setText(isExpanded ? R.string.hide_config : R.string.show_config);
            if (isExpanded) {
                refillCollapsibleSettings();
                requestNotificationPermission(); // Ask when they want to configure
            }
        });

        // --- Start/Stop Logic ---
        tvState = findViewById(R.id.tv_service_state);
        btnStart = findViewById(R.id.btn_start_stop);
        
        btnStart.setOnClickListener(v -> {
            // UPDATED: Using the static boolean from LightService (much more reliable)
            if (LightService.isRunning) { 
                killService();
                displayServiceStatus(0);
            } else {
                if (requestNotificationPermission()) {
                    runService();
                    displayServiceStatus(-1);
                }
            }
        });

        // --- Ping State Logic ---
        findViewById(R.id.btn_get_state).setOnClickListener(v -> {
            displayServiceStatus(LightService.isRunning ? 1 : 0);
            sendBroadcastToService(Constants.SERVICE_INTENT_PAYLOAD_PING);
        });

        // --- Initialize Settings Views ---
        initSettingsViews();
        refillCollapsibleSettings();

        // --- Save Button ---
        findViewById(R.id.btn_save_settings).setOnClickListener(v -> saveCurrentSettings());

        // --- RadioGroup Logic ---
        RadioGroup rgWorkMode = findViewById(R.id.rg_work_mode);
        syncWorkModeUI();
        rgWorkMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_work_always) sett.mode = Constants.WORK_MODE_ALWAYS;
            else if (checkedId == R.id.rb_work_portrait) sett.mode = Constants.WORK_MODE_PORTRAIT;
            else if (checkedId == R.id.rb_work_landscape) sett.mode = Constants.WORK_MODE_LANDSCAPE;
            else if (checkedId == R.id.rb_work_unlock) sett.mode = Constants.WORK_MODE_UNLOCK;
            sett.save();
            sendBroadcastToService(Constants.SERVICE_INTENT_PAYLOAD_SET);
        });

        TextView tvHelp = findViewById(R.id.tv_dontkillmyapp);
        if (tvHelp != null) tvHelp.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void runService() {
        Intent i = new Intent(this, LightService.class);
        // UPDATED: ContextCompat is the 2025 standard for starting foreground services
        ContextCompat.startForegroundService(this, i);
    }

    private void killService() {
        stopService(new Intent(this, LightService.class));
    }

    private boolean requestNotificationPermission() {
        // Android 13 (Tiramisu) and above requires explicit permission for FGS notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 123);
                return false;
            }
        }
        return true;
    }

    private void displayServiceStatus(int status) {
        int color;
        String text;
        if (status == 1 || (status == -1 && LightService.isRunning)) {
            btnStart.setText(R.string.stop);
            color = android.R.color.holo_green_dark;
            text = getString(R.string.service_running);
        } else if (status == -1) {
            color = android.R.color.darker_gray;
            text = getString(R.string.starting_service);
        } else {
            btnStart.setText(R.string.start);
            color = android.R.color.holo_red_dark;
            text = getString(R.string.service_stopped);
        }
        tvState.setTextColor(getResources().getColor(color, null));
        tvState.setText(text);
    }

    // ... (Keep your syncWorkModeUI, checkAndRequestPermissions, and init methods)
    
    private void sendBroadcastToService(int payload) {
        Intent i = new Intent(Constants.SERVICE_INTENT_ACTION);
        i.putExtra(Constants.SERVICE_INTENT_EXTRA, payload);
        i.setPackage(getPackageName()); 
        sendBroadcast(i);
    }
}
