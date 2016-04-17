package com.gmail.walles.johan.headsetharry;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import org.jetbrains.annotations.NonNls;

import timber.log.Timber;

/**
 * Receive incoming SMSes.
 */
public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        @NonNls final Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }

        final Object[] pduObjs = (Object[]) bundle.get("pdus");
        if (pduObjs == null) {
            Timber.w("Got SMS with null PDUs");
            return;
        }
        if (pduObjs.length == 0) {
            Timber.w("Got SMS with no PDUs");
            return;
        }

        for (Object pduObj : pduObjs) {
            SmsMessage message = SmsMessage.createFromPdu((byte[])pduObj);

            SpeakerService.speakSms(context,
                message.getDisplayMessageBody(), message.getOriginatingAddress());
        }
    }
}
