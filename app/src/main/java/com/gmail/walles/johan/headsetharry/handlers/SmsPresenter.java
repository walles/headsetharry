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
import android.text.TextUtils;

import com.gmail.walles.johan.headsetharry.LookupUtils;
import com.gmail.walles.johan.headsetharry.R;
import com.gmail.walles.johan.headsetharry.SpeakerService;
import com.gmail.walles.johan.headsetharry.TextWithLocale;
import com.gmail.walles.johan.headsetharry.Translations;
import com.google.common.base.Optional;

import org.jetbrains.annotations.NonNls;

import java.util.List;
import java.util.Locale;

public class SmsPresenter extends Presenter {
    @NonNls
    private static final String EXTRA_BODY = "com.gmail.walles.johan.headsetharry.body";
    @NonNls
    private static final String EXTRA_SENDER = "com.gmail.walles.johan.headsetharry.sender";

    public static void speak(Context context, CharSequence body, CharSequence sender) {
        Intent intent = new Intent(context, SpeakerService.class);
        intent.setAction(SpeakerService.SPEAK_ACTION);
        Presenter.setType(intent, SmsPresenter.class);
        intent.putExtra(EXTRA_BODY, body);
        intent.putExtra(EXTRA_SENDER, sender);
        context.startService(intent);
    }

    public SmsPresenter(Context context) {
        super(context);
    }

    @NonNull
    @Override
    public Optional<List<TextWithLocale>> getAnnouncement(Intent intent) {
        CharSequence body = Optional.fromNullable(intent.getCharSequenceExtra(EXTRA_BODY)).or("");

        // It's OK for the sender to be null, we'll just say it's unknown
        CharSequence sender = intent.getCharSequenceExtra(EXTRA_SENDER);

        Optional<Locale> smsBodyLocale = identifyLanguage(body);
        Translations translations = new Translations(context, smsBodyLocale.or(Locale.getDefault()),
            R.string.sms,
            R.string.empty_sms,
            R.string.unknown_language_sms,
            R.string.unknown_sender,
            R.string.what_from_where_colon_body);

        String presentableSender;
        if (TextUtils.isEmpty(sender)) {
            presentableSender = translations.getString(R.string.unknown_sender);
        } else {
            presentableSender =
                LookupUtils.getNameForNumber(context, sender.toString())
                    .or(translations.getString(R.string.unknown_sender));
        }

        if (TextUtils.isEmpty(body)) {
            return Optional.of(translations.format(R.string.what_from_where_colon_body,
                translations.getString(R.string.empty_sms), presentableSender, body));
        }

        String sms;
        if (smsBodyLocale.isPresent()) {
            sms = translations.getString(R.string.sms);
        } else {
            sms = translations.getString(R.string.unknown_language_sms);
        }
        return Optional.of(translations.format(R.string.what_from_where_colon_body,
            sms, presentableSender, smsBodyLocale.or(Locale.getDefault()), body));
    }

    @Override
    public boolean isEnabled() {
        return isEnabled(getClass());
    }
}
