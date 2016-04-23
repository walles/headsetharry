package com.gmail.walles.johan.headsetharry;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.common.base.Optional;

import timber.log.Timber;

// This class was inspired by http://stackoverflow.com/a/11863152/473672
public class LookupUtils {
    /*
     * Returns contact's id
     */
    @Nullable
    private static String getContactId(ContentResolver resolver, String phoneNumber) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber));

        Cursor cursor = resolver.query(uri, new String[] {
            ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID }, null, null, null);
        if (cursor == null) {
            return null;
        }

        String contactId = null;
        if (cursor.moveToFirst()) {
            do {
                contactId = cursor.getString(cursor
                    .getColumnIndex(ContactsContract.PhoneLookup._ID));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return contactId;
    }

    /*
     * Returns contact's name
     */
    public static Optional<String> getNameForNumber(
        Context context, @Nullable final CharSequence phoneNumber)
    {
        if (TextUtils.isEmpty(phoneNumber)) {
            return Optional.absent();
        }

        ContentResolver resolver = context.getContentResolver();
        String contactId = getContactId(resolver, phoneNumber.toString());
        if (contactId == null) {
            Timber.i("Failed to look up ID for <%s>", phoneNumber);
            return Optional.absent();
        }

        String[] projection = new String[] { ContactsContract.Contacts.DISPLAY_NAME };
        Cursor cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI, projection,
            ContactsContract.Contacts._ID + "=?", new String[] { contactId }, null);
        if (cursor == null) {
            Timber.w("Failed to look up name for ID=<%s> and phone number=<%s>", contactId, phoneNumber);
            return Optional.absent();
        }

        String name = null;
        if (cursor.moveToFirst()) {
            name = cursor.getString(0);
        }
        cursor.close();

        if (TextUtils.isEmpty(name)) {
            Timber.w("Got empty name for phone number=<%s>", phoneNumber);
            return Optional.absent();
        }

        Timber.i("Looked up phone number <%s> as <%s>", phoneNumber, name);
        return Optional.of(name);
    }
}
