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
import android.net.Uri;
import android.provider.CalendarContract;

import com.gmail.walles.johan.headsetharry.LoggingUtils;

import timber.log.Timber;

public class CalendarReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        LoggingUtils.setUpLogging(context);

        Timber.d("Got calendar intent: %s", intent);
        Timber.d("Calendar intent extras: %s", intent.getExtras());
        for (String key: intent.getExtras().keySet()) {
            Timber.d("   Calendar intent extras key: %s", key);
        }

        if (!intent.getAction().equals(CalendarContract.ACTION_EVENT_REMINDER)) {
            throw new IllegalArgumentException(intent.getAction());
        }

        Uri uri = intent.getData();
        String alarmTime = uri.getLastPathSegment();
        CalendarPresenter.speak(context, Long.parseLong(alarmTime));
    }
}
