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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;

import com.gmail.walles.johan.headsetharry.handlers.CalendarPresenter;
import com.gmail.walles.johan.headsetharry.handlers.SmsPresenter;

import org.jetbrains.annotations.NonNls;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import timber.log.Timber;

/**
 * Checkbox tunnelling requests to the system notification access settings.
 */
public class PermissionsPreference
    extends CheckBoxPreference
    implements SharedPreferences.OnSharedPreferenceChangeListener
{
    @NonNls
    private static final List<String> ALLOWED_KEYS;
    static {
        ALLOWED_KEYS = new LinkedList<>();
        ALLOWED_KEYS.add(SmsPresenter.class.getSimpleName());
        ALLOWED_KEYS.add(CalendarPresenter.class.getSimpleName());
    }

    /**
     * The permissions required by this preference.
     */
    private final String[] permissions;

    private final Activity activity;

    public PermissionsPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        activity = (Activity)context;

        if (ALLOWED_KEYS.indexOf(getKey().intern()) == -1) {
            throw new IllegalArgumentException("Key must be listed in PermissionsPreference.ALLOWED_KEYS");
        }

        String permissionsAttribute =
            attributeSet.getAttributeValue("http://schemas.android.com/apk/lib/com.gmail.walles.johan.headsetharry", "permissions");
        if (permissionsAttribute == null) {
            throw new IllegalArgumentException(
                "Must be set but wasn't: 'permissions' of namespace 'http://schemas.android.com/apk/lib/com.gmail.walles.johan.headsetharry'");
        }
        permissions = parsePermissions(permissionsAttribute);
        if (permissions.length == 0) {
            throw new IllegalArgumentException(
                "Must contain at least one permission but didn't: 'permissions' of namespace 'http://schemas.android.com/apk/lib/com.gmail.walles.johan.headsetharry'");
        }

        // Preferences change listener registered in {@link #onAttachedToActivity}
    }

    public static void onRequestPermissionsResult(
        Context context, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                // We didn't get all we wanted
                Timber.i("Permission not granted: %s", permissions[i]);
                return;
            }
        }

        String key = ALLOWED_KEYS.get(requestCode);
        Timber.i("Enabling preference: %s", key);
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(key, true).apply();
    }

    @NonNull
    static String[] parsePermissions(String permissionsString) {
        List<String> permissions = new LinkedList<>();
        Collections.addAll(permissions, permissionsString.split(", *"));
        return permissions.toArray(new String[permissions.size()]);
    }

    @Override
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();

        // We can't do this in the constructor because getSharedPreferences() returns null there
        getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    private boolean hasPermissions() {
        for (String permission: permissions) {
            int status = ContextCompat.checkSelfPermission(activity, permission);
            if (status != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }

    @Override
    protected void onClick() {
        if (isChecked()) {
            super.onClick();
            return;
        }
        if (hasPermissions()) {
            super.onClick();
            return;
        }


        ActivityCompat.requestPermissions(activity, permissions, ALLOWED_KEYS.indexOf(getKey().intern()));
        Timber.i("Permissions requested");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (!getKey().equals(key)) {
            // Not for us, never mind
            return;
        }

        boolean newValue = sharedPreferences.getBoolean(key, false);
        setChecked(newValue);
        Timber.i("Preference checkbox updated: %s=%b", key, newValue);
    }
}
