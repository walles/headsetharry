package com.gmail.walles.johan.headsetharry;

import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;

import com.google.common.base.Optional;

import java.util.Locale;
import java.util.Set;

import timber.log.Timber;

public class TtsUtils {
    private TtsUtils() {
        // This constructor prevents this (utility class) from being instantiated
    }

    public static Optional<Voice> getVoiceForEngineAndLocale(TextToSpeech tts, Locale locale) {
        final Set<Voice> voices = tts.getVoices();
        if (voices == null) {
            Timber.w("Speech engine doesn't list any voices");
            return Optional.absent();
        }

        for (Voice voice: voices) {
            if (!voice.getLocale().getLanguage().equals(locale.getLanguage())) {
                continue;
            }

            if (voice.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)) {
                Timber.w("Voice not installed: %s (%s)", voice.getName(), locale.getDisplayLanguage());
                continue;
            }

            // FIXME: Should we look for more after the first match?
            return Optional.of(voice);
        }

        return Optional.absent();
    }
}
