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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.gmail.walles.johan.headsetharry.handlers.EmailPresenter;
import com.gmail.walles.johan.headsetharry.handlers.MmsPresenter;
import com.gmail.walles.johan.headsetharry.handlers.Presenter;
import com.gmail.walles.johan.headsetharry.handlers.SmsPresenter;
import com.gmail.walles.johan.headsetharry.handlers.WifiPresenter;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.TestOnly;

import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class SpeakerService extends Service {
    @NonNls
    public static final String SPEAK_ACTION = "com.gmail.walles.johan.headsetharry.speak_action";

    @NonNls
    public static final String EXTRA_TYPE = "com.gmail.walles.johan.headsetharry.type";

    private long isDuplicateTimeoutMs = 10000;
    private List<TextWithLocale> duplicateBase;
    private long duplicateBaseTimestamp;

    @Override
    public void onCreate() {
        super.onCreate();
        LoggingUtils.setUpLogging(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!SPEAK_ACTION.equals(intent.getAction())) {
            Timber.w("Ignoring unsupported action <%s>", intent.getAction());
            return Service.START_NOT_STICKY;
        }

        AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int audioMode = audioManager.getMode();
        if (audioManager.getMode() != AudioManager.MODE_NORMAL) {
            Timber.i("Not speaking, audio mode not MODE_NORMAL: %d", audioMode);
            return START_NOT_STICKY;
        }

        if (audioManager.isBluetoothA2dpOn()) {
            Timber.d("Speaking, A2DP enabled");
            handleIntent(intent, false);
            return Service.START_NOT_STICKY;
        } else //noinspection deprecation
            if (audioManager.isWiredHeadsetOn())
        {
            // isWiredHeadsetOn() is deprecated because "This is not a valid indication that audio
            // playback is actually over the wired headset", but I've decided to keep using it until
            // somebody can demonstrate to me what concrete problems can arise from using it for
            // detecting where our speech will end up.
            Timber.d("Speaking, wired headphones connected");
            handleIntent(intent, false);
            return Service.START_NOT_STICKY;
        } else if (AudioUtils.enableBluetoothSco(this)) {
            Timber.d("Speaking, SCO enabled");
            handleIntent(intent, true);
            return Service.START_NOT_STICKY;
        } else if (EmulatorUtils.isRunningOnEmulator()) {
            Timber.d("Speaking, running in emulator");
            handleIntent(intent, false);
            return Service.START_NOT_STICKY;
        } else {
            Timber.i("Not speaking; no headphones detected");
            return START_NOT_STICKY;
        }
    }

    boolean isDuplicate(List<TextWithLocale> announcement) {
        if (!announcement.equals(duplicateBase)) {
            // Not the same message, start over
            duplicateBase = announcement;
            duplicateBaseTimestamp = System.currentTimeMillis();
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - duplicateBaseTimestamp > isDuplicateTimeoutMs) {
            // This is the same message, but our duplicate has timed out, start over
            duplicateBase = announcement;
            duplicateBaseTimestamp = System.currentTimeMillis();
            return false;
        }

        return true;
    }

    @TestOnly
    void setIsDuplicateTimeoutMs(long timeoutMillis) {
        if (timeoutMillis < 0) {
            throw new IllegalArgumentException("Timeout must be >= 0, was " + timeoutMillis);
        }
        isDuplicateTimeoutMs = timeoutMillis;
    }

    private void handleIntent(Intent intent, final boolean bluetoothSco) {
        String type = intent.getStringExtra(EXTRA_TYPE);
        if (TextUtils.isEmpty(type)) {
            Timber.e("Speak action with no type");
            return;
        }

        Presenter presenter;
        try {
            if (SmsPresenter.TYPE.equals(type)) {
                presenter = new SmsPresenter(this, intent);
            } else if (MmsPresenter.TYPE.equals(type)) {
                presenter = new MmsPresenter(this, intent);
            } else if (WifiPresenter.TYPE.equals(type)) {
                presenter = new WifiPresenter(this, intent);
                if (isDuplicate(presenter.getAnnouncement())) {
                    Timber.w("Ignoring duplicate Wifi announcement <%s>", presenter.getAnnouncement());
                    return;
                }
            } else if (EmailPresenter.TYPE.equals(type)) {
                presenter = new EmailPresenter(this, intent);
            } else {
                Timber.w("Ignoring incoming intent of type %s", type);
                return;
            }
        } catch (IllegalArgumentException e) {
            Timber.e(e, "Error parsing intent: %s", intent);
            return;
        }

        int audioManagerStream;
        if (bluetoothSco) {
            audioManagerStream = AudioManager.STREAM_VOICE_CALL;
        } else {
            // With A2DP enabled, STREAM_MUSIC goes to the headset only. STREAM_NOTIFICATION goes to
            // the phone's speaker as well, and we don't want that.
            audioManagerStream = AudioManager.STREAM_MUSIC;
        }

        TtsUtils.speak(this, presenter.getAnnouncement(), audioManagerStream,
            new TtsUtils.CompletionListener() {
                @Override
                public void onSuccess() {
                    if (bluetoothSco) {
                        stopBluetoothSco();
                    }
                }

                @Override
                public void onFailure(Locale locale, @NonNls String errorMessage) {
                    Timber.e(new Exception(errorMessage), "%s", errorMessage);

                    if (bluetoothSco) {
                        stopBluetoothSco();
                    }
                }
            });
    }

    private void stopBluetoothSco() {
        AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            @NonNls String status = audioManager.isBluetoothScoOn() ? "enabled": "disabled";
            Timber.d("Disabling SCO, was %s", status);
            audioManager.setBluetoothScoOn(false);
            audioManager.stopBluetoothSco();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Binding unsupported, please use startService() instead");
    }
}
