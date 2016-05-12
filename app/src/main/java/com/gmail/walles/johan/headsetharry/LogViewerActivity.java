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

package com.gmail.walles.johan.headsetharry;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.TextView;

import org.jetbrains.annotations.NonNls;

import timber.log.Timber;

/**
 * Show collected system logs and offer user to compose an e-mail to the developer.
 */
public class LogViewerActivity extends AppCompatActivity {
    private TextView logView;
    private boolean logsLoaded = false;
    private MenuItem contactDeveloper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LoggingUtils.setUpLogging(this);

        setContentView(R.layout.contact_developer_layout);

        logView = (TextView)findViewById(R.id.logView);
        final ScrollView verticalScrollView =
                (ScrollView)findViewById(R.id.verticalScrollView);
        if (verticalScrollView == null) {
            throw new NullPointerException("Got null verticalScrollView");
        }

        logView.setText("Reading logs, please stand by...");
        new AsyncTask<Void, Void, CharSequence>() {
            @Override
            protected void onPostExecute(CharSequence logText) {
                logView.setText(logText);
                logsLoaded = true;

                // Scroll log view to bottom
                verticalScrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        verticalScrollView.smoothScrollTo(0, Integer.MAX_VALUE);
                    }
                });

                setUpContactDeveloper();
            }

            @Override
            protected CharSequence doInBackground(Void... voids) {
                return LogCollector.readLogs(LogViewerActivity.this);
            }
        }.execute();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            @NonNls String error = "Got a null ActionBar";
            Timber.w(new Exception(error), error);
            return;
        }
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void setUpContactDeveloper() {
        if (contactDeveloper == null) {
            return;
        }

        if (!logsLoaded) {
            contactDeveloper.setEnabled(false);
            return;
        }

        contactDeveloper.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                ContactDeveloperUtil.sendMail(LogViewerActivity.this, logView.getText());

                return true;
            }
        });
        ContactDeveloperUtil.setUpMenuItem(this, contactDeveloper);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.contact_developer, menu);

        contactDeveloper = menu.findItem(R.id.contact_developer);
        setUpContactDeveloper();

        return super.onCreateOptionsMenu(menu);
    }
}
