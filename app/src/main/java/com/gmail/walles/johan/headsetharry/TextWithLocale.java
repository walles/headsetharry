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

import java.util.ArrayList;
import java.util.Arrays;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TextWithLocale that = (TextWithLocale)o;

        if (!text.equals(that.text)) {
            return false;
        }
        return locale.equals(that.locale);

    }

    @Override
    public int hashCode() {
        int result = text.hashCode();
        result = 31 * result + locale.hashCode();
        return result;
    }

    /**
     * Like {@link String#format(Locale, String, Object...)} except that you can put an extra Locale
     * argument before the last argument.
     * <p/>
     * Assuming the format string ends with %s or %N$s where N is the highest number in the string,
     * this method will return two parts. The first with the initial locale and the second with the
     * extra locale.
     * <p/>
     * With no extra Locale argument, this method works just like String.format().
     *
     * @see Translations#format(int, Object...)
     */
    public static List<TextWithLocale> format(Locale locale, String format, Object... args) {
        if (args.length >= 2 && args[args.length - 2] instanceof Locale) {
            // Next to last argument is a locale
            Locale secondLocale = (Locale)args[args.length - 2];
            if (!secondLocale.getLanguage().equals(locale.getLanguage())) {
                // Second locale is different from the first
                if (!format.endsWith("s")) { //NON-NLS
                    // Since the format string can end with both %s and %3$s and we don't want to
                    // put too much effort into this, settle for ending with "s".
                    throw new IllegalArgumentException(
                        "Format string must end with %s when providing a secondary locale: <" + format + ">");
                }

                Object lastObject = args[args.length - 1];

                // Replace the last two elements of args with an empty string...
                List<Object> strippedArgs = new ArrayList<>(Arrays.asList(args));
                strippedArgs.remove(strippedArgs.size() - 1);
                strippedArgs.remove(strippedArgs.size() - 1);
                strippedArgs.add("");
                args = strippedArgs.toArray();

                List<TextWithLocale> returnMe = new LinkedList<>();
                returnMe.add(TextWithLocale.format(locale, format, args).get(0));
                returnMe.add(new TextWithLocale(secondLocale, lastObject.toString()));
                return returnMe;
            } else {
                // Just drop the second locale, it's the same as the first...
                List<Object> strippedArgs = new ArrayList<>(Arrays.asList(args));
                strippedArgs.remove(strippedArgs.size() - 2);
                args = strippedArgs.toArray();

                // ... then we just fall out of this if statement and go on with the ordinary
                // processing, pretending the second locale wasn't ever there.
            }
        }

        return new TextWithLocale(locale, String.format(locale, format, args)).toList();
    }

    /**
     * Set text to the name of the locale in the locale's language.
     */
    public TextWithLocale(Locale locale) {
        this.locale = locale;
        this.text = locale.getDisplayName(locale);
    }

    private List<TextWithLocale> toList() {
        List<TextWithLocale> list = new LinkedList<>();
        list.add(this);
        return list;
    }

    @Override
    public String toString() {
        return locale.toString() + ": " + text;
    }
}
