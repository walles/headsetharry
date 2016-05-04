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
import android.support.annotation.NonNull;

import com.gmail.walles.johan.headsetharry.R;
import com.gmail.walles.johan.headsetharry.SpeakerService;
import com.gmail.walles.johan.headsetharry.TextWithLocale;
import com.gmail.walles.johan.headsetharry.Translations;
import com.google.common.base.Optional;

import org.jetbrains.annotations.NonNls;

import java.util.List;
import java.util.Locale;

public class NotificationPresenter extends Presenter {
    @NonNls
    public static final String TYPE = "Notification";

    @NonNls
    private static final String EXTRA_TICKER_TEXT = "com.gmail.walles.johan.headsetharry.tickertext";

    private final List<TextWithLocale> announcement;

    public static void speak(Context context, CharSequence tickerText) {
        if (tickerText == null) {
            // Never mind
            return;
        }

        if (tickerText.length() == 0) {
            // Never mind
            return;
        }

        Intent intent = new Intent(context, SpeakerService.class);
        intent.setAction(SpeakerService.SPEAK_ACTION);
        intent.putExtra(SpeakerService.EXTRA_TYPE, TYPE);
        intent.putExtra(EXTRA_TICKER_TEXT, tickerText);
        context.startService(intent);
    }

    @NonNull
    @Override
    public List<TextWithLocale> getAnnouncement() {
        return announcement;
    }

    public NotificationPresenter(Context context, Intent intent) {
        super(context);

        CharSequence tickerText = intent.getCharSequenceExtra(EXTRA_TICKER_TEXT);
        if (tickerText == null) {
            throw new IllegalArgumentException("Got notification with null ticker text");
        }
        if (tickerText.length() == 0) {
            throw new IllegalArgumentException("Got notification with empty ticker text");
        }

        announcement = createAnnouncement(tickerText);
    }

    private List<TextWithLocale> createAnnouncement(CharSequence tickerText) {
        Optional<Locale> tickerTextLocale = identifyLanguage(tickerText);
        Translations translations = new Translations(context, tickerTextLocale.or(Locale.getDefault()),
            R.string.system_notification);
        return translations.format(R.string.system_notification,
            tickerTextLocale.or(Locale.getDefault()), tickerText);
    }
}
