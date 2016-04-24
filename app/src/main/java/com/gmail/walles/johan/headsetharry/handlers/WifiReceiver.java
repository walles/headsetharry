package com.gmail.walles.johan.headsetharry.handlers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import com.gmail.walles.johan.headsetharry.LoggingUtil;

import timber.log.Timber;

public class WifiReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        LoggingUtil.setUpLogging(context);

        if (!WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            Timber.w("WifiReceiver got unexpected action <%s>", intent.getAction());
            return;
        }

        NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if (ConnectivityManager.TYPE_WIFI != networkInfo.getType()) {
            Timber.d("WifiReceiver got non-Wifi event %d", networkInfo.getType());
            return;
        }

        NetworkInfo.State state = networkInfo.getState();
        if (state != NetworkInfo.State.CONNECTED && state != NetworkInfo.State.DISCONNECTED) {
            Timber.d("WifiReceiver got non-final network state %s, ignoring", state);
            return;
        }

        WifiPresenter.speakStatus(context);
    }
}
