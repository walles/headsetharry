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
