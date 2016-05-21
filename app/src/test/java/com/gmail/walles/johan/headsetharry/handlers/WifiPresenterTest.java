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

import com.gmail.walles.johan.headsetharry.TextWithLocale;

import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class WifiPresenterTest {
    @Test
    public void testPrettify() {
        Assert.assertNull(WifiPresenter.prettify(null));
        Assert.assertEquals("", WifiPresenter.prettify(""));
        Assert.assertEquals("", WifiPresenter.prettify("\"\""));
        Assert.assertEquals("Foo", WifiPresenter.prettify("Foo"));
        Assert.assertEquals("Ownit 99", WifiPresenter.prettify("Ownit 99"));
        Assert.assertEquals("99 monkeys", WifiPresenter.prettify("99monkeys"));
        Assert.assertEquals("Ownit 99", WifiPresenter.prettify("Ownit-99"));
        Assert.assertEquals("Ownit Pownit", WifiPresenter.prettify("OwnitPownit"));
        Assert.assertEquals("Ownit POWNIT", WifiPresenter.prettify("OwnitPOWNIT"));
    }

    private static final String SAMPLE_TEXT = "flaska";

    private List<TextWithLocale> toList(String text) {
        List<TextWithLocale> returnMe = new LinkedList<>();
        returnMe.add(new TextWithLocale(Locale.getDefault(), text));
        return returnMe;
    }

    @Test
    public void testIsDuplicate() {
        WifiPresenter.State testMe = new WifiPresenter.State();

        // Tests that the initial comparison is false
        Assert.assertFalse(testMe.isDuplicate(toList(SAMPLE_TEXT), true));
        Assert.assertTrue(testMe.isDuplicate(toList(SAMPLE_TEXT), true));

        // Tests that following comparisons are false
        Assert.assertFalse(testMe.isDuplicate(toList("gris"), true));
        Assert.assertTrue(testMe.isDuplicate(toList("gris"), true));
    }

    @Test
    public void testIsDuplicateTimeout() throws Exception {
        WifiPresenter.State testMe = new WifiPresenter.State();
        testMe.setIsDuplicateTimeoutMs(50);

        Assert.assertFalse(testMe.isDuplicate(toList(SAMPLE_TEXT), true));
        Assert.assertTrue(testMe.isDuplicate(toList(SAMPLE_TEXT), true));

        Thread.sleep(100);
        Assert.assertFalse("Repetitions should be OK after the timeout",
            testMe.isDuplicate(toList(SAMPLE_TEXT), true));
        Assert.assertTrue(testMe.isDuplicate(toList(SAMPLE_TEXT), true));
    }

    @Test
    public void testIsDuplicateDisconnected() throws Exception {
        WifiPresenter.State testMe = new WifiPresenter.State();
        Assert.assertFalse("First time should never be a duplicate", testMe.isDuplicate(null, false));
        Assert.assertTrue(testMe.isDuplicate(null, false));

        // As long as we're disconnected we should always say it's a duplicate
        Assert.assertTrue(testMe.isDuplicate(toList("something"), false));
        Assert.assertTrue(testMe.isDuplicate(toList("anything"), false));

        testMe.setIsDuplicateTimeoutMs(50);
        Thread.sleep(100);
        Assert.assertTrue("Disconnected dups shouldn't time out", testMe.isDuplicate(toList("anything"), false));

        Assert.assertFalse(testMe.isDuplicate(toList(SAMPLE_TEXT), true));
    }
}
