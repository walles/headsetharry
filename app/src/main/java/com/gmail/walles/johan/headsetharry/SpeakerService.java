package com.gmail.walles.johan.headsetharry;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
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

    /**
     * Speak the given text using the given TTS, then shut down the TTS.
     */
    private void speakAndShutdown(final TextToSpeech tts, final CharSequence text) {
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                Timber.v("Speech started...");
            }

            @Override
            public void onDone(String utteranceId) {
                Timber.v("Speech successfully completed");
                tts.shutdown();
            }

            @Override
            public void onError(String utteranceId) {
                String message = "Speech failed: <" + text + ">";
                Timber.e(new Exception(message), message);
                tts.shutdown();
            }
        });

        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "oh, the uniqueness");
        //noinspection deprecation
        tts.speak(text.toString(), TextToSpeech.QUEUE_ADD, params);
    }

    private void speak(final CharSequence text, final Locale locale) {
        TtsUtil.getEngineForLocale(this, locale, new TtsUtil.CompletionListener() {
            @Override
            public void onSuccess(TextToSpeech textToSpeech) {
                // FIXME: We shouldn't say the locale name, that's for development purposes only
                speakAndShutdown(textToSpeech, locale.getDisplayLanguage() + ": " + text);
            }

            @Override
            public void onFailure(String message) {
                Timber.e(new Exception(message), "Speech failed: " + message);
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!SPEAK_ACTION.equals(intent.getAction())) {
            Timber.w("Ignoring unsupported action <%s>", intent.getAction());
            return Service.START_NOT_STICKY;
        }

        AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int audioMode = audioManager.getMode();
        if (audioManager.getMode() != AudioManager.MODE_NORMAL) {
            Timber.i("Not speaking, audio mode not MODE_NORMAL: %d", audioMode);
        }

        handleIntent(intent);

        return Service.START_NOT_STICKY;
    }

    private void handleIntent(Intent intent) {
        CharSequence text = intent.getCharSequenceExtra(TEXT_EXTRA);
        if (text == null) {
            Timber.e("Speak action with null text");
            return;
        }

        Object localeObject = intent.getSerializableExtra(LOCALE_EXTRA);
        if (localeObject == null) {
            Timber.e("Speak action with null locale");
            return;
        }

        if (!(localeObject instanceof Locale)) {
            Timber.e("Speak action locale is not a Locale: %s", localeObject.getClass());
            return;
        }
        Locale locale = (Locale)localeObject;

        Timber.i("Speaking in locale <%s>: <%s>", locale, text);
        speak(text, locale);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Binding unsupported, please use startService() instead");
    }
}
