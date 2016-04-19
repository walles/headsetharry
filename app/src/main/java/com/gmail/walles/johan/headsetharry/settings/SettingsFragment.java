package com.gmail.walles.johan.headsetharry.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.gmail.walles.johan.headsetharry.R;

import org.jetbrains.annotations.NonNls;

public class SettingsFragment extends PreferenceFragment {
    @NonNls
    public static final String ACTIVE_LANGUAGES_LIST = "activeLanguagesList";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }
}
