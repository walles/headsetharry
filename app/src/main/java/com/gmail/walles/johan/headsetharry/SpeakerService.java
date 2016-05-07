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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.gmail.walles.johan.headsetharry.handlers.EmailPresenter;
import com.gmail.walles.johan.headsetharry.handlers.MmsPresenter;
import com.gmail.walles.johan.headsetharry.handlers.SmsPresenter;
import com.gmail.walles.johan.headsetharry.handlers.WifiPresenter;
import com.google.common.base.Optional;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.TestOnly;

import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class SpeakerService extends Service {
    @NonNls
    public static final String SPEAK_ACTION = "com.gmail.walles.johan.headsetharry.speak_action";

    @NonNls
    public static final String EXTRA_TYPE = "com.gmail.walles.johan.headsetharry.type";

    private long isDuplicateTimeoutMs = 10000;
    private List<TextWithLocale> duplicateBase;
    private long duplicateBaseTimestamp;

    @Override
    public void onCreate() {
        super.onCreate();
        LoggingUtils.setUpLogging(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!SPEAK_ACTION.equals(intent.getAction())) {
            Timber.w("Ignoring unsupported action <%s>", intent.getAction());
            return Service.START_NOT_STICKY;
        }

        Optional<List<TextWithLocale>> announcement = toAnnouncement(intent);
        if (!announcement.isPresent()) {
            return Service.START_NOT_STICKY;
        }

        AudioUtils.speakOverHeadset(this, announcement.get(), new TtsUtils.CompletionListener() {
            @Override
            public void onSuccess() {
                // This method intentionally left blank
            }

            @Override
            public void onFailure(@Nullable Locale locale, @NonNls String errorMessage) {
                Timber.e(new Exception(errorMessage), "%s", errorMessage);
            }
        });

        return START_NOT_STICKY;
    }

    boolean isDuplicate(List<TextWithLocale> announcement) {
        if (!announcement.equals(duplicateBase)) {
            // Not the same message, start over
            duplicateBase = announcement;
            duplicateBaseTimestamp = System.currentTimeMillis();
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - duplicateBaseTimestamp > isDuplicateTimeoutMs) {
            // This is the same message, but our duplicate has timed out, start over
            duplicateBase = announcement;
            duplicateBaseTimestamp = System.currentTimeMillis();
            return false;
        }

        return true;
    }

    @TestOnly
    void setIsDuplicateTimeoutMs(long timeoutMillis) {
        if (timeoutMillis < 0) {
            throw new IllegalArgumentException("Timeout must be >= 0, was " + timeoutMillis);
        }
        isDuplicateTimeoutMs = timeoutMillis;
    }

    private Optional<List<TextWithLocale>> toAnnouncement(Intent intent) {
        String type = intent.getStringExtra(EXTRA_TYPE);
        if (TextUtils.isEmpty(type)) {
            Timber.e("Speak action with no type");
            return Optional.absent();
        }

        try {
            if (SmsPresenter.TYPE.equals(type)) {
                return Optional.of(new SmsPresenter(this, intent).getAnnouncement());
            } else if (MmsPresenter.TYPE.equals(type)) {
                return Optional.of(new MmsPresenter(this, intent).getAnnouncement());
            } else if (WifiPresenter.TYPE.equals(type)) {
                List<TextWithLocale> announcement = new WifiPresenter(this, intent).getAnnouncement();
                if (isDuplicate(announcement)) {
                    Timber.w("Ignoring duplicate Wifi announcement <%s>", announcement);
                    return Optional.absent();
                }
                return Optional.of(announcement);
            } else if (EmailPresenter.TYPE.equals(type)) {
                return Optional.of(new EmailPresenter(this, intent).getAnnouncement());
            } else {
                Timber.w("Ignoring incoming intent of type %s", type);
                return Optional.absent();
            }
        } catch (IllegalArgumentException e) {
            Timber.e(e, "Error parsing intent: %s", intent);
            return Optional.absent();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Binding unsupported, please use startService() instead");
    }
}
