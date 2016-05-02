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

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Locale;

public class TextWithLocaleTest {
    @Test
    public void testFormat() {
        List<TextWithLocale> testMe = TextWithLocale.format(Locale.ENGLISH, "");
        Assert.assertEquals(1, testMe.size());
        Assert.assertEquals(testMe.get(0).text, "");
    }

    @Test
    public void testFormat1() {
        List<TextWithLocale> testMe = TextWithLocale.format(Locale.ENGLISH, "%d %d", 1, 2);
        Assert.assertEquals(1, testMe.size());
        Assert.assertEquals(testMe.get(0).text, "1 2");
    }

    @Test
    public void testFormatMultiLocale() {
        List<TextWithLocale> testMe =
            TextWithLocale.format(Locale.ENGLISH, "German: %s", Locale.GERMAN, "Deutsch");
        Assert.assertEquals(2, testMe.size());
        Assert.assertEquals(new TextWithLocale(Locale.ENGLISH, "German: "), testMe.get(0));
        Assert.assertEquals(new TextWithLocale(Locale.GERMAN, "Deutsch"), testMe.get(1));
    }

    @Test
    public void testFormatMergeMultiLocale() {
        List<TextWithLocale> testMe =
            TextWithLocale.format(Locale.ENGLISH, "English: %s", Locale.ENGLISH, "Really English");
        Assert.assertEquals(1, testMe.size());
        Assert.assertEquals(new TextWithLocale(Locale.ENGLISH, "English: Really English"), testMe.get(0));
    }

    @Test
    public void testFormatBadMultiLocale() {
        try {
            TextWithLocale.format(Locale.ENGLISH, "x %s y", Locale.GERMAN, "z");
            Assert.fail("Expected exception");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals(
                "Format string must end with %s when providing a secondary locale: <x %s y>", e.getMessage());
        }
    }
}
