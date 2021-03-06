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

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gmail.walles.johan.headsetharry.R;
import com.gmail.walles.johan.headsetharry.SpeakerService;
import com.gmail.walles.johan.headsetharry.TextWithLocale;
import com.gmail.walles.johan.headsetharry.Translations;
import com.google.common.base.Optional;

import org.jetbrains.annotations.NonNls;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import timber.log.Timber;

public class CalendarPresenter extends Presenter {
    @NonNls
    private static final String EXTRA_ALARM_TIME = "com.gmail.walles.johan.headsetharry.alarmTime";

    public static void speak(Context context, Date alarmTime) {
        Intent intent = new Intent(context, SpeakerService.class);
        intent.setAction(SpeakerService.SPEAK_ACTION);
        Presenter.setType(intent, CalendarPresenter.class);
        intent.putExtra(EXTRA_ALARM_TIME, alarmTime);
        context.startService(intent);
    }

    private final State state = new State();

    static class State {
        private static class DateAndId {
            @NonNull
            public final Date date;
            public final int id;

            public DateAndId(@NonNull Date date, int id) {
                this.date = date;
                this.id = id;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }

                DateAndId dateAndId = (DateAndId)o;

                if (id != dateAndId.id) {
                    return false;
                }
                if (!date.equals(dateAndId.date)) {
                    return false;
                }

                return true;
            }

            @Override
            public int hashCode() {
                int result = date.hashCode();
                result = 31 * result + id;
                return result;
            }
        }

        private final Set<DateAndId> cache = new HashSet<>();

        public boolean isDuplicate(int eventId, Date alarmTime) {
            trim();

            if (cache.contains(new DateAndId(alarmTime, eventId))) {
                return true;
            }

            cache.add(new DateAndId(alarmTime, eventId));
            return false;
        }

        /**
         * Trim old entries from the cache.
         */
        private void trim() {
            Date now = new Date();
            Iterator<DateAndId> iter = cache.iterator();
            while (iter.hasNext()) {
                DateAndId dateAndId = iter.next();
                long ageMs = now.getTime() - dateAndId.date.getTime();
                if (ageMs > 120_000) {
                    iter.remove();
                }
                if (ageMs < 0) {
                    @NonNls
                    String message = "Cached announcement identifier from the future, ageMs=" + ageMs;
                    Timber.w(new Exception(message), message);
                }
            }
        }
    }

    public CalendarPresenter(Context context) {
        super(context);
    }

    @NonNull
    @Override
    public Optional<List<TextWithLocale>> getAnnouncement(Intent intent) {
        Date alarmTime = (Date)intent.getSerializableExtra(EXTRA_ALARM_TIME);
        if (alarmTime == null) {
            throw new IllegalArgumentException(intent.toString());
        }

        // We're getting announcements at random times for things, so unless this date is inside a
        // minute from or before [now] we should just drop it
        if (Math.abs(alarmTime.getTime() - System.currentTimeMillis()) > 45000) {
            Timber.i("Dropping calendar alarm for %s", alarmTime);
            return Optional.absent();
        }

        List<TextWithLocale> announcement = new LinkedList<>();

        // Find all events IDs that have alarms scheduled at this time
        try (Cursor cursor = context.getContentResolver().query(
            CalendarContract.CalendarAlerts.CONTENT_URI_BY_INSTANCE,
            new String[]{CalendarContract.CalendarAlerts.EVENT_ID},
            CalendarContract.CalendarAlerts.ALARM_TIME + "=?",
            new String[]{Long.toString(alarmTime.getTime())}, null))
        {
            if (cursor == null) {
                throw new NullPointerException("Got null cursor from calendar query <id from alarm time>");
            }

            while (cursor.moveToNext()) {
                int eventId = cursor.getInt(0);
                if (state.isDuplicate(eventId, alarmTime)) {
                    continue;
                }

                List<TextWithLocale> announcementForId = createAnnouncementForEventId(eventId);
                if (announcementForId == null) {
                    continue;
                }

                announcement.addAll(announcementForId);
            }
        }

        if (announcement.isEmpty()) {
            Timber.d("No non-declined events found for alarm time %s", alarmTime);
            return Optional.absent();
        }
        return Optional.of(announcement);
    }

    @Nullable
    private List<TextWithLocale> createAnnouncementForEventId(int calendarEventId) {
        try (Cursor cursor = context.getContentResolver().query(
            CalendarContract.Events.CONTENT_URI,

            new String[]{CalendarContract.Events.TITLE, CalendarContract.Events.DESCRIPTION},

            String.format(Locale.ENGLISH, "%s=? AND %s!=?", //NON-NLS
                CalendarContract.Events._ID, CalendarContract.Events.SELF_ATTENDEE_STATUS),
            new String[]{
                Integer.toString(calendarEventId), Integer.toString(CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED)},

            null))
        {
            if (cursor == null) {
                throw new NullPointerException("Got null cursor from calendar query <event from event ID>");
            }

            if (cursor.getCount() != 1) {
                Timber.w("Didn't get exactly one calendar event for ID %d: %d",
                    calendarEventId, cursor.getCount());
            }

            if (!cursor.moveToNext()) {
                return null;
            }

            String title = cursor.getString(0);
            String description = cursor.getString(1);

            Optional<Locale> eventLocale = identifyLanguage(title);
            if (!eventLocale.isPresent()) {
                eventLocale = identifyLanguage(description);
            }

            Translations translations = new Translations(context, eventLocale.or(Locale.getDefault()),
                R.string.calendar_event_colon_what);
            return translations.format(R.string.calendar_event_colon_what, eventLocale.or(Locale.getDefault()), title);
        } catch (SecurityException e) {
            boolean pref = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(getClass().getSimpleName(), true);

            // Warning because if things work as intended this would require somebody to:
            // 1. Enable calendar announcements
            // 2. Grant us READ_CALENDAR permissions
            // 3. Go to the system settings and remove our READ_CALENDAR permissions
            Timber.w(e, "Calendar access denied with pref=%b, resetting preference", pref);

            PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(getClass().getSimpleName(), false)
                .apply();

            return null;
        }
    }

    @Override
    public boolean isEnabled() {
        return isEnabled(getClass());
    }
}
