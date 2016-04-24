package com.gmail.walles.johan.headsetharry.handlers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import com.gmail.walles.johan.headsetharry.LoggingUtil;

import org.jetbrains.annotations.NonNls;

import timber.log.Timber;

/**
 * Receive incoming SMSes.
 */
public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        LoggingUtil.setUpLogging(context);

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

            SmsPresenter.speak(context,
                message.getDisplayMessageBody(), message.getOriginatingAddress());
        }
    }
}
