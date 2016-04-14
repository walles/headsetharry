package com.gmail.walles.johan.headsetharry;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

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

    private void speak(CharSequence text, Locale locale) {
        Timber.e("Should have said in locale <%s>: <%s>", locale, text);
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
