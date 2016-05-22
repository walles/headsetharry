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
import android.content.ComponentCallbacks2;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.crashlytics.android.answers.CustomEvent;
import com.gmail.walles.johan.headsetharry.handlers.CalendarPresenter;
import com.gmail.walles.johan.headsetharry.handlers.EmailPresenter;
import com.gmail.walles.johan.headsetharry.handlers.MmsPresenter;
import com.gmail.walles.johan.headsetharry.handlers.Presenter;
import com.gmail.walles.johan.headsetharry.handlers.SmsPresenter;
import com.gmail.walles.johan.headsetharry.handlers.WifiPresenter;
import com.google.common.base.Optional;

import org.jetbrains.annotations.NonNls;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class SpeakerService extends Service {
    @NonNls
    public static final String SPEAK_ACTION = "com.gmail.walles.johan.headsetharry.speak_action";

    @NonNls
    public static final String EXTRA_TYPE = "com.gmail.walles.johan.headsetharry.type";

    private final List<TimestampedAnnouncement> announcementQueue = new LinkedList<>();

    private final WifiPresenter.State wifiState = new WifiPresenter.State();

    private static class TimestampedAnnouncement {
        public final long timestamp;
        public final List<TextWithLocale> announcement;
        public final String presenterName;

        public TimestampedAnnouncement(List<TextWithLocale> announcement, String presenterName) {
            this.timestamp = System.currentTimeMillis();
            this.announcement = announcement;
            this.presenterName = presenterName;
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
        Timber.d("SpeakerService started");
    }

    @Override
    public void onDestroy() {
        Timber.d("SpeakerService stopping");
        super.onDestroy();
    }

    @Override
    public void onTrimMemory(int level) {
        Timber.d("SpeakerService notified of low memory, level %d/%d",
            level, ComponentCallbacks2.TRIM_MEMORY_COMPLETE);
        super.onTrimMemory(level);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!SPEAK_ACTION.equals(intent.getAction())) {
            Timber.w("Ignoring unsupported action <%s>", intent.getAction());
            return Service.START_NOT_STICKY;
        }

        Optional<Presenter> presenter = getPresenter(intent);
        if (!presenter.isPresent()) {
            return Service.START_NOT_STICKY;
        }

        Optional<List<TextWithLocale>> announcement = presenter.get().getAnnouncement();
        if (!announcement.isPresent()) {
            return Service.START_NOT_STICKY;
        }
        if (announcement.get().isEmpty()) {
            return Service.START_NOT_STICKY;
        }

        enqueue(announcement.get(), presenter.get().getClass().getSimpleName());

        return START_NOT_STICKY;
    }

    private void enqueue(List<TextWithLocale> announcement, String presenterName) {
        if (!announcementQueue.isEmpty()) {
            long oldestAnnouncementAgeMs = System.currentTimeMillis() - announcementQueue.get(0).timestamp;
            @NonNls String message = "Oldest enqueued announcement is " + oldestAnnouncementAgeMs + "ms old";
            Timber.w(new Exception(message), "%s", message);
        }
        announcementQueue.add(new TimestampedAnnouncement(announcement, presenterName));
        dequeue();
    }

    private void dequeue() {
        if (isSpeaking()) {
            return;
        }
        if (announcementQueue.isEmpty()) {
            return;
        }

        final TimestampedAnnouncement entry = announcementQueue.remove(0);
        boolean speechStarted = AudioUtils.speakOverHeadset(this, entry.announcement, new TtsUtils.CompletionListener() {
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

            LoggingUtils.logCustom(
                new CustomEvent("Announcement Sent to TTS"). //NON-NLS
                    putCustomAttribute("Presenter", entry.presenterName)); //NON-NLS
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

    private Optional<Presenter> getPresenter(Intent intent) {
        String type = intent.getStringExtra(EXTRA_TYPE);
        if (TextUtils.isEmpty(type)) {
            Timber.e("Speak action with no type");
            return Optional.absent();
        }

        try {
            Presenter presenter;
            if (SmsPresenter.TYPE.equals(type)) {
                presenter = new SmsPresenter(this, intent);
            } else if (MmsPresenter.TYPE.equals(type)) {
                presenter = new MmsPresenter(this, intent);
            } else if (WifiPresenter.TYPE.equals(type)) {
                presenter = new WifiPresenter(this, wifiState);
            } else if (EmailPresenter.TYPE.equals(type)) {
                presenter = new EmailPresenter(this, intent);
            } else if (CalendarPresenter.TYPE.equals(type)) {
                presenter = new CalendarPresenter(this, intent);
            } else {
                Timber.w("Ignoring incoming intent of type %s", type);
                return Optional.absent();
            }

            return Optional.of(presenter);
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
