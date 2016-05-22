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

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.annotation.StringRes;

import com.crashlytics.android.answers.CustomEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fetch strings for a given locale.
 */
public class Translations {
    private final Locale locale;
    private final Map<Integer, String> idToStrings;

    /**
     * Fetch strings for a given locale.
     * <p/>
     * Note that you may not get the locale you asked for. Therefore, always access these strings by
     * using {@link #format(int, Object...)} to format your output.
     * <p/>
     * Locales are tried in the following order:<ol>
     * <li>First we try to find strings for the requested locale
     * <li>If that fails, we try to find strings for the system locale
     * <li>If that fails, we use strings from the fallback locale ("en")
     * </ol>
     */
    // From: http://stackoverflow.com/a/9475663/473672
    public Translations(Context context, Locale locale, @StringRes int ... resourceIds) {
        idToStrings = new HashMap<>();

        Resources res = context.getResources();
        Configuration conf = res.getConfiguration();
        Locale savedLocale = conf.locale;

        try {
            conf.locale = locale;
            res.updateConfiguration(conf, null); // second arg null means don't change display metrics
            Locale foundLocale = LocaleUtils.parseLocaleString(res.getString(R.string.locale));

            if (!foundLocale.getLanguage().equals(conf.locale.getLanguage())) {
                LoggingUtils.logCustom(
                    new CustomEvent("Retrieve Translations"). //NON-NLS
                        putCustomAttribute("Requested Translations Missing", conf.locale.toString())); //NON-NLS

                conf.locale = Locale.getDefault();
                res.updateConfiguration(conf, null); // second arg null means don't change display metrics
                foundLocale = LocaleUtils.parseLocaleString(res.getString(R.string.locale));

                if (!foundLocale.getLanguage().equals(conf.locale.getLanguage())) {
                    LoggingUtils.logCustom(
                        new CustomEvent("Retrieve Translations"). //NON-NLS
                            putCustomAttribute("System Locale Translations Missing", conf.locale.toString())); //NON-NLS
                }
            }
            this.locale = foundLocale;

            // retrieve resources from desired locale
            for (int resourceId: resourceIds) {
                idToStrings.put(resourceId, res.getString(resourceId));
            }
        } finally {
            // restore original locale
            conf.locale = savedLocale;
            res.updateConfiguration(conf, null);
        }
    }

    /**
     * Retrieve a string for {@link #locale}.
     */
    public String getString(@StringRes int resourceId) {
        String string = idToStrings.get(resourceId);
        if (string == null) {
            throw new IllegalArgumentException("Invalid resource ID " + resourceId);
        }
        return string;
    }

    /**
     * @see TextWithLocale#format(Locale, String, Object...)
     */
    public List<TextWithLocale> format(@StringRes int resourceId, Object ... args) {
        return TextWithLocale.format(locale, getString(resourceId), args);
    }
}
