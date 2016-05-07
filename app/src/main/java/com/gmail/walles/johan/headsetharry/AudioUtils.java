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
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class AudioUtils {
    // With A2DP enabled, STREAM_MUSIC goes to the headset only. STREAM_NOTIFICATION goes to
    // the phone's speaker as well, and we don't want that.
    private int audioManagerStream = AudioManager.STREAM_MUSIC;
    private boolean weEnabledBluetoothSco = false;

    private final Context context;

    private AudioUtils(Context context) {
        this.context = context;
    }

    /**
     * Try to say something over a connected headset.
     * @return true if a headset was connected, false otherwise
     */
    private boolean speakOverHeadset(
        List<TextWithLocale> announcement, final TtsUtils.CompletionListener completionListener)
    {
        if (!setUpHeadset()) {
            return false;
        }

        TtsUtils.speak(context, announcement, audioManagerStream, new TtsUtils.CompletionListener() {
            @Override
            public void onSuccess() {
                if (weEnabledBluetoothSco) {
                    stopBluetoothSco();
                }
                completionListener.onSuccess();
            }

            @Override
            public void onFailure(@Nullable Locale locale, @NonNls String errorMessage) {
                if (weEnabledBluetoothSco) {
                    stopBluetoothSco();
                }
                completionListener.onFailure(locale, errorMessage);
            }
        });

        return true;
    }

    private boolean setUpHeadset() {
        AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        int audioMode = audioManager.getMode();
        if (audioManager.getMode() != AudioManager.MODE_NORMAL) {
            Timber.i("Not speaking, audio mode not MODE_NORMAL: %d", audioMode);
            return false;
        }

        if (audioManager.isBluetoothA2dpOn()) {
            Timber.d("Speaking, A2DP enabled");
            return true;
        }

        //noinspection deprecation
        if (audioManager.isWiredHeadsetOn()) {
            // isWiredHeadsetOn() is deprecated because "This is not a valid indication that audio
            // playback is actually over the wired headset", but I've decided to keep using it until
            // somebody can demonstrate to me what concrete problems can arise from using it for
            // detecting where our speech will end up.
            Timber.d("Speaking, wired headphones connected");
            return true;
        }

        if (enableBluetoothSco(context)) {
            audioManagerStream = AudioManager.STREAM_VOICE_CALL;
            Timber.d("Speaking, SCO enabled");
            return true;
        }

        if (EmulatorUtils.isRunningOnEmulator()) {
            Timber.d("Speaking, running in emulator");
            return true;
        }

        Timber.i("Not speaking; no headphones detected");
        return false;
    }

    private void stopBluetoothSco() {
        AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            @NonNls String status = audioManager.isBluetoothScoOn() ? "enabled": "disabled";
            Timber.d("Disabling SCO, was %s", status);
            audioManager.setBluetoothScoOn(false);
            audioManager.stopBluetoothSco();
        }
    }

    /**
     * Enable audio over Bluetooth SCO
     * @return True if we Bluetooth SCO is now enabled, false otherwise.
     */
    private boolean enableBluetoothSco(Context context) {
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
                    // Was it by our request or anyway?
                    weEnabledBluetoothSco = requested;
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

    /**
     * Try to say something over a connected headset.
     *
     * @return true if a headset was connected, false otherwise
     */
    public static boolean speakOverHeadset(
        Context context,
        List<TextWithLocale> announcement,
        final TtsUtils.CompletionListener completionListener)
    {
        return new AudioUtils(context).speakOverHeadset(announcement, completionListener);
    }
}
