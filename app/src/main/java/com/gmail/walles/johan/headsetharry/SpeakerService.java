package com.gmail.walles.johan.headsetharry;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.gmail.walles.johan.headsetharry.handlers.MmsPresenter;
import com.gmail.walles.johan.headsetharry.handlers.SmsPresenter;
import com.gmail.walles.johan.headsetharry.handlers.WifiPresenter;

import org.jetbrains.annotations.NonNls;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import timber.log.Timber;

public class SpeakerService extends Service {
    @NonNls
    public static final String SPEAK_ACTION = "com.gmail.walles.johan.headsetharry.speak_action";

    @NonNls
    public static final String EXTRA_TYPE = "com.gmail.walles.johan.headsetharry.type";

    @Override
    public void onCreate() {
        super.onCreate();
        LoggingUtil.setUpLogging(this);
    }

    private static boolean isRunningOnEmulator() {
        // Inspired by
        // http://stackoverflow.com/questions/2799097/how-can-i-detect-when-an-android-application-is-running-in>
        if (Build.PRODUCT == null) {
            return false;
        }

        @NonNls Set<String> parts = new HashSet<>(Arrays.asList(Build.PRODUCT.split("_")));
        if (parts.size() == 0) {
            return false;
        }

        parts.remove("sdk");
        parts.remove("google");
        parts.remove("x86");
        parts.remove("phone");

        // If the build identifier contains only the above keywords in some order, then we're
        // in an emulator
        return parts.isEmpty();
    }

    private boolean enableBluetoothSco(AudioManager audioManager) {
        if (!audioManager.isBluetoothScoAvailableOffCall()) {
            Timber.d("Bluetooth SCO not available off call, not trying it");
            return false;
        }

        long t0 = System.currentTimeMillis();
        boolean requested = false;
        while (System.currentTimeMillis() - t0 < 3000) {
            IntentFilter filter = new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
            Intent intent = registerReceiver(null, filter);
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
        } else if (audioManager.isWiredHeadsetOn()) {
            // isWiredHeadsetOn() is deprecated because "This is not a valid indication that audio
            // playback is actually over the wired headset", but I've decided to keep using it until
            // somebody can demonstrate to me what concrete problems can arise from using it for
            // detecting where our speech will actually end up.
            Timber.d("Speaking, wired headphones connected");
            handleIntent(intent, false);
            return Service.START_NOT_STICKY;
        } else if (enableBluetoothSco(audioManager)) {
            Timber.d("Speaking, SCO enabled");
            handleIntent(intent, true);
            return Service.START_NOT_STICKY;
        } else if (isRunningOnEmulator()) {
            Timber.d("Speaking, running in emulator");
            handleIntent(intent, false);
            return Service.START_NOT_STICKY;
        } else {
            Timber.i("Not speaking; no headphones detected");
            return START_NOT_STICKY;
        }
    }

    private void handleIntent(Intent intent, boolean bluetoothSco) {
        String type = intent.getStringExtra(EXTRA_TYPE);
        if (TextUtils.isEmpty(type)) {
            Timber.e("Speak action with no type");
            return;
        }

        Presenter presenter;
        if (SmsPresenter.TYPE.equals(type)) {
            presenter = new SmsPresenter(this, intent);
        } else if (MmsPresenter.TYPE.equals(type)) {
            presenter = new MmsPresenter(this, intent);
        } else if (WifiPresenter.TYPE.equals(type)) {
            presenter = new WifiPresenter(this);
        } else {
            Timber.w("Ignoring incoming intent of type %s", type);
            return;
        }

        TtsUtil.speak(this,
            presenter.getAnnouncement(),
            presenter.getAnnouncementLocale(),
            bluetoothSco);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Binding unsupported, please use startService() instead");
    }
}
