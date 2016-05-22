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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import org.jetbrains.annotations.NonNls;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

public class LoggingUtils {
    private static Class<Timber> initializedLoggingClass = null;

    private static String version;

    private LoggingUtils() {
        // Don't let people instantiate this class
    }

    private static boolean isCrashlyticsEnabled() {
        return !EmulatorUtils.isRunningOnEmulator();
    }

    public static void logCustom(CustomEvent event) {
        if (isCrashlyticsEnabled()) {
            event.putCustomAttribute("App Version", version); //NON-NLS
            Answers.getInstance().logCustom(event);
        }
    }

    public static void setUpLogging(Context context) {
        Timber.Tree tree;
        if (isCrashlyticsEnabled()) {
            tree = new CrashlyticsTree(context);
        } else {
            tree = new LocalTree();
        }

        if (initializedLoggingClass != Timber.class) {
            initializedLoggingClass = Timber.class;
            Timber.plant(tree);
            Timber.v("Logging tree planted: %s", tree.getClass());
        }

        LogCollector.keepAlive(context);

        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Timber.w(e, "Unable to find out my own version");
            version = "<unknown>"; //NON-NLS
        }
    }

    private static class CrashlyticsTree extends Timber.Tree {
        public CrashlyticsTree(Context context) {
            Fabric.with(context, new Crashlytics());
        }

        @Override
        protected void log(int priority, @NonNls String tag, String message, Throwable t) {
            if (BuildConfig.DEBUG) {
                tag = "DEBUG";
            } else if (TextUtils.isEmpty(tag)) {
                tag = "HeadsetHarry";
            }

            // This call logs to *both* Crashlytics and LogCat, and will log the Exception backtrace
            // to LogCat on exceptions.
            Crashlytics.log(priority, tag, message);

            if (t != null) {
                Crashlytics.logException(t);
            }
        }
    }

    private static class LocalTree extends Timber.Tree {
        @Override
        protected void log(int priority, @NonNls String tag, String message, Throwable t) {
            if (BuildConfig.DEBUG) {
                tag = "DEBUG";
            } else if (TextUtils.isEmpty(tag)) {
                tag = "HeadsetHarry";
            }

            if (t != null) {
                message += "\n" + Log.getStackTraceString(t);
            }
            Log.println(priority, tag, message);
        }
    }
}
