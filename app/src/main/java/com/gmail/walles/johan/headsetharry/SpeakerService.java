package com.gmail.walles.johan.headsetharry;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.gmail.walles.johan.headsetharry.settings.LanguagesPreference;
import com.google.common.base.Optional;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;

import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

public class SpeakerService extends Service {
    @NonNls
    private static final String SPEAK_ACTION = "com.gmail.walles.johan.headsetharry.speak_action";

    @NonNls
    private static final String EXTRA_TYPE = "com.gmail.walles.johan.headsetharry.type";
    @NonNls
    private static final String EXTRA_BODY = "com.gmail.walles.johan.headsetharry.body";
    @NonNls
    private static final String EXTRA_SENDER = "com.gmail.walles.johan.headsetharry.sender";

    @NonNls
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

    private static boolean isRunningOnEmulator() {
        // Inspired by
        // http://stackoverflow.com/questions/2799097/how-can-i-detect-when-an-android-application-is-running-in>
        if (Build.PRODUCT == null) {
            return false;
        }

        @NonNls Set<String> parts = new HashSet<>(Arrays.asList(Build.PRODUCT.split("_")));
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

    private boolean enableBluetoothSco(AudioManager audioManager) {
        if (!audioManager.isBluetoothScoAvailableOffCall()) {
            Timber.d("Bluetooth SCO not available off call, not trying it");
            return false;
        }

        long t0 = System.currentTimeMillis();
        boolean requested = false;
        while (System.currentTimeMillis() - t0 < 3000) {
            IntentFilter filter = new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
            Intent intent = registerReceiver(null, filter);
            if (intent == null) {
                Timber.w("Got null Intent when asking for ACTION_SCO_AUDIO_STATE_UPDATED");
                audioManager.setBluetoothScoOn(false);
                audioManager.stopBluetoothSco();
                return false;
            }

            int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_ERROR);
            if (state == AudioManager.SCO_AUDIO_STATE_ERROR) {
                Timber.w("Got error result when asking for ACTION_SCO_AUDIO_STATE_UPDATED");
                audioManager.setBluetoothScoOn(false);
                audioManager.stopBluetoothSco();
                return false;
            }

            if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                if (audioManager.isBluetoothScoOn()) {
                    Timber.d("Bluetooth SCO audio connected");
                    return true;
                }

                Timber.w("Bluetooth audio connected but not enabled, keep waiting...");
            }

            if (!requested) {
                Timber.v("Requesting Bluetooth SCO audio output");
                try {
                    // From: http://stackoverflow.com/a/17150250/473672
                    audioManager.startBluetoothSco();
                } catch (NullPointerException e) {
                    // We get this on some versions of Android if there is no headset:
                    // http://stackoverflow.com/a/26914789/473672
                    Timber.d("Got NPE from AudioManager.startBluetoothSco() => no headset available");
                    return false;
                }
                audioManager.setBluetoothScoOn(true);
                requested = true;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Timber.w(e, "Interrupted waiting for Bluetooth SCO to get started");
                audioManager.setBluetoothScoOn(false);
                audioManager.stopBluetoothSco();
                return false;
            }
        }

        Timber.w("No response to trying to enable SCO audio, marking as failed");
        audioManager.setBluetoothScoOn(false);
        audioManager.stopBluetoothSco();
        return false;
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
            handleIntent(intent, false);
            return Service.START_NOT_STICKY;
        } else if (enableBluetoothSco(audioManager)) {
            Timber.d("Speaking, SCO enabled");
            handleIntent(intent, true);
            return Service.START_NOT_STICKY;
        } else if (isRunningOnEmulator()) {
            Timber.d("Speaking, running in emulator");
            handleIntent(intent, false);
            return Service.START_NOT_STICKY;
        } else {
            Timber.i("Not speaking; no headphones detected");
            return START_NOT_STICKY;
        }
    }

    private void handleIntent(Intent intent, boolean bluetoothSco) {
        String type = intent.getStringExtra(EXTRA_TYPE);
        if (TextUtils.isEmpty(type)) {
            Timber.e("Speak action with no type");
            return;
        }

        if (TYPE_SMS.equals(type)) {
            handleSmsIntent(intent, bluetoothSco);
        } else {
            Timber.w("Ignoring incoming intent of type %s", type);
            return;
        }
    }

    private void handleSmsIntent(Intent intent, boolean bluetoothSco) {
        CharSequence body = intent.getCharSequenceExtra(EXTRA_BODY);
        if (body == null) {
            Timber.e("Speak SMS intent with null body");
            return;
        }

        // It's OK for the sender to be null, we'll just say it's unknown
        CharSequence sender = intent.getCharSequenceExtra(EXTRA_SENDER);

        Optional<Locale> optionalLocale = identifyLanguage(body);

        Locale locale = optionalLocale.or(Locale.getDefault());
        TtsUtil.speak(this, toSmsAnnouncement(body, sender, optionalLocale), locale, bluetoothSco);
    }

    // From: http://stackoverflow.com/a/9475663/473672
    private Map<Integer, String> getStrings(Locale locale, int ... resourceIds) {
        Map<Integer, String> returnMe = new HashMap<>();

        Resources res = getResources();
        Configuration conf = res.getConfiguration();
        Locale savedLocale = conf.locale;
        conf.locale = locale;
        res.updateConfiguration(conf, null); // second arg null means don't change

        // retrieve resources from desired locale
        for (int resourceId: resourceIds) {
            returnMe.put(resourceId, res.getString(resourceId));
        }

        // restore original locale
        conf.locale = savedLocale;
        res.updateConfiguration(conf, null);

        return returnMe;
    }

    private CharSequence toSmsAnnouncement(
        CharSequence body, @Nullable CharSequence sender, Optional<Locale> optionalLocale)
    {
        Locale locale = optionalLocale.or(Locale.getDefault());
        Map<Integer, String> translations = getStrings(locale,
            R.string.unknown_sender,
            R.string.sms_from,
            R.string.unknown_language);

        if (TextUtils.isEmpty(sender)) {
            sender = translations.get(R.string.unknown_sender);
        } else {
            sender =
                LookupUtils.getNameForNumber(this, sender.toString())
                    .or(translations.get(R.string.unknown_sender));
        }

        return String.format(translations.get(R.string.sms_from),
            optionalLocale.isPresent() ? "" : translations.get(R.string.unknown_language),
            sender, body);
    }

    @NonNull
    private List<LanguageProfile> getLanguageProfiles() {
        List<LanguageProfile> languageProfiles = new LinkedList<>();
        LanguageProfileReader languageProfileReader = new LanguageProfileReader();

        for (String localeCode: LanguagesPreference.getValues(this)) {
            try {
                languageProfiles.add(languageProfileReader.readBuiltIn(LdLocale.fromString(localeCode)));
            } catch (IOException e) {
                @NonNls String message = "Failed to load configured language " + localeCode;
                Timber.e(new Exception(message), message);
            }
        }
        return languageProfiles;
    }

    private Optional<Locale> identifyLanguage(CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            return Optional.absent();
        }

        List<LanguageProfile> languageProfiles = getLanguageProfiles();
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
