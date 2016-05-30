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

import com.crashlytics.android.answers.CustomEvent;
import com.gmail.walles.johan.headsetharry.handlers.CalendarPresenter;
import com.gmail.walles.johan.headsetharry.handlers.EmailPresenter;
import com.gmail.walles.johan.headsetharry.handlers.MmsPresenter;
import com.gmail.walles.johan.headsetharry.handlers.Presenter;
import com.gmail.walles.johan.headsetharry.handlers.SmsPresenter;
import com.gmail.walles.johan.headsetharry.handlers.WifiPresenter;
import com.google.common.base.Optional;

import org.jetbrains.annotations.NonNls;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import timber.log.Timber;

public class SpeakerService extends Service {
    @NonNls
    public static final String SPEAK_ACTION = "com.gmail.walles.johan.headsetharry.speak_action";

    private final List<TimestampedAnnouncement> announcementQueue = new LinkedList<>();

    private final Set<Presenter> presenters;

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

    public SpeakerService() {
        super();

        Set<Presenter> presenters = new HashSet<>();
        presenters.add(new CalendarPresenter(this));
        presenters.add(new EmailPresenter(this));
        presenters.add(new MmsPresenter(this));
        presenters.add(new SmsPresenter(this));
        presenters.add(new WifiPresenter(this));

        this.presenters = Collections.unmodifiableSet(presenters);
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
            return START_NOT_STICKY;
        }

        Optional<Presenter> presenter = getPresenter(intent);
        if (!presenter.isPresent()) {
            return START_NOT_STICKY;
        }

        Optional<List<TextWithLocale>> announcement;
        try {
            announcement = presenter.get().getAnnouncement(intent);
        } catch (IllegalArgumentException e) {
            Timber.w(e, "Failed to parse incoming intent");
            return START_NOT_STICKY;
        }
        if (!announcement.isPresent()) {
            return START_NOT_STICKY;
        }
        if (announcement.get().isEmpty()) {
            return START_NOT_STICKY;
        }

        enqueue(announcement.get(), presenter.get().getClass().getSimpleName());

        return START_NOT_STICKY;
    }

    private void enqueue(List<TextWithLocale> announcement, String presenterName) {
        if (!announcementQueue.isEmpty()) {
            long oldestAnnouncementAgeMs = System.currentTimeMillis() - announcementQueue.get(0).timestamp;
            if (oldestAnnouncementAgeMs > 30_000) {
                @NonNls String message = "Oldest enqueued announcement is " + oldestAnnouncementAgeMs + "ms old";
                Timber.w(new Exception(message), "%s", message);
            }
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
        for (Presenter candidate: presenters) {
            if (!Presenter.hasType(intent, candidate.getClass())) {
                continue;
            }

            if (!candidate.isEnabled()) {
                // Candidate is of the right type, but it's not enabled
                return Optional.absent();
            }

            // Candidate is enabled and of the right type
            return Optional.of(candidate); //NOPMD
        }

        Timber.w("No handler for intent %s", intent);
        return Optional.absent();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Binding unsupported, please use startService() instead");
    }
}
