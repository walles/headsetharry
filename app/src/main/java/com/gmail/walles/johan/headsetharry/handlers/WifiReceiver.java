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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import com.gmail.walles.johan.headsetharry.LoggingUtil;

import java.util.Locale;

import timber.log.Timber;

public class WifiReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        LoggingUtil.setUpLogging(context);

        NetworkInfo networkInfo =
            intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if (networkInfo == null) {
            Timber.w("Got null NetworkInfo from action <%s>", intent.getAction());
            return;
        }

        @SuppressWarnings("HardCodedStringLiteral")
        String background =
            String.format(Locale.ENGLISH, "action=<%s> type=<%s> detailed state=<%s>",
                intent.getAction(), intent.getType(), networkInfo.getDetailedState());

        // These if statements and this functionality is heavily inspired by
        // http://stackoverflow.com/a/5890104/473672
        if(WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            if(networkInfo.isConnected()) {
                Timber.i("WifiReceiver speaking status for: %s", background);
                WifiPresenter.speakStatus(context);
                return;
            }
        } else if(ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            if(networkInfo.getType() == ConnectivityManager.TYPE_WIFI &&
                networkInfo.getDetailedState() == NetworkInfo.DetailedState.DISCONNECTED)
            {
                Timber.i("WifiReceiver speaking status for: %s", background);
                WifiPresenter.speakStatus(context);
                return;
            }
        } else {
            Timber.w("WifiReceiver got unexpected action %s", intent.getAction());
        }

        Timber.i("WifiReceiver not speaking status for: %s", background);
    }
}
