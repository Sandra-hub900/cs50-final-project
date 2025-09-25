package com.example.womensafetyapp2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
                Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()) ||
                Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction())) {

            // Start Guardian Service on boot
            Intent serviceIntent = new Intent(context, GuardianService.class);
            context.startForegroundService(serviceIntent);
        }
    }
}