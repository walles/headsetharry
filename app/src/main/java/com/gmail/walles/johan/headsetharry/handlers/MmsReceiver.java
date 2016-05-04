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

import com.gmail.walles.johan.headsetharry.LoggingUtils;

import org.jetbrains.annotations.NonNls;

import java.nio.charset.Charset;

import timber.log.Timber;

/**
 * Receives incoming MMSes.
 */
public class MmsReceiver extends BroadcastReceiver {
    @NonNls
    private static final String ACTION_MMS_RECEIVED = "android.provider.Telephony.WAP_PUSH_RECEIVED";
    @NonNls
    private static final String MMS_DATA_TYPE = "application/vnd.wap.mms-message";

    @Override
    public void onReceive(Context context, Intent intent) {
        LoggingUtils.setUpLogging(context);

        if (!ACTION_MMS_RECEIVED.equals(intent.getAction())) {
            Timber.w("Got broadcast with unknown MMS action: <%s>", intent.getAction());
            return;
        }

        if (!MMS_DATA_TYPE.equals(intent.getType())) {
            Timber.w("Got broadcast with unknown MMS type: <%s>", intent.getType());
            return;
        }

        @NonNls Bundle bundle = intent.getExtras();
        if (bundle == null) {
            Timber.w("Got null bundle with MMS intent");
            return;
        }

        byte[] buffer = bundle.getByteArray("data");
        if (buffer == null) {
            Timber.w("Got null data byte array with MMS intent");
            return;
        }

        // FIXME: That the charset of incoming numbers is the default Android encoding (UTF-8) is
        // really just a guess
        @NonNls String incomingNumber = new String(buffer, Charset.defaultCharset());
        Timber.d("Incoming MMS number, raw: <%s>", incomingNumber);

        // From: http://stackoverflow.com/q/14452808/473672
        // Tested on a real phone and found working.
        // FIXME: Get magic constant "15" from somewhere that's not Stack Overflow
        int index = incomingNumber.indexOf("/TYPE");
        if(index > 0 && (index - 15) > 0){
            int newIndex = index - 15;
            incomingNumber = incomingNumber.substring(newIndex, index);
            index = incomingNumber.indexOf('+');
            if(index > 0){
                incomingNumber = incomingNumber.substring(index);
            }
        }
        Timber.d("Incoming MMS number, cooked: <%s>", incomingNumber);

        MmsPresenter.speak(context, incomingNumber);
    }
}
