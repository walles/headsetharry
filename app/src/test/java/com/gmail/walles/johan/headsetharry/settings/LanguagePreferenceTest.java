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
