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

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.text.SpannableString;
import android.text.TextUtils;

import com.gmail.walles.johan.headsetharry.R;
import com.gmail.walles.johan.headsetharry.SpeakerService;
import com.gmail.walles.johan.headsetharry.TextWithLocale;
import com.gmail.walles.johan.headsetharry.Translations;
import com.google.common.base.Optional;

import org.jetbrains.annotations.NonNls;

import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class EmailPresenter extends Presenter {
    @NonNls
    private static final String GOOGLE_INBOX_PACKAGE_NAME = "com.google.android.apps.inbox";

    private final CharSequence sender;
    private final CharSequence subject;
    private final CharSequence body;

    /**
     * Try to parse a Notification as an incoming e-mail announcement and if it is, speak it.
     *
     * @return true if we accepted it, false otherwise
     */
    @CheckResult
    public static boolean speak(Context context, StatusBarNotification statusBarNotification) {
        if (!GOOGLE_INBOX_PACKAGE_NAME.equals(statusBarNotification.getPackageName())) {
            // We support only Google Inbox for now
            return false;
        }

        /*
        Single e-mail announcement from Google Inbox:

        Incoming notification from com.google.android.apps.inbox with extras
        <Bundle[{android.title=Johan Walles,
           android.subText=johan.walles@gmail.com,
           android.template=android.app.Notification$BigTextStyle,
           android.showChronometer=false,
           android.icon=2130837710,
           android.text=Test subject,
           android.progress=0,
           android.progressMax=0,
           android.showWhen=true,
           android.rebuild.applicationInfo=ApplicationInfo{212fe30b com.google.android.apps.inbox},
           android.people=[Ljava.lang.String;@1e1335e8,
           android.largeIcon=android.graphics.Bitmap@20ad401,
           android.bigText=Test subject
                           Test body 3,
           android.infoText=null,
           android.wearable.EXTENSIONS=Bundle[mParcelledData.dataSize=308],
           android.originatingUserId=0,
           android.progressIndeterminate=false,
           android.summaryText=johan.walles@gmail.com}]>


        Multi e-mail announcement from Google Inbox:

        Incoming notification from com.google.android.apps.inbox with extras
        <Bundle[{android.title=2 nya meddelanden,
           android.textLines=[Ljava.lang.CharSequence;@166d2946,
           android.subText=null,
           android.template=android.app.Notification$InboxStyle,
           android.showChronometer=false,
           android.icon=2130837711,
           android.text=johan.walles@gmail.com,
           android.progress=0,
           android.progressMax=0,
           android.showWhen=true,
           android.rebuild.applicationInfo=ApplicationInfo{1321707 com.google.android.apps.inbox},
           android.people=[Ljava.lang.String;@1bc22034,
           android.infoText=2,
           android.originatingUserId=0,
           android.progressIndeterminate=false,
           android.summaryText=johan.walles@gmail.com}]>
        Text line <class android.text.SpannableString>: <Johan Walles   Test subject 2>
          Span 0-12, flags=0: android.text.style.TextAppearanceSpan@13afbe5d
        Text line <class android.text.SpannableString>: <Johan Walles   Test subject>
          Span 0-12, flags=0: android.text.style.TextAppearanceSpan@6c293d2


        Multi e-mail announcement from Google Inbox, with some message being announced by folder
        name only. Notice how the folder-only spans reach to the end of the multi-whitespace part,
        but the other spans go to the start of that region:

        Incoming notification from com.google.android.apps.inbox with extras
        <Bundle[{android.title=3 nya meddelanden,
                 android.textLines=[Ljava.lang.CharSequence;@1540cb2a,
                 android.subText=null,
                 android.template=android.app.Notification$InboxStyle,
                 android.showChronometer=false,
                 android.icon=2130837711,
                 android.text=johan.walles@gmail.com,
                 android.progress=0,
                 android.progressMax=0,
                 android.showWhen=true,
                 android.rebuild.applicationInfo=ApplicationInfo{16f97d1b com.google.android.apps.inbox},
                 android.people=[Ljava.lang.String;@2a3aaeb8,
                 android.infoText=3,
                 android.originatingUserId=0,
                 android.progressIndeterminate=false,
                 android.summaryText=johan.walles@gmail.com}]>
        Text line <class android.text.SpannableString>: <1 ny i Uppdateringar   Travis CI>
          Span 0-23, flags=0, style=0: android.text.style.TextAppearanceSpan@3688a491
        Text line <class android.text.SpannableString>: <Johan Walles   Testmeddelande på svenska>
          Span 0-12, flags=0, style=0: android.text.style.TextAppearanceSpan@33786ff6
        Text line <class android.text.SpannableString>: <Johan Walles   Test subject>
          Span 0-12, flags=0, style=0: android.text.style.TextAppearanceSpan@2aa8cff7
        Person: <mailto:walles@spotify.com>
        */

        Intent intent = new Intent(context, SpeakerService.class);
        intent.setAction(SpeakerService.SPEAK_ACTION);
        intent.putExtra(SpeakerService.EXTRA_TYPE, TYPE);

        Bundle extras = statusBarNotification.getNotification().extras;
        CharSequence body;
        CharSequence sender;
        CharSequence subject;

        boolean isSingle = extras.getCharSequence(Notification.EXTRA_INFO_TEXT) == null;

        if (isSingle) {
            // This is a single-message announcement
            sender = extras.getCharSequence(Notification.EXTRA_TITLE);
            subject = extras.getCharSequence(Notification.EXTRA_TEXT);

            // This will be available on some versions of Android; if it's not there though that's
            // fine as well.
            body = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT);
        } else {
            // Multi-message announcement
            CharSequence[] textLines =
                extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
            if (textLines == null) {
                throw new IllegalArgumentException("Got Inbox notification with null textLines");
            }
            SpannableString mostRecentLine = (SpannableString)textLines[0];

            Object[] spans = mostRecentLine.getSpans(0, mostRecentLine.length() - 1, Object.class);
            int firstSpanEnd = mostRecentLine.getSpanEnd(spans[0]);

            // See comment above with full dumps of bundle contents for where this test comes from
            boolean isSenderAndSubject = mostRecentLine.charAt(firstSpanEnd - 1) != ' ';
            if (isSenderAndSubject) {
                sender = mostRecentLine.subSequence(0, firstSpanEnd);
                subject = mostRecentLine.subSequence(firstSpanEnd, mostRecentLine.length());
            } else {
                sender = mostRecentLine.subSequence(firstSpanEnd, mostRecentLine.length());
                subject = null;
            }

            // Multi-e-mail announcements don't come with any bodies
            body = null;
        }

        intent.putExtra(EXTRA_SENDER, censorSender(sender));
        intent.putExtra(EXTRA_SUBJECT, subject);
        intent.putExtra(EXTRA_BODY, body);

        context.startService(intent);

        return true;
    }

    /**
     * If sender contains ": ", remove everything before and including that.
     * <p/>
     * The rationale is that Google Inbox sender is often prefixed with the bundle name, and we
     * don't want to read that as part of the sender.
     */
    static CharSequence censorSender(@Nullable CharSequence sender) {
        if (sender == null) {
            return null;
        }

        String senderString = sender.toString();
        int colonSpaceIndex = senderString.indexOf(": ");
        if (colonSpaceIndex == -1) {
            return sender;
        }

        return senderString.substring(colonSpaceIndex + ": ".length());
    }

    @NonNls
    public static final String TYPE = "Email";

    @NonNls
    private static final String EXTRA_SENDER = "com.gmail.walles.johan.headsetharry.sender";
    @NonNls
    private static final String EXTRA_SUBJECT = "com.gmail.walles.johan.headsetharry.subject";

    /**
     * The e-mail body is only used for language detection purposes.
     */
    @NonNls
    private static final String EXTRA_BODY = "com.gmail.walles.johan.headsetharry.body";

    public EmailPresenter(Context context, Intent intent) {
        super(context);

        sender = intent.getCharSequenceExtra(EXTRA_SENDER);
        if (TextUtils.isEmpty(sender)) {
            throw new IllegalArgumentException("Sender must not be empty: " + sender);
        }

        // We don't always get the subject from Google Inbox
        subject = intent.getCharSequenceExtra(EXTRA_SUBJECT);

        // It's OK for the body to be empty; we don't always get it and we don't need to present it
        body = intent.getCharSequenceExtra(EXTRA_BODY);
    }

    @NonNull
    @Override
    protected Optional<List<TextWithLocale>> createAnnouncement() {
        Optional<Locale> emailLocale = identifyLanguage(subject);
        boolean hasBody = !TextUtils.isEmpty(body);
        if (hasBody && !emailLocale.isPresent()) {
            emailLocale = identifyLanguage(body);
            if (emailLocale.isPresent()) {
                Timber.d("Email locale identified from body: %s", emailLocale.get());
            } else {
                Timber.d("Failed to identify email locale from body");
            }
        }

        Translations translations = new Translations(context, emailLocale.or(Locale.getDefault()),
            R.string.email_from_who_colon_subject,
            R.string.email_from_whom);
        if (TextUtils.isEmpty(subject)) {
            return Optional.of(translations.format(R.string.email_from_whom, sender));
        } else {
            return Optional.of(translations.format(R.string.email_from_who_colon_subject,
                sender, emailLocale.or(Locale.getDefault()), subject));
        }
    }

    @Override
    protected boolean isEnabled() {
        // FIXME: This is decided by whether we have Notifications access or not, maybe we should
        // FIXME: turn that into an actual preference for symmetry reasons?
        return true;
    }
}
