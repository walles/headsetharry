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
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.gmail.walles.johan.headsetharry.TextWithLocale;
import com.gmail.walles.johan.headsetharry.Translations;
import com.gmail.walles.johan.headsetharry.LookupUtils;
import com.gmail.walles.johan.headsetharry.R;
import com.gmail.walles.johan.headsetharry.SpeakerService;
import com.google.common.base.Optional;

import org.jetbrains.annotations.NonNls;

import java.util.List;
import java.util.Locale;

public class SmsPresenter extends Presenter {
    @NonNls
    private static final String EXTRA_BODY = "com.gmail.walles.johan.headsetharry.body";
    @NonNls
    private static final String EXTRA_SENDER = "com.gmail.walles.johan.headsetharry.sender";

    @NonNls
    public static final String TYPE = "SMS";

    private final TextWithLocale announcement;

    public static void speak(Context context, CharSequence body, CharSequence sender) {
        Intent intent = new Intent(context, SpeakerService.class);
        intent.setAction(SpeakerService.SPEAK_ACTION);
        intent.putExtra(SpeakerService.EXTRA_TYPE, TYPE);
        intent.putExtra(EXTRA_BODY, body);
        intent.putExtra(EXTRA_SENDER, sender);
        context.startService(intent);
    }

    @Override
    public List<TextWithLocale> getAnnouncement() {
        return announcement.toList();
    }

    public SmsPresenter(Context context, Intent intent) {
        super(context);

        CharSequence body = intent.getCharSequenceExtra(EXTRA_BODY);
        if (body == null) {
            body = "";
        }

        // It's OK for the sender to be null, we'll just say it's unknown
        CharSequence sender = intent.getCharSequenceExtra(EXTRA_SENDER);

        announcement = createAnnouncement(body, sender);
    }

    private TextWithLocale createAnnouncement(CharSequence body, @Nullable CharSequence sender)
    {
        Optional<Locale> locale = identifyLanguage(body);
        Translations translations = new Translations(context, locale.or(Locale.getDefault()),
            R.string.sms,
            R.string.empty_sms,
            R.string.unknown_language_sms,
            R.string.unknown_sender,
            R.string.what_from_where_colon_body);

        if (TextUtils.isEmpty(sender)) {
            sender = translations.getString(R.string.unknown_sender);
        } else {
            sender =
                LookupUtils.getNameForNumber(context, sender.toString())
                    .or(translations.getString(R.string.unknown_sender));
        }

        if (TextUtils.isEmpty(body)) {
            return TextWithLocale.format(Locale.getDefault(), translations.getString(R.string.what_from_where_colon_body),
                translations.getString(R.string.empty_sms), sender, body);
        }

        String sms;
        if (locale.isPresent()) {
            sms = translations.getString(R.string.sms);
        } else {
            sms = translations.getString(R.string.unknown_language_sms);
        }
        return TextWithLocale.format(locale.or(Locale.getDefault()),
            translations.getString(R.string.what_from_where_colon_body), sms, sender, body);
    }
}
