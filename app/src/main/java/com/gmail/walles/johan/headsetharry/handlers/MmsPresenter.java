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

import com.gmail.walles.johan.headsetharry.LookupUtils;
import com.gmail.walles.johan.headsetharry.R;
import com.gmail.walles.johan.headsetharry.SpeakerService;
import com.gmail.walles.johan.headsetharry.TextWithLocale;
import com.gmail.walles.johan.headsetharry.Translations;
import com.google.common.base.Optional;

import org.jetbrains.annotations.NonNls;

import java.util.List;
import java.util.Locale;

public class MmsPresenter extends Presenter {
    @NonNls
    private static final String EXTRA_SENDER = "com.gmail.walles.johan.headsetharry.sender";

    public static void speak(Context context, CharSequence sender) {
        Intent intent = new Intent(context, SpeakerService.class);
        intent.setAction(SpeakerService.SPEAK_ACTION);
        Presenter.setType(intent, MmsPresenter.class);
        intent.putExtra(EXTRA_SENDER, sender);
        context.startService(intent);
    }

    public MmsPresenter(Context context) {
        super(context);
    }

    @NonNull
    @Override
    public Optional<List<TextWithLocale>> getAnnouncement(Intent intent) {
        // It's OK for the sender to be null, we'll just say it's unknown
        CharSequence sender = intent.getCharSequenceExtra(EXTRA_SENDER);

        String presentableSender =
            LookupUtils.getNameForNumber(context, sender)
                .or(context.getString(R.string.unknown_sender));

        Translations translations = new Translations(context, Locale.getDefault(), R.string.mms_from_x);
        return Optional.of(translations.format(R.string.mms_from_x, presentableSender));
    }

    @Override
    public boolean isEnabled() {
        // We use the same setting for both SMS and MMS presentations
        return isEnabled(SmsPresenter.class);
    }
}
