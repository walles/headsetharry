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

package com.gmail.walles.johan.headsetharry.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.CheckBoxPreference;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;

import org.jetbrains.annotations.NonNls;

/**
 * Checkbox tunnelling requests to the system notification access settings.
 */
public class NotificationsPreference
    extends CheckBoxPreference
    implements SharedPreferences.OnSharedPreferenceChangeListener
{
    /**
     * The value of this key gets twiddled by the NotificationListener.
     *
     * @see com.gmail.walles.johan.headsetharry.handlers.NotificationListener#onBind(Intent)
     * @see com.gmail.walles.johan.headsetharry.handlers.NotificationListener#onUnbind(Intent)
     */
    @NonNls
    public final static String RECEIVING_NOTIFICATIONS_PREF = "receivingNotifications";

    // This should really be part of the SDK
    @NonNls
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS =
        "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    public NotificationsPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        // Just ignore the in-XML key, this is a system global state and there should be exactly one
        // key for it.
        setKey(RECEIVING_NOTIFICATIONS_PREF);

        // Preferences change listener registered in {@link #onAttachedToActivity}
    }

    @Override
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();

        // We can't do this in the constructor because getSharedPreferences() returns null there
        getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @SuppressWarnings("HardCodedStringLiteral")
    @Override
    protected void onClick() {
        new AlertDialog.Builder(getContext()).
            setTitle("Need Notifications Access").
            setMessage(String.format(
                "Please enable Headset Harry in the following screen.%n" +
                "%n" +
                "E-mail notifications are done by reading system notifications from e-mail apps, " +
                "and to do that we need to be able to read system notifications at all. That's " +
                "what the next screen is about.")).
            setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS);
                    getContext().startActivity(intent);
                }
            }).show();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (!RECEIVING_NOTIFICATIONS_PREF.equals(key)) {
            // Not for us, never mind
            return;
        }

        boolean newValue = sharedPreferences.getBoolean(key, false);
        setChecked(newValue);
    }
}
