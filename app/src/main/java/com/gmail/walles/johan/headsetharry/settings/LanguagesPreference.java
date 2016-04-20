package com.gmail.walles.johan.headsetharry.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.MultiSelectListPreference;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.profiles.BuiltInLanguages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class LanguagesPreference
    extends MultiSelectListPreference
    implements SharedPreferences.OnSharedPreferenceChangeListener
{
    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        super.onSetInitialValue(restoreValue, defaultValue);

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
        List<String> values = new ArrayList<>(getValues());

        List<String> names = new ArrayList<>(values.size());
        for (String value: values) {
            names.add(parseLocale(value).getDisplayName());
        }
        Collections.sort(names);

        if (names.isEmpty()) {
            setSummary("-");
        } else {
            setSummary(TextUtils.join(", ", names));
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

    private Locale parseLocale(String string) {
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
}
