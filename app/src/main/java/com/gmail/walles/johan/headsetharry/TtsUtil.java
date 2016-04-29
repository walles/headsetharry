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
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.Nullable;

import com.google.common.base.Optional;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class TtsUtil {
    /**
     * Speak the given text using the given TTS, then shut down the TTS.
     */
    private static void speakAndShutdown(
        final Context context, final TextToSpeech tts, final CharSequence text, final boolean bluetoothSco)
    {
        speakAndShutdown(context, tts, text, bluetoothSco, null);
    }

    /**
     * Speak the given text using the given TTS, then shut down the TTS.
     *
     * @param completionListener If non-null, will be called after speaking is done
     */
    private static void speakAndShutdown(final Context context,
                                         final TextToSpeech tts,
                                         final CharSequence text,
                                         final boolean bluetoothSco,
                                         @Nullable final CompletionListener completionListener)
    {
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                Timber.v("Speech started...");
            }

            @Override
            public void onDone(String utteranceId) {
                Timber.v("Speech successfully completed");
                tts.shutdown();

                stopBluetoothSco();

                if (completionListener != null) {
                    completionListener.onSuccess(tts);
                }
            }

            @Override
            public void onError(String utteranceId) {
                @NonNls String message = "Speech failed: <" + text + ">";
                Timber.e(new Exception(message), message);
                tts.shutdown();

                stopBluetoothSco();

                if (completionListener != null) {
                    completionListener.onFailure(message);
                }
            }

            private void stopBluetoothSco() {
                if (bluetoothSco) {
                    AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
                    if (audioManager != null) {
                        @NonNls String status = audioManager.isBluetoothScoOn() ? "enabled": "disabled";
                        Timber.d("Disabling SCO, was %s", status);
                        audioManager.setBluetoothScoOn(false);
                        audioManager.stopBluetoothSco();
                    }
                }
            }
        });

        @NonNls HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "oh, the uniqueness");

        if (bluetoothSco) {
            Timber.v("Asking TTS to speak over Bluetooth SCO");
            params.put(
                TextToSpeech.Engine.KEY_PARAM_STREAM,
                Integer.toString(AudioManager.STREAM_VOICE_CALL));
        }

        //noinspection deprecation
        tts.speak(text.toString(), TextToSpeech.QUEUE_ADD, params);
    }

    public static void speak(
        final Context context, final CharSequence text, final Locale locale, final boolean bluetoothSco)
    {
        Timber.i("Speaking in locale <%s>: <%s>", locale, text);
        getEngineForLocale(context, locale, new CompletionListener() {
            @Override
            public void onSuccess(TextToSpeech textToSpeech) {
                speakAndShutdown(context, textToSpeech, text, bluetoothSco);
            }

            @Override
            public void onFailure(String message) {
                Optional<Locale> lowerPrecisionLocale = getLowerPrecisionLocale(locale);
                if (lowerPrecisionLocale.isPresent()) {
                    Timber.i("Speech failed for locale <%s>, trying <%s>: %s",
                        locale, lowerPrecisionLocale.get(), message);
                    speak(context, text, lowerPrecisionLocale.get(), bluetoothSco);
                    return;
                }

                Timber.e(new Exception(message), "Speech failed: %s", message);
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

    public static void testSpeakLocales(final Context context,
                                        Collection<Locale> locales,
                                        final TestFailureListener testFailureListener,
                                        final boolean bluetoothSco)
    {
        Timber.i("Test speaking locales <%s>", locales);
        if (locales.isEmpty()) {
            Timber.i("Not speaking, empty locales list");
            return;
        }

        final List<Locale> fewerLocales = new LinkedList<>(locales);
        final Locale locale = fewerLocales.remove(0);
        getEngineForLocale(context, locale, new CompletionListener() {
            @Override
            public void onSuccess(TextToSpeech textToSpeech) {
                speakAndShutdown(context, textToSpeech, locale.getDisplayName(locale), bluetoothSco,
                    new CompletionListener() {
                        @Override
                        public void onSuccess(TextToSpeech textToSpeech) {
                            if (fewerLocales.isEmpty()) {
                                // Done!
                                return;
                            }

                            testSpeakLocales(context, fewerLocales, testFailureListener, bluetoothSco);
                        }

                        @Override
                        public void onFailure(@NonNls String message) {
                            testFailureListener.onTestSpeakLocaleFailed(locale);
                        }
                    });
            }

            @Override
            public void onFailure(String message) {
                Timber.e(new Exception(message), "Speech failed: %s", message);
                testFailureListener.onTestSpeakLocaleFailed(locale);
            }
        });
    }

    public interface CompletionListener {
        void onSuccess(TextToSpeech textToSpeech);
        void onFailure(@NonNls String message);
    }

    public interface TestFailureListener {
        void onTestSpeakLocaleFailed(Locale locale);
    }

    private static class EngineGetter {
        private final Context context;
        private final Locale locale;
        private final CompletionListener callback;
        private TextToSpeech candidate;
        private List<String> remainingEnginePackageNames;

        public EngineGetter(
            Context context, Locale locale, CompletionListener callback)
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
                callback.onFailure("No engine found for " + locale);
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
                        callback.onSuccess(candidate);
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

    private TtsUtil() {
        throw new UnsupportedOperationException("Utility class, don't instantiate");
    }

    private static void getEngineForLocale(Context context, Locale locale, CompletionListener callback) {
        new EngineGetter(context, locale, callback).getEngine();
    }
}
