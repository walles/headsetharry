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

package com.gmail.walles.johan.headsetharry;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;

import timber.log.Timber;

public class AudioUtils {
    private AudioUtils() {
        throw new UnsupportedOperationException("Utility class, please don't instantiate");
    }

    /**
     * Enable audio over Bluetooth SCO
     * @return True if we Bluetooth SCO is now enabled, false otherwise.
     */
    public static boolean enableBluetoothSco(Context context) {
        AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        if (!audioManager.isBluetoothScoAvailableOffCall()) {
            Timber.d("Bluetooth SCO not available off call, not trying it");
            return false;
        }

        long t0 = System.currentTimeMillis();
        boolean requested = false;
        while (System.currentTimeMillis() - t0 < 3000) {
            IntentFilter filter = new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
            Intent intent = context.registerReceiver(null, filter);
            if (intent == null) {
                Timber.w("Got null Intent when asking for ACTION_SCO_AUDIO_STATE_UPDATED");
                audioManager.setBluetoothScoOn(false);
                audioManager.stopBluetoothSco();
                return false;
            }

            int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_ERROR);
            if (state == AudioManager.SCO_AUDIO_STATE_ERROR) {
                Timber.w("Got error result when asking for ACTION_SCO_AUDIO_STATE_UPDATED");
                audioManager.setBluetoothScoOn(false);
                audioManager.stopBluetoothSco();
                return false;
            }

            if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                if (audioManager.isBluetoothScoOn()) {
                    Timber.d("Bluetooth SCO audio connected");
                    return true;
                }

                Timber.w("Bluetooth audio connected but not enabled, keep waiting...");
            }

            if (!requested) {
                Timber.v("Requesting Bluetooth SCO audio output");
                try {
                    // From: http://stackoverflow.com/a/17150250/473672
                    audioManager.startBluetoothSco();
                } catch (NullPointerException e) {
                    // We get this on some versions of Android if there is no headset:
                    // http://stackoverflow.com/a/26914789/473672
                    Timber.d("Got NPE from AudioManager.startBluetoothSco() => no headset available");
                    return false;
                }
                audioManager.setBluetoothScoOn(true);
                requested = true;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Timber.w(e, "Interrupted waiting for Bluetooth SCO to get started");
                audioManager.setBluetoothScoOn(false);
                audioManager.stopBluetoothSco();
                return false;
            }
        }

        Timber.w("No response to trying to enable SCO audio, marking as failed");
        audioManager.setBluetoothScoOn(false);
        audioManager.stopBluetoothSco();
        return false;
    }
}
