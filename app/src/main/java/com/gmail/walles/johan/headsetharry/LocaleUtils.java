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

import java.util.Locale;

public class LocaleUtils {
    private LocaleUtils() {
        throw new UnsupportedOperationException("This is a utility class, please don't instantiate");
    }

    public static Locale parseLocaleString(String string) {
        String parts[] = string.split("_", -1);
        if (parts.length == 1) {
            parts = string.split("-", -1);
        }

        if (parts.length == 1) {
            return new Locale(parts[0]);
        }

        if (parts.length == 2
            || (parts.length == 3 && parts[2].startsWith("#"))) {
            return new Locale(parts[0], parts[1]);
        }

        if (parts.length == 3) {
            return new Locale(parts[0], parts[1], parts[2]);
        }

        throw new IllegalArgumentException("Failed to parse locale string: <" + string + ">");
    }
}
