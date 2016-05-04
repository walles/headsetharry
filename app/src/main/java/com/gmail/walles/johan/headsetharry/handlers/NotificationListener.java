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

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import com.gmail.walles.johan.headsetharry.LoggingUtils;

import timber.log.Timber;

public class NotificationListener extends NotificationListenerService {
    @Override
    public void onCreate() {
        LoggingUtils.setUpLogging(this);
        super.onCreate();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn.isOngoing()) {
            // Just read transient notifications
            return;
        }

        CharSequence tickerText = sbn.getNotification().tickerText;
        if (tickerText == null || tickerText.length() == 0) {
            Timber.i("Ignoring tickerText-less notification from %s", sbn.getPackageName());
            return;
        }

        NotificationPresenter.speak(this, tickerText);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // This method intentionally left blank
    }
}
