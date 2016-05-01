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

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class TextWithLocale {
    public final String text;
    public final Locale locale;

    public TextWithLocale(Locale locale, String text) {
        this.locale = locale;
        this.text = text;
    }

    public static TextWithLocale format(Locale locale, String format, Object... args) {
        return new TextWithLocale(locale, String.format(locale, format, args));
    }

    /**
     * Set text to the name of the locale in the locale's language.
     */
    public TextWithLocale(Locale locale) {
        this.locale = locale;
        this.text = locale.getDisplayName(locale);
    }

    public List<TextWithLocale> toList() {
        List<TextWithLocale> list = new LinkedList<>();
        list.add(this);
        return list;
    }

    @Override
    public String toString() {
        return locale.toString() + ": " + text;
    }
}
