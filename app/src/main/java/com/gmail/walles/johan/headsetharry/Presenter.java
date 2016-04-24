package com.gmail.walles.johan.headsetharry;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.annotation.NonNull;
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

public abstract class Presenter {
    public abstract Locale getAnnouncementLocale();

    public abstract String getAnnouncement();

    protected final Context context;

    protected Presenter(Context context) {
        this.context = context;
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

    protected Optional<Locale> identifyLanguage(CharSequence text) {
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

    // From: http://stackoverflow.com/a/9475663/473672
    protected Map<Integer, String> getStringsForLocale(Locale locale, int ... resourceIds) {
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
}
