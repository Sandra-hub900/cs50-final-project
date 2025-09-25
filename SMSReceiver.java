package com.example.womensafetyapp2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class SMSReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus != null) {
                for (Object pdu : pdus) {
                    SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu, "3gpp");
                    String sender = smsMessage.getOriginatingAddress();
                    String messageBody = smsMessage.getMessageBody();

                    Log.d("SMSReceiver", "SMS received from: " + sender + " | Message: " + messageBody);

                    // Check for a specific keyword in the message to trigger the alert
                    if (messageBody.contains("EMERGENCY ALERT")) {
                        // Start the EmergencyResponseActivity for siren and flashing
                        Intent flashIntent = new Intent(context, EmergencyResponseActivity.class);
                        flashIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(flashIntent);
                    }
                }
            }
        }
    }
}
