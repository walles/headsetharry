package com.gmail.walles.johan.headsetharry;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class TtsUtil {
    public interface CompletionListener {
        void onSuccess(TextToSpeech textToSpeech);
        void onFailure(@NonNls String message);
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

        public void getEngine() {
            candidate = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.SUCCESS) {
                        @NonNls String message = "Failed to initialize system default TTS: " + status;
                        Timber.e(new Exception(message), message);
                        return;
                    }

                    remainingEnginePackageNames = sortEngines(candidate.getEngines(), candidate.getDefaultEngine());
                    Timber.d("System TTS packages: %s", remainingEnginePackageNames);
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

    public static void getEngineForLocale(Context context, Locale locale, CompletionListener callback) {
        new EngineGetter(context, locale, callback).getEngine();
    }
}
