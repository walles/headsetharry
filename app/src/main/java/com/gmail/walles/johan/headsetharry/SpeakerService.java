package com.gmail.walles.johan.headsetharry;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Locale;

import timber.log.Timber;

public class SpeakerService extends Service {
    private static final String SPEAK_ACTION = "com.gmail.walles.johan.headsetharry.speak_action";
    private static final String TEXT_EXTRA = "com.gmail.walles.johan.headsetharry.text";
    private static final String LOCALE_EXTRA = "com.gmail.walles.johan.headsetharry.locale";

    private class SpeakOnce {
        private TextToSpeech output;
        private final Locale locale;
        private final Handler handler = new Handler();

        public SpeakOnce(Locale locale) {
            this.locale = locale;
        }

        private void doSpeak(final CharSequence text) {
            output.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    Timber.v("Speech started...");
                }

                @Override
                public void onDone(String utteranceId) {
                    Timber.v("Speech successfully completed");
                    output.shutdown();
                }

                @Override
                public void onError(String utteranceId) {
                    String message = "Speech failed: <" + text + ">";
                    Timber.e(new Exception(message), message);
                    output.shutdown();
                }
            });

            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "oh, the uniqueness");
            //noinspection deprecation
            output.speak(text.toString(), TextToSpeech.QUEUE_ADD, params);
        }

        private boolean isSetLanguageOk(int setLanguageReturnCode) {
            switch (setLanguageReturnCode) {
                case TextToSpeech.LANG_AVAILABLE:
                case TextToSpeech.LANG_COUNTRY_AVAILABLE:
                case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
                    return true;

                default:
                    return false;
            }
        }

        @SuppressWarnings("deprecation")
        private void configureEngineAndSpeak(CharSequence text) {
            // Tell the TTS engine to use a voice that supports the current locale
            int result = output.setLanguage(locale);
            if (isSetLanguageOk(result)) {
                Timber.v("TTS configured for %s using %s", locale, output.getLanguage());
                doSpeak(text);
                return;
            }

            final Locale defaultLocale = output.getDefaultLanguage();

            Timber.w("TTS doesn't support %s (%d), using default locale %s", locale, result, defaultLocale);
            result = output.setLanguage(defaultLocale);
            if (isSetLanguageOk(result)) {
                Timber.v("TTS configured for default locale %s using %s", locale, output.getLanguage());
                doSpeak(text);
                return;
            }

            String message =
                String.format("TTS doesn't support its own default locale %s (%d)", defaultLocale, result);
            Timber.e(new Exception(message), message);
            output.shutdown();
        }

        public void speak(final CharSequence text) {
            final TextToSpeech.OnInitListener listener = new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.SUCCESS) {
                        String message = "Error initializing TTS, status code was " + status;
                        Timber.e(new Exception(message), message);
                        return;
                    }

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                // FIXME: We shouldn't say the locale name, that's for development
                                // purposes only
                                configureEngineAndSpeak(locale.getDisplayLanguage() + ": " + text);
                            } catch (Exception e) {
                                Timber.e(e, "Speaking failed: " + text);
                            }
                        }
                    });
                }
            };

            // FIXME: Use a TTS engine that supports the requested locale
            output = new TextToSpeech(SpeakerService.this, listener);
        }
    }

    public static void speak(Context context, CharSequence text, Locale locale) {
        Intent intent = new Intent(context, SpeakerService.class);
        intent.setAction(SPEAK_ACTION);
        intent.putExtra(TEXT_EXTRA, text);
        intent.putExtra(LOCALE_EXTRA, locale);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LoggingUtil.setUpLogging(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!SPEAK_ACTION.equals(intent.getAction())) {
            Timber.w("Ignoring unsupported action <%s>", intent.getAction());
            return Service.START_NOT_STICKY;
        }

        CharSequence text = intent.getCharSequenceExtra(TEXT_EXTRA);
        if (text == null) {
            Timber.e("Speak action with null text");
            return Service.START_NOT_STICKY;
        }

        Object localeObject = intent.getSerializableExtra(LOCALE_EXTRA);
        if (localeObject == null) {
            Timber.e("Speak action with null locale");
            return Service.START_NOT_STICKY;
        }

        if (!(localeObject instanceof Locale)) {
            Timber.e("Speak action locale is not a Locale: %s", localeObject.getClass());
            return Service.START_NOT_STICKY;
        }

        Locale locale = (Locale)localeObject;

        Timber.i("Speaking in locale <%s>: <%s>", locale, text);
        new SpeakOnce(locale).speak(text);

        return Service.START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Binding unsupported, please use startService() instead");
    }
}
