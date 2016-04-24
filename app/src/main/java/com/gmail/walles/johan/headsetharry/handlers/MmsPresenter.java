package com.gmail.walles.johan.headsetharry.handlers;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.gmail.walles.johan.headsetharry.LookupUtils;
import com.gmail.walles.johan.headsetharry.Presenter;
import com.gmail.walles.johan.headsetharry.R;
import com.gmail.walles.johan.headsetharry.SpeakerService;

import org.jetbrains.annotations.NonNls;

import java.util.Locale;

public class MmsPresenter extends Presenter {
    @NonNls
    private static final String EXTRA_SENDER = "com.gmail.walles.johan.headsetharry.sender";

    @NonNls
    public static final String TYPE = "MMS";

    private final String announcement;

    public static void speak(Context context, CharSequence sender) {
        Intent intent = new Intent(context, SpeakerService.class);
        intent.setAction(SpeakerService.SPEAK_ACTION);
        intent.putExtra(SpeakerService.EXTRA_TYPE, TYPE);
        intent.putExtra(EXTRA_SENDER, sender);
        context.startService(intent);
    }

    public MmsPresenter(Context context, Intent intent) {
        super(context);

        // It's OK for the sender to be null, we'll just say it's unknown
        CharSequence sender = intent.getCharSequenceExtra(EXTRA_SENDER);

        announcement = createAnnouncement(sender);
    }

    @Override
    public Locale getAnnouncementLocale() {
        return Locale.getDefault();
    }

    @Override
    public String getAnnouncement() {
        return announcement;
    }

    private String createAnnouncement(@Nullable CharSequence sender) {
        sender =
            LookupUtils.getNameForNumber(context, sender)
                .or(context.getString(R.string.unknown_sender));

        return String.format(context.getString(R.string.mms_from_x), sender);
    }
}
