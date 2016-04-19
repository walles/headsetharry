package com.gmail.walles.johan.headsetharry.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.Preference;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;

public class LanguagesPreference extends Preference {
    public LanguagesPreference(final Context context, AttributeSet attributes) {
        super(context, attributes);

        setSummary("Swedish, English");

        setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(context).
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
