package com.gmail.walles.johan.headsetharry.settings;

import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;

import com.gmail.walles.johan.headsetharry.R;

import org.jetbrains.annotations.NonNls;

public class SettingsFragment extends PreferenceFragment {
    @NonNls
    public static final String ACTIVE_LANGUAGES_LIST = "activeLanguagesList";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        Preference activeLanguagesPref = findPreference(ACTIVE_LANGUAGES_LIST);
        activeLanguagesPref.setSummary("Swedish, English");
        activeLanguagesPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(getActivity()).
                    setMessage("You should have been taken to a pick-languages activity").
                    setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).
                    create().
                    show();
                return true;
            }
        });
    }
}
