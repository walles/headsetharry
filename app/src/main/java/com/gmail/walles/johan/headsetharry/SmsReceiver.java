package com.gmail.walles.johan.headsetharry;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import com.google.common.base.Optional;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

/**
 * Receive incoming SMSes.
 */
public class SmsReceiver extends BroadcastReceiver {
    private class LanguageDetectionException extends Exception {
        public LanguageDetectionException(String message) {
            super(message);
        }
        public LanguageDetectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }

        final Object[] pduObjs = (Object[]) bundle.get("pdus");
        if (pduObjs == null) {
            Timber.w("Got SMS with no PDUs");
            return;
        }

        for (Object pduObj : pduObjs) {
            SmsMessage message = SmsMessage.createFromPdu((byte[])pduObj);
            Locale locale;
            try {
                locale = getLocale(message);
            } catch (LanguageDetectionException e) {
                Timber.e(e, "Language detection failed");
                continue;
            }
            CharSequence announcement = toString(message, locale);
            SpeakerService.speak(context, announcement, locale);
        }
    }

    private Locale getLocale(SmsMessage message) throws LanguageDetectionException {
        List<LanguageProfile> languageProfiles = new LinkedList<>();
        LanguageProfileReader languageProfileReader = new LanguageProfileReader();

        // FIXME: Use the locales for which there are voices present on the system
        try {
            languageProfiles.add(languageProfileReader.readBuiltIn(LdLocale.fromString("en")));
            languageProfiles.add(languageProfileReader.readBuiltIn(LdLocale.fromString("sv")));
        } catch (IOException e) {
            throw new LanguageDetectionException("Loading language profiles failed", e);
        }

        LanguageDetector languageDetector =
            LanguageDetectorBuilder.create(NgramExtractors.standard())
            .withProfiles(languageProfiles)
            .build();
        Optional<LdLocale> ldLocale = languageDetector.detect(message.getDisplayMessageBody());

        if (!ldLocale.isPresent()) {
            throw new LanguageDetectionException(
                "Unable to detect language for: <" + message.getDisplayMessageBody() + ">");
        }

        return new Locale.Builder().setLanguageTag(ldLocale.get().getLanguage()).build();
    }

    private CharSequence toString(SmsMessage message, Locale locale) {
        // FIXME: Get an SMS message template for the given locale and fill that in.

        // FIXME: What if we don't have an SMS message template for the given locale?

        String source = message.getDisplayOriginatingAddress();
        if (source == null) {
            source = "unknown sender";
        }
        return String.format("SMS from %s: %s", source, message.getDisplayMessageBody());
    }
}
