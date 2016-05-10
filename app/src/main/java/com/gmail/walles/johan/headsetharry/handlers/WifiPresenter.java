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
 * along with Headset Harry.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gmail.walles.johan.headsetharry.handlers;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.gmail.walles.johan.headsetharry.R;
import com.gmail.walles.johan.headsetharry.SpeakerService;
import com.gmail.walles.johan.headsetharry.TextWithLocale;
import com.gmail.walles.johan.headsetharry.Translations;

import org.jetbrains.annotations.NonNls;

import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class WifiPresenter extends Presenter {
    @NonNls
    public final static String TYPE = "WiFi";

    private final List<TextWithLocale> announcement;

    /**
     * Speak current WiFi connectivity status.
     */
    public static void speakStatus(Context context) {
        Intent intent = new Intent(context, SpeakerService.class);
        intent.setAction(SpeakerService.SPEAK_ACTION);
        intent.putExtra(SpeakerService.EXTRA_TYPE, TYPE);
        context.startService(intent);
    }

    @NonNull
    @Override
    public List<TextWithLocale> getAnnouncement() {
        return announcement;
    }

    public WifiPresenter(Context context) {
        super(context);

        Translations translations = new Translations(context, Locale.getDefault(),
            R.string.wifi_disconnected);

        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        @NonNls String ssid = wifiInfo.getSSID();

        if (TextUtils.isEmpty(ssid)) {
            // This is apparently how it used to work:
            // https://code.google.com/p/android/issues/detail?id=43336
            announcement = translations.format(R.string.wifi_disconnected);
            return;
        }

        // Explanation of the "<unknown ssid>" magic constant:
        // http://developer.android.com/reference/android/net/wifi/WifiInfo.html#getSSID()
        if ("<unknown ssid>".equals(ssid)) {
            announcement = translations.format(R.string.wifi_disconnected);
            return;
        }

        // Trim surrounding double quotes if applicable
        ssid = spacify(ssid.replaceAll("^\"|\"$", "")).toString();
        if (TextUtils.isEmpty(ssid)) {
            @NonNls String problem = "Got empty SSID when supposedly connected: " + ssid;
            Timber.w(new Exception(problem), "%s", problem);
            announcement = translations.format(R.string.wifi_disconnected);
            return;
        }

        announcement = createWifiAnnouncement(ssid);
    }

    /**
     * Insert spaces where the SSID goes from lowercase to uppercase or from letters to numbers.
     * Also replace dashes and underscores with spaces.
     */
    static CharSequence spacify(CharSequence ssid) {
        StringBuilder returnMe = new StringBuilder();
        char lastChar = '☺';
        for (int i = 0; i < ssid.length(); i++) {
            char currentChar = ssid.charAt(i);

            if (currentChar == '-' || currentChar == '_') {
                currentChar = ' ';
            }

            if (i == 0) {
                returnMe.append(currentChar);
                lastChar = currentChar;
                continue;
            }

            if (Character.isLowerCase(lastChar) && Character.isUpperCase(currentChar)) {
                // From lower case to upper case
                returnMe.append(' ');
            } else if (Character.isDigit(lastChar) && Character.isLetter(currentChar)) {
                // From digit to letter
                returnMe.append(' ');
            } else if (Character.isLetter(lastChar) && Character.isDigit(currentChar)) {
                // From letter to digit
                returnMe.append(' ');
            }

            returnMe.append(currentChar);
            lastChar = currentChar;
        }

        return returnMe;
    }

    private List<TextWithLocale> createWifiAnnouncement(String ssid) {
        Locale ssidLocale = identifyLanguage(ssid).or(Locale.getDefault());
        Translations translations = new Translations(context, ssidLocale,
            R.string.connected_to_networkname);
        return translations.format(R.string.connected_to_networkname, ssidLocale, ssid);
    }
}
