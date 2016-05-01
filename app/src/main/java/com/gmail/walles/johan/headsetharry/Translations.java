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

import java.util.HashMap;
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
     * Note that before using these strings you may want to check what locale you actually got. That
     * is done by calling {@link #getLocale()}.
     */
    // From: http://stackoverflow.com/a/9475663/473672
    public Translations(Context context, Locale locale, int ... resourceIds) {
        idToStrings = new HashMap<>();

        Resources res = context.getResources();
        Configuration conf = res.getConfiguration();
        Locale savedLocale = conf.locale;
        conf.locale = locale;
        res.updateConfiguration(conf, null); // second arg null means don't change display metrics

        this.locale = LocaleUtils.parseLocaleString(res.getString(R.string.locale));

        // retrieve resources from desired locale
        for (int resourceId: resourceIds) {
            idToStrings.put(resourceId, res.getString(resourceId));
        }

        // restore original locale
        conf.locale = savedLocale;
        res.updateConfiguration(conf, null);
    }

    /**
     * Retrieve a string for the locale returned by {@link #getLocale()}.
     */
    public String getString(int resourceId) {
        String string = idToStrings.get(resourceId);
        if (string == null) {
            throw new IllegalArgumentException("Invalid resource ID " + resourceId);
        }
        return string;
    }

    /**
     * Get the locale for which {@link #getString(int)} returns strings.
     */
    public Locale getLocale() {
        return locale;
    }
}
