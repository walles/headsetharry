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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.gmail.walles.johan.headsetharry.TextWithLocale;
import com.gmail.walles.johan.headsetharry.settings.LanguagesPreference;
import com.google.common.base.Optional;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;

import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public abstract class Presenter {
    @NonNls
    private static final String EXTRA_TYPE = "com.gmail.walles.johan.headsetharry.type";

    protected static void setType(Intent intent, Class<? extends Presenter> type) {
        intent.putExtra(EXTRA_TYPE, type.getSimpleName());
    }

    public static boolean hasType(Intent intent, Class<? extends Presenter> type) {
        String intentType = intent.getStringExtra(EXTRA_TYPE);
        return type.getSimpleName().equals(intentType);
    }

    public abstract boolean isEnabled();

    public abstract Optional<List<TextWithLocale>> getAnnouncement(Intent intent);

    protected final Context context;

    protected Presenter(Context context) {
        this.context = context;
    }

    protected boolean isEnabled(Class<? extends Presenter> klass) {
        String key = klass.getSimpleName();

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (!preferences.contains(key)) {
            @NonNls String message =
                "No preference for class, defaulting to false, check preferences.xml: " + key;
            Timber.w(new IllegalArgumentException(message), message);
        }

        return preferences.getBoolean(key, false);
    }

    @NonNull
    private List<LanguageProfile> getLanguageProfiles() {
        List<LanguageProfile> languageProfiles = new LinkedList<>();
        LanguageProfileReader languageProfileReader = new LanguageProfileReader();

        for (String localeCode: LanguagesPreference.getValues(context)) {
            try {
                languageProfiles.add(languageProfileReader.readBuiltIn(LdLocale.fromString(localeCode)));
            } catch (IOException e) {
                @NonNls String message = "Failed to load configured language " + localeCode;
                Timber.e(new Exception(message), message);
            }
        }
        return languageProfiles;
    }

    protected Optional<Locale> identifyLanguage(CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            return Optional.absent();
        }

        List<LanguageProfile> languageProfiles = getLanguageProfiles();
        LanguageDetector languageDetector =
            LanguageDetectorBuilder.create(NgramExtractors.standard())
                .withProfiles(languageProfiles)
                .build();
        Optional<LdLocale> ldLocale = languageDetector.detect(text);

        if (!ldLocale.isPresent()) {
            String languages = "";
            for (LanguageProfile languageProfile: languageProfiles) {
                if (languages.length() > 0) {
                    languages += ",";
                }
                languages += languageProfile.getLocale();
            }
            Timber.w("Unable to detect language among <%s> for: <%s>", languages, text);
            return Optional.absent();
        }

        return Optional.of(new Locale(ldLocale.get().toString()));
    }
}
