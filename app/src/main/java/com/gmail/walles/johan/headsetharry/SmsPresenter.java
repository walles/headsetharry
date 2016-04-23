package com.gmail.walles.johan.headsetharry;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class SmsPresenter {
    @NonNls
    private static final String EXTRA_BODY = "com.gmail.walles.johan.headsetharry.body";
    @NonNls
    private static final String EXTRA_SENDER = "com.gmail.walles.johan.headsetharry.sender";

    @NonNls
    public static final String TYPE = "SMS";

    private final Context context;
    private final Optional<Locale> locale;
    private final String announcement;

    public static void speak(Context context, CharSequence body, CharSequence sender) {
        Intent intent = new Intent(context, SpeakerService.class);
        intent.setAction(SpeakerService.SPEAK_ACTION);
        intent.putExtra(SpeakerService.EXTRA_TYPE, TYPE);
        intent.putExtra(EXTRA_BODY, body);
        intent.putExtra(EXTRA_SENDER, sender);
        context.startService(intent);
    }

    public Locale getAnnouncementLocale() {
        return locale.or(Locale.getDefault());
    }

    public String getAnnouncement() {
        return announcement;
    }

    public SmsPresenter(Context context, Intent intent) {
        this.context = context;

        CharSequence body = intent.getCharSequenceExtra(EXTRA_BODY);
        if (body == null) {
            body = "";
        }

        // It's OK for the sender to be null, we'll just say it's unknown
        CharSequence sender = intent.getCharSequenceExtra(EXTRA_SENDER);
        locale = identifyLanguage(body);

        announcement = createAnnouncement(body, sender);
    }

    // From: http://stackoverflow.com/a/9475663/473672
    private Map<Integer, String> getStrings(Locale locale, int ... resourceIds) {
        Map<Integer, String> returnMe = new HashMap<>();

        Resources res = context.getResources();
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

    private String createAnnouncement(CharSequence body, @Nullable CharSequence sender)
    {
        Map<Integer, String> translations = getStrings(getAnnouncementLocale(),
            R.string.sms,
            R.string.empty_sms,
            R.string.unknown_language_sms,
            R.string.unknown_sender,
            R.string.what_from_where_colon_body);

        if (TextUtils.isEmpty(sender)) {
            sender = translations.get(R.string.unknown_sender);
        } else {
            sender =
                LookupUtils.getNameForNumber(context, sender.toString())
                    .or(translations.get(R.string.unknown_sender));
        }

        if (TextUtils.isEmpty(body)) {
            return String.format(translations.get(R.string.what_from_where_colon_body),
                translations.get(R.string.empty_sms), sender, body);
        }

        String sms;
        if (locale.isPresent()) {
            sms = translations.get(R.string.sms);
        } else {
            sms = translations.get(R.string.unknown_language_sms);
        }
        return String.format(translations.get(R.string.what_from_where_colon_body), sms, sender, body);
    }

    @NonNull
    private List<LanguageProfile> getLanguageProfiles() {
        List<LanguageProfile> languageProfiles = new LinkedList<>();
        LanguageProfileReader languageProfileReader = new LanguageProfileReader();

        for (String localeCode: LanguagesPreference.getValues(context)) {
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
}
