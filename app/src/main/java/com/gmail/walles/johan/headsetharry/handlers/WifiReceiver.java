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
