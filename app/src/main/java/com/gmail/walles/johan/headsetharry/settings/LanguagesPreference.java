package com.gmail.walles.johan.headsetharry.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.MultiSelectListPreference;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;

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

public class LanguagesPreference
    extends MultiSelectListPreference
    implements SharedPreferences.OnSharedPreferenceChangeListener
{
    @NonNls
    private static final String ACTIVE_LANGUAGES_PREFERENCE = "activeLanguagesList";

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
            summary = summary.substring(0, 1).toUpperCase() + summary.substring(1);

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

            return Optional.of(ldLocale.toString());
        }

        return Optional.absent();
    }
}
