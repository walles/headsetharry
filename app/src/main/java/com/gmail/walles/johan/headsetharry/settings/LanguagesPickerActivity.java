package com.gmail.walles.johan.headsetharry.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.gmail.walles.johan.headsetharry.R;

public class LanguagesPickerActivity extends AppCompatActivity {
    public static void start(Context context) {
        context.startActivity(new Intent(context, LanguagesPickerActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_languages_picker);
    }
}
