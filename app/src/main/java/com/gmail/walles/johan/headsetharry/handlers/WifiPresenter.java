package com.gmail.walles.johan.headsetharry.handlers;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.gmail.walles.johan.headsetharry.Presenter;
import com.gmail.walles.johan.headsetharry.R;
import com.gmail.walles.johan.headsetharry.SpeakerService;
import com.google.common.base.Optional;

import org.jetbrains.annotations.NonNls;

import java.util.Locale;
import java.util.Map;

public class WifiPresenter extends Presenter {
    @NonNls
    public final static String TYPE = "WiFi";

    private final Optional<Locale> locale;
    private final String announcement;

    /**
     * Speak current WiFi connectivity status.
     */
    public static void speakStatus(Context context) {
        Intent intent = new Intent(context, SpeakerService.class);
        intent.setAction(SpeakerService.SPEAK_ACTION);
        intent.putExtra(SpeakerService.EXTRA_TYPE, TYPE);
        context.startService(intent);
    }

    @Override
    public Locale getAnnouncementLocale() {
        return locale.or(Locale.getDefault());
    }

    @Override
    public String getAnnouncement() {
        return announcement;
    }

    public WifiPresenter(Context context) {
        super(context);

        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        @NonNls String ssid = wifiInfo.getSSID();

        // Explanation of the "<unknown ssid>" magic constant:
        // http://developer.android.com/reference/android/net/wifi/WifiInfo.html#getSSID()
        if ("<unknown ssid>".equals(ssid)) {
            locale = Optional.absent();
            announcement = context.getString(R.string.wifi_disconnected);
            return;
        }

        if (!ssid.startsWith("\"")) {
            locale = Optional.absent();
            announcement = context.getString(R.string.connected_to_unnamed_network);
            return;
        }

        // Trim surrounding double quotes
        ssid = spacify(ssid.replaceAll("^\"|\"$", "")).toString();

        locale = identifyLanguage(ssid);
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

    private String createWifiAnnouncement(String ssid) {
        Map<Integer, String> translations = getStringsForLocale(getAnnouncementLocale(),
            R.string.connected_to_networkname);
        return String.format(translations.get(R.string.connected_to_networkname), ssid);
    }
}