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

import java.util.Date;

import timber.log.Timber;

public class CalendarReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        LoggingUtils.setUpLogging(context);

        // Findings: These are always the same:
        // intent.getData().getLastPathSegment()
        // intent.getExtras().get("alarmTime")
        // intent.getExtras().get("android.intent.extra.ALARM_TARGET_TIME")
        //
        // That holds both for actual alarms and the fake ones we get when somebody modifies an
        // event on the device.
        //
        // Regarding intent.getExtras().get("android.intent.extra.ALARM_TARGET_TIME"), I've only
        // seen the value 1 for that, both for fake and for real alarms.
        //   /johan.walles@gmail.com - 2016May09
        Timber.d("Got calendar intent: %s", intent);
        Timber.d("Calendar intent extras: %s", intent.getExtras());
        for (String key: intent.getExtras().keySet()) {
            Timber.d("   Calendar intent extras: %s=<%s>", key, intent.getExtras().get(key));
        }

        if (!intent.getAction().equals(CalendarContract.ACTION_EVENT_REMINDER)) {
            throw new IllegalArgumentException(intent.getAction());
        }

        Uri uri = intent.getData();
        Date alarmTime = new Date(Long.parseLong(uri.getLastPathSegment()));
        Timber.d("Got calendar reminder for %s", alarmTime);
        CalendarPresenter.speak(context, alarmTime);
    }
}
