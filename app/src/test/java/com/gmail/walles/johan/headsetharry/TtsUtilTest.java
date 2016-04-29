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

import java.util.Locale;

public class TtsUtilTest {
    @Test
    public void testGetLowerPrecisionLocale() {
        Assert.assertEquals(
            new Locale("xx", "yy"),
            TtsUtil.getLowerPrecisionLocale(new Locale("xx", "yy", "zz")).get());

        Assert.assertEquals(
            new Locale("xx"),
            TtsUtil.getLowerPrecisionLocale(new Locale("xx", "yy")).get());

        Assert.assertFalse(TtsUtil.getLowerPrecisionLocale(new Locale("xx")).isPresent());
    }
}
