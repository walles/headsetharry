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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.io.FileNotFoundException;

import timber.log.Timber;

/**
 * Inspired by http://stephendnicholas.com/archives/974
 */
public class LogProvider extends ContentProvider {
    @NonNls
    public static final String AUTHORITY = "com.gmail.walles.johan.headsetharry";

    private static final int URI_CODE = 1;

    /** UriMatcher used to match against incoming requests */
    private UriMatcher uriMatcher;

    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context == null) {
            // According to the getContext() docs, the context is "Only available once onCreate()
            // has been called". According to the source code (API level 21), the context is
            // available inside of onCreate() as well. If that doesn't hold we want to know about it.
            throw new NullPointerException("getContext() returned null in LogProvider.onCreate()");
        }

        LoggingUtils.setUpLogging(context);

        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        // Add a URI to the matcher which will match against the form
        // 'content://com.gmail.walles.johan.headsetharry/logs.txt'
        // and return 1 in the case that the incoming Uri matches this pattern
        uriMatcher.addURI(AUTHORITY, "attachment.txt", URI_CODE); //NON-NLS

        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String ignoredMode)
            throws FileNotFoundException
    {
        Timber.v("Called with URI: '%s", uri);

        if (uriMatcher.match(uri) != URI_CODE) {
            throw new FileNotFoundException("Unsupported uri: " + uri);
        }

        Context context = getContext();
        if (context == null) {
            throw new NullPointerException("getContext() returned null in LogProvider.openFile()");
        }

        return ParcelFileDescriptor.open(
                com.gmail.walles.johan.headsetharry.ContactDeveloperUtil.getAttachmentFile(context),
                ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues contentvalues, String s,
                      String[] as)
    {
        return 0;
    }

    @Override
    public int delete(@NonNull Uri uri, String s, String[] as) {
        return 0;
    }

    @Override
    @Nullable
    public Uri insert(@NonNull Uri uri, ContentValues contentvalues) {
        return null;
    }

    @Override
    @Nullable
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Override
    @Nullable
    public Cursor query(@NonNull Uri uri, String[] projection, String s, String[] as1,
                        String s1)
    {
        return null;
    }
}
