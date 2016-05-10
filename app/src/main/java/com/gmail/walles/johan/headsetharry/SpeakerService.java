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

import com.gmail.walles.johan.headsetharry.handlers.CalendarPresenter;
import com.gmail.walles.johan.headsetharry.handlers.EmailPresenter;
import com.gmail.walles.johan.headsetharry.handlers.MmsPresenter;
import com.gmail.walles.johan.headsetharry.handlers.SmsPresenter;
import com.gmail.walles.johan.headsetharry.handlers.WifiPresenter;
import com.google.common.base.Optional;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.TestOnly;

import java.util.LinkedList;
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

    private final List<TimestampedAnnouncement> announcementQueue = new LinkedList<>();

    private static class TimestampedAnnouncement {
        public final long timestamp;
        public final List<TextWithLocale> announcement;

        public TimestampedAnnouncement(List<TextWithLocale> announcement) {
            this.timestamp = System.currentTimeMillis();
            this.announcement = announcement;
        }
    }

    /**
     * 0 means not speaking.
     */
    private long speechStartTimestamp = 0;

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

        enqueue(announcement.get());

        return START_NOT_STICKY;
    }

    private void enqueue(List<TextWithLocale> announcement) {
        if (!announcementQueue.isEmpty()) {
            long oldestAnnouncementAgeMs = System.currentTimeMillis() - announcementQueue.get(0).timestamp;
            @NonNls String message = "Oldest enqueued announcement is " + oldestAnnouncementAgeMs + "ms old";
            Timber.w(new Exception(message), "%s", message);
        }
        announcementQueue.add(new TimestampedAnnouncement(announcement));
        dequeue();
    }

    private void dequeue() {
        if (isSpeaking()) {
            return;
        }
        if (announcementQueue.isEmpty()) {
            return;
        }

        List<TextWithLocale> announcement = announcementQueue.remove(0).announcement;
        boolean speechStarted = AudioUtils.speakOverHeadset(this, announcement, new TtsUtils.CompletionListener() {
            @Override
            public void onSuccess() {
                speechStartTimestamp = 0;
                dequeue();
            }

            @Override
            public void onFailure(@Nullable Locale locale, @NonNls String errorMessage) {
                speechStartTimestamp = 0;
                Timber.e(new Exception(errorMessage), "%s", errorMessage);
                dequeue();
            }
        });
        if (speechStarted) {
            speechStartTimestamp = System.currentTimeMillis();
        }
    }

    private boolean isSpeaking() {
        if (speechStartTimestamp == 0) {
            return false;
        }
        long speechDurationMs = System.currentTimeMillis() - speechStartTimestamp;
        if (speechDurationMs > 60000) {
            @NonNls String message = "Still speaking after " + speechDurationMs + "ms, pretending we're done";
            Timber.w(new Exception(message), "%s", message);
            return false;
        }
        return true;
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
                List<TextWithLocale> announcement = new WifiPresenter(this).getAnnouncement();
                if (isDuplicate(announcement)) {
                    Timber.w("Ignoring duplicate Wifi announcement <%s>", announcement);
                    return Optional.absent();
                }
                return Optional.of(announcement);
            } else if (EmailPresenter.TYPE.equals(type)) {
                return Optional.of(new EmailPresenter(this, intent).getAnnouncement());
            } else if (CalendarPresenter.TYPE.equals(type)) {
                List<TextWithLocale> announcement = new CalendarPresenter(this, intent).getAnnouncement();
                if (announcement.isEmpty()) {
                    // There are two known causes for this:
                    // 1. We were notified about a declined event
                    // 2. When the user edits an event on the device, we get fake event notifications
                    //   that we can only ignore
                    return Optional.absent();
                }
                return Optional.of(announcement);
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
