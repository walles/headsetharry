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
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.gmail.walles.johan.headsetharry.ContactDeveloperUtil;
import com.gmail.walles.johan.headsetharry.LogViewerActivity;
import com.gmail.walles.johan.headsetharry.LoggingUtils;
import com.gmail.walles.johan.headsetharry.R;

import org.jetbrains.annotations.NonNls;

import timber.log.Timber;

public class SettingsActivity extends AppCompatActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LoggingUtils.setUpLogging(this);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        setContentView(R.layout.settings);

        // Log preference changes
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        // Set up Support callback
        MenuItem support = menu.findItem(R.id.support);
        support.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                final Context context = SettingsActivity.this;
                int stringId = context.getApplicationInfo().labelRes;
                String applicationLabel = context.getString(stringId);

                @NonNls String versionName = "<Unknown>";
                try {
                    versionName = context.getPackageManager()
                        .getPackageInfo(context.getPackageName(), 0).versionName;
                } catch (PackageManager.NameNotFoundException e) {
                    Timber.w(e, "Getting version name failed");
                }

                new AlertDialog.Builder(context)
                    .setTitle(applicationLabel)
                    .setMessage("Version " + versionName)
                    .setNeutralButton(R.string.view_app_logs,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                final Intent viewLogsIntent =
                                    new Intent(context, LogViewerActivity.class);
                                context.startActivity(viewLogsIntent);
                            }
                        })
                    .setPositiveButton(R.string.contact_developer_condensed,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ContactDeveloperUtil.sendMail(context);
                            }
                        })
                    .setIcon(R.drawable.ic_launcher)
                    .show();

                return true;
            }
        });

        return true;
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        Timber.i("Settings activity notified about updated permissions, code=%d", requestCode);
        PermissionsPreference.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Object value = sharedPreferences.getAll().get(key);
        Timber.d("Preference <%s> set to <%s>", key, value);
    }
}
