package com.gmail.walles.johan.headsetharry;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.support.annotation.Nullable;

import com.google.common.base.Optional;

import java.util.Locale;

import timber.log.Timber;

public class SpeakerService extends Service {
    private static final String SPEAK_ACTION = "com.gmail.walles.johan.headsetharry.speak_action";
    private static final String TEXT_EXTRA = "com.gmail.walles.johan.headsetharry.text";
    private static final String LOCALE_EXTRA = "com.gmail.walles.johan.headsetharry.locale";

    private class SpeakOnce {
        private TextToSpeech output;
        private Handler handler = new Handler();

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

            output.speak(text, TextToSpeech.QUEUE_ADD, null, "oh, the uniqueness");
        }

        public void speak(final CharSequence text, final Locale locale) {
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
                                doSpeak(locale.getDisplayLanguage() + ": " + text);
                            } catch (Exception e) {
                                Timber.e(e, "Speaking failed: " + text);
                            }
                        }
                    });
                }
            };

            // FIXME: Use a TTS engine that supports the requested locale
            output = new TextToSpeech(SpeakerService.this, listener);

            // Tell the TTS engine to use a voice that supports the current locale
            Optional<Voice> optionalVoice = TtsUtils.getVoiceForEngineAndLocale(output, locale);
            if (optionalVoice.isPresent()) {
                Timber.i("Using voice: %s", optionalVoice.get().getName());
                output.setVoice(optionalVoice.get());
            } else {
                Timber.w("No %s voice found, using default voice", locale.getDisplayLanguage());
            }
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

    private void speak(CharSequence text, Locale locale) {
        Timber.i("Speaking in locale <%s>: <%s>", locale, text);
        new SpeakOnce().speak(text, locale);
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

        speak(text, locale);
        return Service.START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Binding unsupported, please use startService() instead");
    }
}
