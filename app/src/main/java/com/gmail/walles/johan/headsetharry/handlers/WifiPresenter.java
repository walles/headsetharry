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

    @NonNls
    private final static String EXTRA_CONNECTED = "com.gmail.walles.johan.headsetharry.connected";

    private final TextWithLocale announcement;

    /**
     * Speak current WiFi connectivity status.
     */
    public static void speakStatus(Context context, boolean connected) {
        Intent intent = new Intent(context, SpeakerService.class);
        intent.setAction(SpeakerService.SPEAK_ACTION);
        intent.putExtra(SpeakerService.EXTRA_TYPE, TYPE);
        intent.putExtra(EXTRA_CONNECTED, connected);
        context.startService(intent);
    }

    @Override
    public List<TextWithLocale> getAnnouncement() {
        return announcement.toList();
    }

    public WifiPresenter(Context context, Intent intent) {
        super(context);

        if (!intent.hasExtra(EXTRA_CONNECTED)) {
            throw new IllegalArgumentException("Wifi intent without connected status");
        }
        boolean connected = intent.getBooleanExtra(EXTRA_CONNECTED, false);

        if (!connected) {
            announcement = new TextWithLocale(
                Locale.getDefault(), context.getString(R.string.wifi_disconnected));
            return;
        }

        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        @NonNls String ssid = wifiInfo.getSSID();

        // Explanation of the "<unknown ssid>" magic constant:
        // http://developer.android.com/reference/android/net/wifi/WifiInfo.html#getSSID()
        if ("<unknown ssid>".equals(ssid)) {
            @NonNls String problem = "Got Wifi-connected event but wifi seems disconnected";
            Timber.w(new Exception(problem), problem);
            announcement = new TextWithLocale(
                Locale.getDefault(), context.getString(R.string.wifi_disconnected));
            return;
        }

        // Trim surrounding double quotes if applicable
        ssid = spacify(ssid.replaceAll("^\"|\"$", "")).toString();
        if (TextUtils.isEmpty(ssid)) {
            @NonNls String problem = "Got empty SSID when supposedly connected";
            Timber.w(new Exception(problem), problem);
            announcement = new TextWithLocale(
                Locale.getDefault(), context.getString(R.string.connected_to_unnamed_network));
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

    private TextWithLocale createWifiAnnouncement(String ssid) {
        Locale locale = identifyLanguage(ssid).or(Locale.getDefault());
        Translations translations = new Translations(context, locale,
            R.string.connected_to_networkname);
        return TextWithLocale.format(locale,
            translations.getString(R.string.connected_to_networkname), ssid);
    }
}
