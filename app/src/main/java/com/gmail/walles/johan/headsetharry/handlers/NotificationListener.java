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

import android.app.Notification;
import android.content.Intent;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;

import com.crashlytics.android.answers.CustomEvent;
import com.gmail.walles.johan.headsetharry.LoggingUtils;
import com.gmail.walles.johan.headsetharry.settings.NotificationsPreference;

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

        logIncomingNotification(sbn);

        if (!EmailPresenter.speak(this, sbn)) {
            Timber.d("No handler for %s notification", sbn.getPackageName());
        }
    }

    private void logIncomingNotification(StatusBarNotification sbn) {
        Timber.i("Incoming notification from %s with extras <%s>",
            sbn.getPackageName(), sbn.getNotification().extras);
        CharSequence[] textLines = sbn.getNotification().extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (textLines != null) {
            for (CharSequence textLine: textLines) {
                Timber.i("  Text line <%s>: <%s>", textLine.getClass(), textLine);
                if (textLine instanceof SpannableString) {
                    SpannableString spannable = (SpannableString)textLine;
                    Object[] spans = spannable.getSpans(0, spannable.length() - 1, Object.class);
                    for (Object span: spans) {
                        int from = spannable.getSpanStart(span);
                        int to = spannable.getSpanEnd(span);
                        int flags = spannable.getSpanFlags(span);

                        int style = 0;
                        if (span instanceof TextAppearanceSpan) {
                            style = ((TextAppearanceSpan)span).getTextStyle();
                        }

                        Timber.i("    Span %d-%d, flags=%d, style=%d: %s", from, to, flags, style, span);
                    }
                }
            }
        }
        CharSequence[] people = sbn.getNotification().extras.getCharSequenceArray(Notification.EXTRA_PEOPLE);
        if (people != null) {
            for (CharSequence person: people) {
                Timber.i("  Person: <%s>", person);
            }
        }

        LoggingUtils.logCustom(
            new CustomEvent("Status Bar Notification"). //NON-NLS
                putCustomAttribute("package", sbn.getPackageName())); //NON-NLS
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // This method intentionally left blank
    }

    @Override
    public IBinder onBind(Intent intent) {
        Timber.i("Getting Notification notifications");
        IBinder result = super.onBind(intent);
        PreferenceManager.getDefaultSharedPreferences(this).
            edit().
            putBoolean(NotificationsPreference.RECEIVING_NOTIFICATIONS_PREF, true).
            apply();
        return result;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Timber.w("Not getting Notification notifications any more");
        boolean result = super.onUnbind(intent);
        PreferenceManager.getDefaultSharedPreferences(this).
            edit().
            putBoolean(NotificationsPreference.RECEIVING_NOTIFICATIONS_PREF, false).
            apply();
        return result;
    }
}
