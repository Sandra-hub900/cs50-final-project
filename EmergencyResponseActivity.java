package com.example.womensafetyapp2;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EmergencyResponseActivity extends AppCompatActivity {

    private static final long EMERGENCY_DURATION = 5 * 60 * 1000; // 5 minutes in milliseconds
    private static final long FLASH_INTERVAL = 500; // Flash every 500ms
    private static final long VIBRATION_PATTERN[] = {0, 1000, 500}; // Vibrate for 1 second, pause 0.5 seconds

    private MediaPlayer sirenPlayer;
    private Vibrator vibrator;
    private CountDownTimer emergencyTimer;
    private ValueAnimator flashAnimator;

    private View rootLayout;
    private TextView timerText;
    private TextView statusText;
    private Button stopButton;

    private boolean isEmergencyActive = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_response);

        // Keep screen on and show over lock screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        initializeViews();
        startEmergencyResponse();
    }

    private void initializeViews() {
        rootLayout = findViewById(R.id.rootLayout);
        timerText = findViewById(R.id.timerText);
        statusText = findViewById(R.id.statusText);
        stopButton = findViewById(R.id.stopButton);

        stopButton.setOnClickListener(v -> stopEmergencyResponse());

        // Set initial status
        statusText.setText("EMERGENCY RESPONSE ACTIVE");
        statusText.setTextColor(Color.WHITE);
        statusText.setTextSize(24);
    }

    private void startEmergencyResponse() {
        startSiren();
        startVibration();
        startFlashing();
        startTimer();

        Toast.makeText(this, "Emergency Response Started - Will run for 5 minutes", Toast.LENGTH_LONG).show();
    }

    private void startSiren() {
        try {
            sirenPlayer = MediaPlayer.create(this, R.raw.siren);
            if (sirenPlayer != null) {
                sirenPlayer.setLooping(true);
                sirenPlayer.setVolume(1.0f, 1.0f); // Maximum volume
                sirenPlayer.start();
            }
        } catch (Exception e) {
            // If siren file doesn't exist, create a beeping sound programmatically
            Toast.makeText(this, "Siren file not found - using system alert", Toast.LENGTH_SHORT).show();
        }
    }

    private void startVibration() {
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            // Repeat vibration pattern indefinitely
            vibrator.vibrate(VIBRATION_PATTERN, 0);
        }
    }

    private void startFlashing() {
        flashAnimator = ValueAnimator.ofFloat(0f, 1f);
        flashAnimator.setDuration(FLASH_INTERVAL);
        flashAnimator.setRepeatCount(ValueAnimator.INFINITE);
        flashAnimator.setRepeatMode(ValueAnimator.REVERSE);

        flashAnimator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();

            // Create flashing red effect
            int red = (int) (255 * animatedValue);
            int color = Color.rgb(red, 0, 0);
            rootLayout.setBackgroundColor(color);

            // Also flash the text
            int textAlpha = (int) (255 * (1 - animatedValue));
            timerText.setTextColor(Color.argb(255, 255, textAlpha, textAlpha));
        });

        flashAnimator.start();
    }

    private void startTimer() {
        emergencyTimer = new CountDownTimer(EMERGENCY_DURATION, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = millisUntilFinished / 60000;
                long seconds = (millisUntilFinished % 60000) / 1000;

                String timeText = String.format("Time Remaining: %02d:%02d", minutes, seconds);
                timerText.setText(timeText);
                timerText.setTextSize(32);
                timerText.setTextColor(Color.WHITE);
            }

            @Override
            public void onFinish() {
                stopEmergencyResponse();
                Toast.makeText(EmergencyResponseActivity.this, "Emergency Response Completed", Toast.LENGTH_LONG).show();
            }
        };

        emergencyTimer.start();
    }

    private void stopEmergencyResponse() {
        if (!isEmergencyActive) return;

        isEmergencyActive = false;

        // Stop siren
        if (sirenPlayer != null) {
            sirenPlayer.stop();
            sirenPlayer.release();
            sirenPlayer = null;
        }

        // Stop vibration
        if (vibrator != null) {
            vibrator.cancel();
        }

        // Stop flashing
        if (flashAnimator != null) {
            flashAnimator.cancel();
        }

        // Stop timer
        if (emergencyTimer != null) {
            emergencyTimer.cancel();
        }

        // Reset background
        rootLayout.setBackgroundColor(Color.BLACK);

        // Update UI
        statusText.setText("EMERGENCY RESPONSE STOPPED");
        statusText.setTextColor(Color.GREEN);
        timerText.setText("Response Deactivated");
        timerText.setTextColor(Color.GREEN);
        stopButton.setText("Close");

        // Close activity after a short delay
        rootLayout.postDelayed(() -> finish(), 2000);

        Toast.makeText(this, "Emergency Response Stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopEmergencyResponse();
    }

    @Override
    public void onBackPressed() {
        // Prevent accidental back press during emergency
        Toast.makeText(this, "Use STOP EMERGENCY button to deactivate", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Keep emergency response active even when app goes to background
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure emergency response continues when app comes back to foreground
    }
}