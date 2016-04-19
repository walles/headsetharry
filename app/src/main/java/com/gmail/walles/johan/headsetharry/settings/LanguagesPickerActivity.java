package com.gmail.walles.johan.headsetharry.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;

import com.gmail.walles.johan.headsetharry.R;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.profiles.BuiltInLanguages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class LanguagesPickerActivity extends AppCompatActivity {
    public static void start(Context context) {
        context.startActivity(new Intent(context, LanguagesPickerActivity.class));
    }

    private static class CheckableLocale implements Comparable<CheckableLocale> {
        public Locale locale;
        public boolean checked;

        public CheckableLocale(Locale locale, boolean checked) {
            this.locale = locale;
            this.checked = checked;
        }

        @Override
        public int compareTo(@NonNull CheckableLocale other) {
            return locale.getDisplayName().compareTo((other).locale.getDisplayName());
        }
    }

    private static class CheckableLocaleAdapter extends ArrayAdapter<CheckableLocale> {
        private List<CheckableLocale> checkableLocales;

        public CheckableLocaleAdapter(Context context, List<CheckableLocale> countryList) {
            super(context, R.layout.checkable_string, countryList);
            this.checkableLocales = new ArrayList<>();
            this.checkableLocales.addAll(countryList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            CheckBox checkBox;

            if (convertView == null) {
                LayoutInflater inflater =
                    (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.checkable_string, parent, false);

                checkBox = (CheckBox)convertView.findViewById(R.id.checkable_string_checkbox);
                convertView.setTag(checkBox);

                checkBox.setOnClickListener( new View.OnClickListener() {
                    public void onClick(View v) {
                        CheckBox checkBox = (CheckBox)v;
                        CheckableLocale checkableLocale = (CheckableLocale)checkBox.getTag();
                        checkableLocale.checked = checkBox.isChecked();
                    }
                });
            }
            else {
                checkBox = (CheckBox)convertView.getTag();
            }

            CheckableLocale checkableLocale = checkableLocales.get(position);
            checkBox.setText(checkableLocale.locale.getDisplayName());
            checkBox.setChecked(checkableLocale.checked);
            checkBox.setTag(checkableLocale);

            return convertView;
        }
    }

    private void populateList() {
        // List the languages supported by our language detector
        List<LdLocale> available = BuiltInLanguages.getLanguages();
        List<CheckableLocale> listed = new ArrayList<>(available.size());
        for (LdLocale locale: available) {
            listed.add(new CheckableLocale(new Locale(locale.toString()), false));
        }
        Collections.sort(listed);

        ListView listView = (ListView)findViewById(R.id.languagePickerListView);
        if (listView == null) {
            throw new RuntimeException("listView not found");
        }

        CheckableLocaleAdapter adapter =
            new CheckableLocaleAdapter(this, listed);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_languages_picker);

        // FIXME: Add a back arrow to the top of the languages picker activity

        populateList();
    }
}
