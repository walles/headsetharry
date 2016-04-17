package com.gmail.walles.johan.headsetharry;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.common.base.Optional;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import timber.log.Timber;

public class SpeakerService extends Service {
    private static final String SPEAK_ACTION = "com.gmail.walles.johan.headsetharry.speak_action";

    private static final String EXTRA_TYPE = "com.gmail.walles.johan.headsetharry.type";
    private static final String EXTRA_BODY = "com.gmail.walles.johan.headsetharry.body";
    private static final String EXTRA_SENDER = "com.gmail.walles.johan.headsetharry.sender";

    private static final String TYPE_SMS = "SMS";

    public static void speakSms(Context context, CharSequence body, CharSequence sender) {
        Intent intent = new Intent(context, SpeakerService.class);
        intent.setAction(SPEAK_ACTION);
        intent.putExtra(EXTRA_BODY, body);
        intent.putExtra(EXTRA_SENDER, sender);
        intent.putExtra(EXTRA_TYPE, TYPE_SMS);
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
        Timber.i("Speaking in locale <%s>: <%s>", locale, text);
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

    public static boolean isRunningOnEmulator() {
        // Inspired by
        // http://stackoverflow.com/questions/2799097/how-can-i-detect-when-an-android-application-is-running-in>
        if (Build.PRODUCT == null) {
            return false;
        }

        Set<String> parts = new HashSet<>(Arrays.asList(Build.PRODUCT.split("_")));
        if (parts.size() == 0) {
            return false;
        }

        parts.remove("sdk");
        parts.remove("google");
        parts.remove("x86");
        parts.remove("phone");

        // If the build identifier contains only the above keywords in some order, then we're
        // in an emulator
        return parts.isEmpty();
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
            return START_NOT_STICKY;
        }

        if (audioManager.isBluetoothA2dpOn()) {
            Timber.d("Speaking, A2DP enabled");
            handleIntent(intent);
            return Service.START_NOT_STICKY;
        } else if (isRunningOnEmulator()) {
            Timber.d("Speaking, running in emulator");
            handleIntent(intent);
            return Service.START_NOT_STICKY;
        } else {
            Timber.i("Not speaking; no headphones detected");
            return START_NOT_STICKY;
        }
    }

    private void handleIntent(Intent intent) {
        String type = intent.getStringExtra(EXTRA_TYPE);
        if (TextUtils.isEmpty(type)) {
            Timber.e("Speak action with no type");
            return;
        }

        if (TYPE_SMS.equals(type)) {
            handleSmsIntent(intent);
        } else {
            Timber.w("Ignoring incoming intent of type %s");
            return;
        }
    }

    private void handleSmsIntent(Intent intent) {
        CharSequence body = intent.getCharSequenceExtra(EXTRA_BODY);
        if (body == null) {
            Timber.e("Speak SMS intent with null body");
            return;
        }

        CharSequence sender = intent.getCharSequenceExtra(EXTRA_SENDER);
        if (sender == null) {
            Timber.e("Speak SMS intent with null sender");
            return;
        }

        Optional<Locale> optionalLocale = Optional.absent();
        try {
            optionalLocale = getLocale(body);
        } catch (IOException e) {
            Timber.e(e, "Language detection failed");
        }

        Locale locale;
        if (optionalLocale.isPresent()) {
            locale = optionalLocale.get();
        } else {
            // Unable to find out from the message, go with the system locale
            locale = Locale.getDefault();
        }

        speak(toSmsAnnouncement(body, sender, optionalLocale), locale);
    }

    private CharSequence toSmsAnnouncement(
        CharSequence body, CharSequence sender, Optional<Locale> optionalLocale)
    {
        // FIXME: Get an SMS message template for the given locale and fill that in.
        // FIXME: Fall back to getting a template for the system locale if that can't be found.
        // FIXME: Fall back to getting a template in English if that can't be found. Or a generic one?

        // FIXME: Localize all strings in this method

        if (sender == null) {
            sender = "unknown sender";
        }

        return String.format("%sSMS from %s: %s",
            optionalLocale.isPresent() ? "" : "Unknown language ",
            sender, body);
    }

    private Optional<Locale> getLocale(CharSequence text) throws IOException {
        List<LanguageProfile> languageProfiles = new LinkedList<>();
        LanguageProfileReader languageProfileReader = new LanguageProfileReader();

        // FIXME: Use locales for which there are voices present on the system
        languageProfiles.add(languageProfileReader.readBuiltIn(LdLocale.fromString("en")));
        languageProfiles.add(languageProfileReader.readBuiltIn(LdLocale.fromString("sv")));

        LanguageDetector languageDetector =
            LanguageDetectorBuilder.create(NgramExtractors.standard())
                .withProfiles(languageProfiles)
                .build();
        Optional<LdLocale> ldLocale = languageDetector.detect(text);

        if (!ldLocale.isPresent()) {
            String languages = "";
            for (LanguageProfile languageProfile: languageProfiles) {
                if (languages.length() > 0) {
                    languages += ",";
                }
                languages += languageProfile.getLocale();
            }
            Timber.w("Unable to detect language among <%s> for: <%s>", languages, text);
            return Optional.absent();
        }

        return Optional.of(new Locale(ldLocale.get().toString()));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Binding unsupported, please use startService() instead");
    }
}
