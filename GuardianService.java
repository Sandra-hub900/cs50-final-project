package com.example.womensafetyapp2;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.telephony.SmsMessage;
import android.widget.Toast;

    public class GuardianService extends Service {
        private BroadcastReceiver smsReceiver;

        @Override
        public void onCreate() {
            super.onCreate();
            createNotificationChannel();
            try {
                startForeground(1, createNotification());
                setupSMSReceiver();
            } catch (SecurityException e) {
                // Handle case where foreground service permissions are not granted
                android.util.Log.e("GuardianService", "Failed to start foreground service: " + e.getMessage());
                stopSelf();
            }
        }

        private void createNotificationChannel() {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                android.app.NotificationChannel channel = new android.app.NotificationChannel(
                        "GUARDIAN_CHANNEL",
                        "Guardian Monitoring",
                        android.app.NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Monitoring for emergency alerts");

                android.app.NotificationManager manager = getSystemService(android.app.NotificationManager.class);
                manager.createNotificationChannel(channel);
            }
        }

        private android.app.Notification createNotification() {
            return new android.app.Notification.Builder(this, "GUARDIAN_CHANNEL")
                    .setContentTitle("🛡️ Guardian Mode Active")
                    .setContentText("Monitoring for emergency alerts")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .build();
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Toast.makeText(this, "🛡️ Guardian monitoring active in background", Toast.LENGTH_SHORT).show();
            return START_STICKY; // Restart if killed
        }

        private void setupSMSReceiver() {
            smsReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if ("android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
                        android.os.Bundle bundle = intent.getExtras();
                        if (bundle != null) {
                            Object[] pdus = (Object[]) bundle.get("pdus");
                            if (pdus != null) {
                                for (Object pdu : pdus) {
                                    SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);
                                    String messageBody = sms.getMessageBody();
                                    String sender = sms.getOriginatingAddress();

                                    // Check if this is an emergency alert
                                    if (isEmergencyAlert(messageBody)) {
                                        // IMMEDIATELY launch full-screen red alert
                                        launchEmergencyAlert(sender, messageBody);
                                    }
                                }
                            }
                        }
                    }
                }
            };

            IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
            filter.setPriority(1000);
            registerReceiver(smsReceiver, filter);
        }

        private boolean isEmergencyAlert(String message) {
            String lowerMessage = message.toLowerCase();
            return lowerMessage.contains("emergency alert") ||
                    lowerMessage.contains("🚨") ||
                    lowerMessage.contains("urgent") ||
                    lowerMessage.contains("danger") ||
                    lowerMessage.contains("women's safety guardian") ||
                    lowerMessage.contains("emergency:");
        }

        private void launchEmergencyAlert(String sender, String message) {
            // Launch FULL-SCREEN red flashing alert immediately
            Intent alertIntent = new Intent(this, EmergencyAlertActivity.class);
            alertIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            alertIntent.putExtra("sender", sender);
            alertIntent.putExtra("message", message);
            startActivity(alertIntent);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (smsReceiver != null) {
                unregisterReceiver(smsReceiver);
            }
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

