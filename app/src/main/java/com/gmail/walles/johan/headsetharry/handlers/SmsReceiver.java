/*
 * Copyright 2016 Johan Walles <johan.walles@gmail.com>
 *
 * This file is part of Headset Harry.
 *
 * Headset Harry is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Headset Harry is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Headset Harry.  If not, see <http://www.gnu.org/licenses/>.
 */

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
            //noinspection deprecation
            SmsMessage message = SmsMessage.createFromPdu((byte[])pduObj);

            SmsPresenter.speak(context,
                message.getDisplayMessageBody(), message.getOriginatingAddress());
        }
    }
}
