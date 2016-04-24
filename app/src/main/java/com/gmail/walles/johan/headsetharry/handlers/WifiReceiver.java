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

        NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if (ConnectivityManager.TYPE_WIFI != netInfo.getType ()) {
            Timber.d("WifiReceiver got non-Wifi event %d", netInfo.getType());
            return;
        }

        WifiPresenter.speakStatus(context);
    }
}
