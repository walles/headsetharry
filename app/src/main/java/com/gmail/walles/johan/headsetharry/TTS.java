package com.gmail.walles.johan.headsetharry;

import java.util.Locale;

import timber.log.Timber;

public class TTS {
    public static class UnsupportedLocaleException extends Exception {
        public UnsupportedLocaleException(Locale locale) {
            super("Unsupported locale: " + locale);
        }
    }

    /**
     * Speak the given text using a good voice for the given locale.
     * @param text The text to speak
     * @param locale The text's locale
     */
    void speak(CharSequence text, Locale locale) throws UnsupportedLocaleException {
        Timber.i("FIXME: Speak in locale <%s>: %s", locale, text);
    }
}
