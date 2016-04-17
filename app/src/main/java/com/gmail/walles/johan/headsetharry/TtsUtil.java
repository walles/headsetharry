package com.gmail.walles.johan.headsetharry;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.speech.tts.TextToSpeech;
import android.util.StringBuilderPrinter;

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
        private final List<String> remainingEnginePackageNames;
        private TextToSpeech candidate;

        public EngineGetter(
            Context context, Locale locale, CompletionListener callback, List<String> enginePackageNames)
        {
            this.context = context;
            this.locale = locale;
            this.callback = callback;
            this.remainingEnginePackageNames = enginePackageNames;
        }

        public void getEngine() {
            if (candidate != null) {
                candidate.shutdown();
            }

            if (remainingEnginePackageNames.isEmpty()) {
                Timber.w("No TTS engine seems to support " + locale);
                callback.onFailure("No engine found for " + locale);
                return;
            }
            final String enginePackageName = remainingEnginePackageNames.get(0);
            remainingEnginePackageNames.remove(0);
            Timber.d("Asking TTS engine %s about locale %s...", enginePackageName, locale);

            candidate = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.SUCCESS) {
                        Timber.w("Failed to initialize TTS engine %s", enginePackageName);
                        getEngine();
                        return;
                    }

                    int result = candidate.setLanguage(locale);
                    if (isSetLanguageOk(result)) {
                        // FIXME: Should we keep looking to see if we find a better one?
                        //noinspection deprecation
                        Timber.i("TTS engine %s set to %s for locale %s",
                            enginePackageName, candidate.getLanguage(), locale);
                        callback.onSuccess(candidate);
                        return;
                    }

                    // This wasn't it, try the next candidate
                    Timber.d("TTS engine %s didn't support locale %s", enginePackageName, locale);
                    getEngine();
                }
            }, enginePackageName);
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

    private static List<String> getEnginePackageNames(Context context) {
        final Intent ttsIntent = new Intent();
        ttsIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);

        // List packages responding to ACTION_CHECK_TTS_DATA
        final PackageManager pm = context.getPackageManager();
        final List<ResolveInfo> resolveInfos =
            pm.queryIntentActivities(ttsIntent, PackageManager.GET_META_DATA);

        // Extract package names only
        final List<String> returnMe = new ArrayList<>(resolveInfos.size());
        for (ResolveInfo resolveInfo: resolveInfos) {
            final String packageName = resolveInfo.activityInfo.applicationInfo.packageName;
            if (packageName == null) {
                StringBuilder stringBuilder = new StringBuilder();
                StringBuilderPrinter sbPrinter = new StringBuilderPrinter(stringBuilder);
                resolveInfo.dump(sbPrinter, "  ");
                Timber.e(new Exception(stringBuilder.toString()), "Package name is null");
                continue;
            }

            returnMe.add(packageName);
        }

        Timber.d("System TTS packages: %s", returnMe);
        return returnMe;
    }

    private TtsUtil() {
        throw new UnsupportedOperationException("Utility class, don't instantiate");
    }

    public static void getEngineForLocale(Context context, Locale locale, CompletionListener callback) {
        new EngineGetter(context, locale, callback, getEnginePackageNames(context)).getEngine();
    }
}
