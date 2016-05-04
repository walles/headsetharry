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

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class SpeakerServiceTest {

    private static final String SAMPLE_TEXT = "flaska";

    private List<TextWithLocale> toList(String text) {
        List<TextWithLocale> returnMe = new LinkedList<>();
        returnMe.add(new TextWithLocale(Locale.getDefault(), text));
        return returnMe;
    }

    @Test
    public void testIsDuplicate() {
        SpeakerService testMe = new SpeakerService();

        // Tests that the initial comparison is false
        Assert.assertFalse(testMe.isDuplicate(toList(SAMPLE_TEXT)));
        Assert.assertTrue(testMe.isDuplicate(toList(SAMPLE_TEXT)));

        // Tests that following comparisons are false
        Assert.assertFalse(testMe.isDuplicate(toList("gris")));
        Assert.assertTrue(testMe.isDuplicate(toList("gris")));
    }

    @Test
    public void testIsDuplicateTimeout() throws Exception {
        SpeakerService testMe = new SpeakerService();
        testMe.setIsDuplicateTimeoutMs(50);

        Assert.assertFalse(testMe.isDuplicate(toList(SAMPLE_TEXT)));
        Assert.assertTrue(testMe.isDuplicate(toList(SAMPLE_TEXT)));

        Thread.sleep(100);
        Assert.assertFalse("Repetitions should be OK after the timeout",
            testMe.isDuplicate(toList(SAMPLE_TEXT)));
        Assert.assertTrue(testMe.isDuplicate(toList(SAMPLE_TEXT)));
    }
}
