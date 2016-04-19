package com.gmail.walles.johan.headsetharry.settings;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

public class LanguagesPreference extends Preference {
    public LanguagesPreference(final Context context, AttributeSet attributes) {
        super(context, attributes);

        // FIXME: Subscribe to changes in our preference

        // FIXME: Set summary from the current preference value
        setSummary("Swedish, English");
    }

    @Override
    protected void onClick() {
        LanguagesPickerActivity.start(getContext());
    }
}
