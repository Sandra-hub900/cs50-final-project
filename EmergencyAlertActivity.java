// Create new file: EmergencyAlertActivity.java
package com.example.womensafetyapp2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class EmergencyAlertActivity extends Activity {
    private View flashingView;
    private Handler flashHandler;
    private Runnable flashRunnable;
    private MediaPlayer alarmPlayer;
    private boolean isFlashing = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make full screen and show over lock screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_emergency_alert);

        String sender = getIntent().getStringExtra("sender");
        String message = getIntent().getStringExtra("message");

        setupUI(sender, message);
        startRedFlashing();
        startAlarmSound();
    }

    private void setupUI(String sender, String message) {
        flashingView = findViewById(R.id.flashingView);
        TextView alertText = findViewById(R.id.alertText);
        TextView senderText = findViewById(R.id.senderText);
        TextView messageText = findViewById(R.id.messageText);
        Button callButton = findViewById(R.id.callButton);
        Button textButton = findViewById(R.id.textButton);
        Button stopButton = findViewById(R.id.stopButton);

        alertText.setText("🚨 EMERGENCY ALERT 🚨");
        senderText.setText("From: " + sender);
        messageText.setText(message);

        callButton.setOnClickListener(v -> {
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:" + sender));
            startActivity(callIntent);
        });

        textButton.setOnClickListener(v -> {
            Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
            smsIntent.setData(Uri.parse("smsto:" + sender));
            smsIntent.putExtra("sms_body", "I received your emergency alert. Are you safe? Calling you now.");
            startActivity(smsIntent);
        });

        stopButton.setOnClickListener(v -> {
            stopAlert();
            finish();
        });
    }

    private void startRedFlashing() {
        flashHandler = new Handler();
        flashRunnable = new Runnable() {
            boolean isRed = false;

            @Override
            public void run() {
                if (isFlashing) {
                    if (isRed) {
                        flashingView.setBackgroundColor(Color.RED);
                        getWindow().setStatusBarColor(Color.RED);
                        getWindow().setNavigationBarColor(Color.RED);
                    } else {
                        flashingView.setBackgroundColor(Color.WHITE);
                        getWindow().setStatusBarColor(Color.WHITE);
                        getWindow().setNavigationBarColor(Color.WHITE);
                    }
                    isRed = !isRed;
                    flashHandler.postDelayed(this, 500); // Flash every 500ms
                }
            }
        };
        flashHandler.post(flashRunnable);
    }

    private void startAlarmSound() {
        try {
            // Try to use siren sound first
            alarmPlayer = MediaPlayer.create(this, R.raw.siren);
            if (alarmPlayer == null) {
                // Fallback to system alarm sound
                Uri alarmSound = android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI;
                alarmPlayer = MediaPlayer.create(this, alarmSound);
            }

            if (alarmPlayer != null) {
                alarmPlayer.setLooping(true);
                alarmPlayer.start();
            }
        } catch (Exception e) {
            // Last resort - notification sound
            try {
                android.media.RingtoneManager.getRingtone(this,
                        android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)).play();
            } catch (Exception ignored) {}
        }
    }

    private void stopAlert() {
        isFlashing = false;
        if (flashHandler != null && flashRunnable != null) {
            flashHandler.removeCallbacks(flashRunnable);
        }
        if (alarmPlayer != null) {
            alarmPlayer.stop();
            alarmPlayer.release();
            alarmPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlert();
    }

    @Override
    public void onBackPressed() {
        // Prevent accidental dismissal - require stop button
    }
}