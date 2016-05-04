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

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.Nullable;

import com.google.common.base.Optional;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class TtsUtils {
    public interface CompletionListener {
        void onSuccess();
        void onFailure(@Nullable Locale locale, @NonNls String errorMessage);
    }

    /**
     * Speak the given text using the given TTS, then shut down the TTS.
     *
     * @param completionListener If non-null, will be called if speech fails
     */
    private static void speakAndShutdown(final Context context,
                                         final TextToSpeech tts,
                                         final List<TextWithLocale> texts,
                                         final int audioManagerStream,
                                         final CompletionListener completionListener)
    {
        if (texts.isEmpty()) {
            @NonNls String message = "Got empty speech request internally";
            Timber.e(new Exception(message), message);
            completionListener.onFailure(null, message); //NON-NLS
            tts.shutdown();
            return;
        }

        final TextWithLocale toSpeak = texts.get(0);

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                Timber.v("Speech started...");
            }

            @Override
            public void onDone(String utteranceId) {
                Timber.v("One phrase done, shutting down TTS");
                tts.shutdown();

                List<TextWithLocale> remainingTexts = new LinkedList<>(texts);
                remainingTexts.remove(0);
                if (!remainingTexts.isEmpty()) {
                    Timber.v("Speaking remaining texts: %s", remainingTexts);
                    speak(context, remainingTexts, audioManagerStream, completionListener);
                    return;
                }

                Timber.v("Speech successfully completed");
                completionListener.onSuccess();
            }

            @Override
            public void onError(String utteranceId) {
                @NonNls String errorMessage = "Speech failed: <" + toSpeak + ">";
                Timber.e(new Exception(errorMessage), errorMessage);
                tts.shutdown();

                completionListener.onFailure(toSpeak.locale, errorMessage);
            }
        });

        @NonNls HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "oh, the uniqueness");
        params.put(
            TextToSpeech.Engine.KEY_PARAM_STREAM,
            Integer.toString(audioManagerStream));

        //noinspection deprecation
        tts.speak(toSpeak.text, TextToSpeech.QUEUE_ADD, params);
    }

    public static void speak(final Context context,
                             final List<TextWithLocale> texts,
                             final int audioManagerStream,
                             final CompletionListener completionListener)
    {
        if (texts.isEmpty()) {
            @NonNls String message = "Nothing to say, never mind";
            Timber.w(new Exception(message), message);
            completionListener.onFailure(null, "Nothing to say");
            return;
        }

        final TextWithLocale text = texts.get(0);

        Timber.i("Speaking in locale <%s>: <%s>", text.locale, text.text);
        getEngineForLocale(context, text.locale, new EngineResultListener() {
            @Override
            public void onFound(TextToSpeech textToSpeech) {
                speakAndShutdown(context, textToSpeech, texts, audioManagerStream, completionListener);
            }

            @Override
            public void onNotFound() {
                Optional<Locale> lowerPrecisionLocale = getLowerPrecisionLocale(text.locale);
                if (lowerPrecisionLocale.isPresent()) {
                    Timber.i("No engine found for locale <%s>, trying <%s>",
                        text.locale, lowerPrecisionLocale.get());

                    speak(context, texts, audioManagerStream, completionListener);
                    return;
                }

                completionListener.onFailure(text.locale, "No speech engine found for " + text.locale);
            }
        });
    }

    static Optional<Locale> getLowerPrecisionLocale(Locale higherPrecision) {
        String variant = higherPrecision.getVariant();
        String country = higherPrecision.getCountry();
        String language = higherPrecision.getLanguage();

        if (variant != null && !variant.isEmpty()) {
            return Optional.of(new Locale(language, country));
        }

        if (country != null && !country.isEmpty()) {
            return Optional.of(new Locale(language));
        }

        return Optional.absent();
    }

    private interface EngineResultListener {
        void onFound(TextToSpeech tts);
        void onNotFound();
    }

    private static class EngineGetter {
        private final Context context;
        private final Locale locale;
        private final EngineResultListener callback;
        private TextToSpeech candidate;
        private List<String> remainingEnginePackageNames;

        public EngineGetter(
            Context context, Locale locale, EngineResultListener callback)
        {
            this.context = context;
            this.locale = locale;
            this.callback = callback;
        }

        /**
         * Put the system default TTS first in the list.
         */
        private static List<String> sortEngines(List<TextToSpeech.EngineInfo> engines, String defaultEngine) {
            List<String> returnMe = new ArrayList<>(engines.size());
            for (TextToSpeech.EngineInfo engine: engines) {
                String packageName = engine.name;
                if (defaultEngine.equals(packageName)) {
                    returnMe.add(0, packageName);
                } else {
                    returnMe.add(packageName);
                }
            }

            return returnMe;
        }

        /**
         * Entry point for locating a TTS engine for a given locale.
         * <p/>
         * The end result of calling this method is that {@link #callback} gets notified.
         */
        public void getEngine() {
            final TextToSpeech[] someTts = new TextToSpeech[1];

            // We need to initialize some (any) TTS...
            someTts[0] = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    // ... so that we can query it for a list of others once it's initialized.
                    if (status != TextToSpeech.SUCCESS) {
                        @NonNls String message = "Failed to initialize system default TTS: " + status;
                        Timber.e(new Exception(message), message);
                        return;
                    }

                    remainingEnginePackageNames =
                        sortEngines(someTts[0].getEngines(), someTts[0].getDefaultEngine());
                    Timber.d("System TTS packages: %s", remainingEnginePackageNames);

                    // Start going through the remaining engine package names
                    tryNextEngine();
                }
            });
        }

        private void tryNextEngine() {
            if (candidate != null) {
                candidate.shutdown();
            }

            if (remainingEnginePackageNames.isEmpty()) {
                Timber.w("No TTS engine seems to support %s", locale);
                callback.onNotFound();
                return;
            }
            final String engine = remainingEnginePackageNames.get(0);
            remainingEnginePackageNames.remove(0);
            Timber.d("Asking TTS engine %s about locale %s...", engine, locale);

            candidate = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @SuppressWarnings("deprecation")
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.SUCCESS) {
                        Timber.w("Failed to initialize TTS engine %s", engine);
                        tryNextEngine();
                        return;
                    }

                    int result = candidate.setLanguage(locale);
                    if (isSetLanguageOk(result)) {
                        Timber.i("TTS engine %s set to %s for locale %s",
                            engine, candidate.getLanguage(), locale);
                        callback.onFound(candidate);
                        return;
                    }

                    // This wasn't it, try the next candidate
                    Timber.d("TTS engine %s didn't support locale %s", engine, locale);
                    tryNextEngine();
                }
            }, engine);
        }
    }

    private static boolean isSetLanguageOk(int setLanguageReturnCode) {
        switch (setLanguageReturnCode) {
            case TextToSpeech.LANG_AVAILABLE:
            case TextToSpeech.LANG_COUNTRY_AVAILABLE:
            case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
                return true;

            default:
                return false;
        }
    }

    private TtsUtils() {
        throw new UnsupportedOperationException("Utility class, don't instantiate");
    }

    private static void getEngineForLocale(Context context, Locale locale, EngineResultListener callback) {
        new EngineGetter(context, locale, callback).getEngine();
    }
}
