package com.gmail.walles.johan.headsetharry;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import java.util.Locale;

import timber.log.Timber;

/**
 * Receive incoming SMSes.
 */
public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }

        final Object[] pduObjs = (Object[]) bundle.get("pdus");
        if (pduObjs == null) {
            Timber.w("Got SMS with no PDUs");
            return;
        }

        for (Object pduObj : pduObjs) {
            SmsMessage message = SmsMessage.createFromPdu((byte[])pduObj);
            Locale locale = getLocale(message);
            CharSequence announcement = toString(message, locale);
            SpeakerService.speak(context, announcement, locale);
        }
    }

    private Locale getLocale(SmsMessage message) {
        // FIXME: Identify locale here
        return Locale.ENGLISH;
    }

    private CharSequence toString(SmsMessage message, Locale locale) {
        // FIXME: Get an SMS message template for the given locale and fill that in.

        // FIXME: What if we don't have an SMS message template for the given locale?

        String source = message.getDisplayOriginatingAddress();
        if (source == null) {
            source = "unknown sender";
        }
        return String.format("SMS from %s: %s", source, message.getDisplayMessageBody());
    }
}
