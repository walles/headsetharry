/*
 * Copyright 2016 Johan Walles <johan.walles@gmail.com>
 *
 * This file is part of Headset Harry.
 *
 * Headset Harry is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Headset Harry is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gmail.walles.johan.headsetharry.settings;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.net.Uri;
import android.preference.MultiSelectListPreference;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.Toast;

import com.gmail.walles.johan.headsetharry.TtsUtil;
import com.google.common.base.Optional;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.profiles.BuiltInLanguages;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import timber.log.Timber;

public class LanguagesPreference
    extends MultiSelectListPreference
    implements SharedPreferences.OnSharedPreferenceChangeListener
{
    @NonNls
    private static final String ACTIVE_LANGUAGES_PREFERENCE = "activeLanguagesList";
    @NonNls
    private static final String PLAY_STORE_SEARCH_TTS_URL = "market://search?q=TTS&c=apps";
    @NonNls
    private static final String ACTION_ANDROID_TTS_SETTINGS = "com.android.settings.TTS_SETTINGS";

    @Override
    public Set<String> getValues() {
        throw new UnsupportedOperationException("Call static method getValues(Context) instead");
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        // We don't care what anybody has written in their XML, default to the system language if
        // possible.
        return getValues(getContext());
    }

    @Override
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();

        getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        updateSummary();
    }

    public LanguagesPreference(final Context context, AttributeSet attributes) {
        super(context, attributes);

        // FIXME: Should we rather do this right before the dialog pops up?
        populateList();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (!TextUtils.equals(key, getKey())) {
            return;
        }

        updateSummary();

        testSpeakConfiguredLanguages();
    }

    private void testSpeakConfiguredLanguages() {
        Set<String> configuredLanguageNames = getValues(getContext());
        List<Locale> locales = new ArrayList<>(configuredLanguageNames.size());
        for (String languageName: configuredLanguageNames) {
            locales.add(parseLocale(languageName));
        }
        TtsUtil.testSpeakLocales(getContext(), locales, new TtsUtil.TestFailureListener() {
            @Override
            public void onTestSpeakLocaleFailed(Locale locale) {
                new AlertDialog.Builder(getContext()).
                    setTitle("Can't speak " + locale.getDisplayName()).
                    setMessage(String.format(
                        "No Text-to-speech support for %s.%n" +
                        "%n" +
                        "You can either try to get a new TTS engine from Google Play Store " +
                        "or try to configure your existing TTS engines.",
                        locale.getDisplayName())).
                    setPositiveButton("Install TTS", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // FIXME: Launch Google Play with a pre-filled search
                            Intent goToMarket = new Intent(Intent.ACTION_VIEW);
                            goToMarket.setData(Uri.parse(PLAY_STORE_SEARCH_TTS_URL));
                            try {
                                getContext().startActivity(goToMarket);
                            } catch (ActivityNotFoundException e) {
                                Timber.w(e, "Google Play Store not available");
                                Toast.makeText(getContext(),
                                    "Google Play Store not available on this device",
                                    Toast.LENGTH_SHORT).show();
                            }
                        }
                    }).
                    setNeutralButton("System TTS Settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Launch System TTS Settings, from http://stackoverflow.com/a/8688354/473672
                            Intent openSystemTtsSettings = new Intent();
                            openSystemTtsSettings.setAction(ACTION_ANDROID_TTS_SETTINGS);
                            try {
                                getContext().startActivity(openSystemTtsSettings);
                            } catch (ActivityNotFoundException e) {
                                Timber.w(e, "System TTS Settings not available");
                                Toast.makeText(getContext(),
                                    "System TTS Settings not available",
                                    Toast.LENGTH_SHORT).show();
                            }
                        }
                    }).
                    setCancelable(true).
                    show();
            }
        }, false);
    }

    private void updateSummary() {
        Set<String> values = getValues(getContext());
        if (!values.equals(super.getValues())) {
            // Keep MultiSelectListPreference's internal cache in sync with reality. Without this
            // line here, if you clear your app data and then click on the language selection
            // preference, the resulting list has no selections.
            setValues(values);
        }

        List<String> names = new ArrayList<>(values.size());
        for (String value: values) {
            names.add(parseLocale(value).getDisplayName());
        }
        Collections.sort(names);

        if (names.isEmpty()) {
            setSummary("-");
        } else {
            String summary = TextUtils.join(", ", names);

            // Capitalize first character, purely for looks
            summary = summary.substring(0, 1).toUpperCase(Locale.getDefault()) + summary.substring(1);

            setSummary(summary);
        }
    }

    private void populateList() {
        // List the languages supported by our language detector
        List<LdLocale> languages = BuiltInLanguages.getLanguages();
        LdLocale available[] = languages.toArray(new LdLocale[languages.size()]);

        Arrays.sort(available, new Comparator<LdLocale>() {
            @Override
            public int compare(LdLocale lhs, LdLocale rhs) {
                String l0 = new Locale(lhs.getLanguage()).getDisplayName();
                String l1 = new Locale(rhs.getLanguage()).getDisplayName();
                return l0.compareTo(l1);
            }
        });

        CharSequence entries[] = new CharSequence[available.length];
        CharSequence values[] = new CharSequence[available.length];
        for (int i = 0; i < available.length; i++) {
            LdLocale current = available[i];
            entries[i] = parseLocale(current.toString()).getDisplayName();
            values[i] = current.toString();
        }

        setEntries(entries);
        setEntryValues(values);
    }

    private static Locale parseLocale(String string) {
        String parts[] = string.split("_", -1);
        if (parts.length == 1) {
            parts = string.split("-", -1);
        }

        if (parts.length == 1) {
            return new Locale(parts[0]);
        }

        if (parts.length == 2
            || (parts.length == 3 && parts[2].startsWith("#"))) {
            return new Locale(parts[0], parts[1]);
        }

        if (parts.length == 3) {
            return new Locale(parts[0], parts[1], parts[2]);
        }

        throw new IllegalArgumentException("Failed to parse locale string: <" + string + ">");
    }

    /**
     * Return the set of configured languages, defaulting to the system language if the list is
     * empty.
     * <p/>
     * As a side effect, this method updates the persisted preferences if it had to go for the
     * default.
     */
    public static Set<String> getValues(Context context) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> localeCodes =
            preferences.
                getStringSet(LanguagesPreference.ACTIVE_LANGUAGES_PREFERENCE, new HashSet<String>());

        if (!localeCodes.isEmpty()) {
            return localeCodes;
        }

        Optional<String> defaultLocaleCode = getDetectableLocaleCode(Locale.getDefault());
        if (!defaultLocaleCode.isPresent()) {
            return localeCodes;
        }

        localeCodes.add(defaultLocaleCode.get());
        preferences.edit().putStringSet(ACTIVE_LANGUAGES_PREFERENCE, localeCodes).apply();

        return localeCodes;
    }

    /**
     * Given a locale, find a locale string supported by our language detection that is as close to
     * that locale as possible.
     */
    static Optional<String> getDetectableLocaleCode(Locale locale) {
        for (LdLocale ldLocale: BuiltInLanguages.getLanguages()) {
            if (!ldLocale.getLanguage().equals(locale.getLanguage())) {
                // Language doesn't match
                continue;
            }

            if (locale.getCountry().length() == 0 && ldLocale.getRegion().isPresent()) {
                // Supported locale has region, query has not
                continue;
            }

            if (ldLocale.getRegion().isPresent() && !ldLocale.getRegion().get().equals(locale.getCountry())) {
                // Regions don't match
                continue;
            }

            return Optional.of(ldLocale.toString()); //NOPMD
        }

        return Optional.absent();
    }
}
