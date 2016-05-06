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
import android.support.annotation.Nullable;

import com.gmail.walles.johan.headsetharry.LookupUtils;
import com.gmail.walles.johan.headsetharry.R;
import com.gmail.walles.johan.headsetharry.SpeakerService;
import com.gmail.walles.johan.headsetharry.TextWithLocale;
import com.gmail.walles.johan.headsetharry.Translations;

import org.jetbrains.annotations.NonNls;

import java.util.List;
import java.util.Locale;

public class MmsPresenter extends Presenter {
    @NonNls
    private static final String EXTRA_SENDER = "com.gmail.walles.johan.headsetharry.sender";

    @NonNls
    public static final String TYPE = "MMS";

    private final List<TextWithLocale> announcement;

    public static void speak(Context context, CharSequence sender) {
        Intent intent = new Intent(context, SpeakerService.class);
        intent.setAction(SpeakerService.SPEAK_ACTION);
        intent.putExtra(SpeakerService.EXTRA_TYPE, TYPE);
        intent.putExtra(EXTRA_SENDER, sender);
        context.startService(intent);
    }

    public MmsPresenter(Context context, Intent intent) {
        super(context);

        // It's OK for the sender to be null, we'll just say it's unknown
        CharSequence sender = intent.getCharSequenceExtra(EXTRA_SENDER);

        announcement = createAnnouncement(sender);
    }

    @NonNull
    @Override
    public List<TextWithLocale> getAnnouncement() {
        return announcement;
    }

    private List<TextWithLocale> createAnnouncement(@Nullable CharSequence sender) {
        sender =
            LookupUtils.getNameForNumber(context, sender)
                .or(context.getString(R.string.unknown_sender));

        Translations translations = new Translations(context, Locale.getDefault(), R.string.mms_from_x);
        return translations.format(R.string.mms_from_x, sender);
    }
}
