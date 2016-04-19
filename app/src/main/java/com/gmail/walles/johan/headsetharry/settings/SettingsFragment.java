package com.gmail.walles.johan.headsetharry.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.gmail.walles.johan.headsetharry.R;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }
}
