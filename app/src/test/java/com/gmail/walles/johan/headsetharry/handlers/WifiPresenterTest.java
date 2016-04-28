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

import junit.framework.Assert;

import org.junit.Test;

public class WifiPresenterTest {
    @Test
    public void testSpacify() {
        Assert.assertEquals("Foo", WifiPresenter.spacify("Foo").toString());
        Assert.assertEquals("Ownit 99", WifiPresenter.spacify("Ownit 99").toString());
        Assert.assertEquals("99 monkeys", WifiPresenter.spacify("99monkeys").toString());
        Assert.assertEquals("Ownit 99", WifiPresenter.spacify("Ownit-99").toString());
        Assert.assertEquals("Ownit Pownit", WifiPresenter.spacify("OwnitPownit").toString());
        Assert.assertEquals("Ownit POWNIT", WifiPresenter.spacify("OwnitPOWNIT").toString());
    }
}
