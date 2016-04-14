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

            Optional<Locale> optionalLocale = Optional.absent();
            try {
                optionalLocale = getLocale(message);
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
            CharSequence announcement = toString(message, optionalLocale);
            SpeakerService.speak(context, announcement, locale);
        }
    }

    private Optional<Locale> getLocale(SmsMessage message) throws IOException {
        List<LanguageProfile> languageProfiles = new LinkedList<>();
        LanguageProfileReader languageProfileReader = new LanguageProfileReader();

        // FIXME: Use the locales for which there are voices present on the system
        languageProfiles.add(languageProfileReader.readBuiltIn(LdLocale.fromString("en")));
        languageProfiles.add(languageProfileReader.readBuiltIn(LdLocale.fromString("sv")));

        LanguageDetector languageDetector =
            LanguageDetectorBuilder.create(NgramExtractors.standard())
            .withProfiles(languageProfiles)
            .build();
        Optional<LdLocale> ldLocale = languageDetector.detect(message.getDisplayMessageBody());

        if (!ldLocale.isPresent()) {
            String languages = "";
            for (LanguageProfile languageProfile: languageProfiles) {
                if (languages.length() > 0) {
                    languages += ",";
                }
                languages += languageProfile.getLocale();
            }
            Timber.w("Unable to detect language among <%s> for: <%s>",
                    languages,
                    message.getDisplayMessageBody());
            return Optional.absent();
        }

        return Optional.of(new Locale.Builder().setLanguageTag(ldLocale.get().getLanguage()).build());
    }

    private CharSequence toString(SmsMessage message, Optional<Locale> locale) {
        // FIXME: Get an SMS message template for the given locale and fill that in.

        // FIXME: What if we don't have an SMS message template for the given locale?

        // FIXME: Localize all strings in this method

        String source = message.getDisplayOriginatingAddress();
        if (source == null) {
            source = "unknown sender";
        }

        return String.format("%sSMS from %s: %s",
            locale.isPresent() ? "" : "Unknown language ",
            source,
            message.getDisplayMessageBody());
    }
}
