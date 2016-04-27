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
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gmail.walles.johan.headsetharry.settings;

import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;

public class LanguagePreferenceTest {
    @Test
    public void testGetDetectableLocale() {
        Assert.assertEquals("sv",
            LanguagesPreference.getDetectableLocaleCode(new Locale("sv")).get());
        Assert.assertEquals("sv",
            LanguagesPreference.getDetectableLocaleCode(new Locale("sv", "SE")).get());

        Assert.assertFalse(LanguagesPreference.getDetectableLocaleCode(new Locale("zh")).isPresent());
        Assert.assertFalse(LanguagesPreference.getDetectableLocaleCode(new Locale("zh", "XX")).isPresent());

        Assert.assertEquals("zh-CN",
            LanguagesPreference.getDetectableLocaleCode(new Locale("zh", "CN")).get());
        Assert.assertEquals("zh-TW",
            LanguagesPreference.getDetectableLocaleCode(new Locale("zh", "TW")).get());

        Assert.assertFalse("'xx' is not a valid language, and not supported by our language detector",
            LanguagesPreference.getDetectableLocaleCode(new Locale("xx")).isPresent());
    }
}
