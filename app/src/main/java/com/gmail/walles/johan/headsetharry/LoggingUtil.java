/*
 * Copyright 2016 Johan Walles <johan.walles@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gmail.walles.johan.headsetharry;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.jetbrains.annotations.NonNls;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

public class LoggingUtil {
    private static Class<Timber> initializedLoggingClass = null;

    private LoggingUtil() {
        // Don't let people instantiate this class
    }

    public static void setUpLogging(Context context) {
        Timber.Tree tree;
        if (EmulatorUtil.isRunningOnEmulator()) {
            tree = new LocalTree();
        } else {
            tree = new CrashlyticsTree();
        }

        if (initializedLoggingClass != Timber.class) {
            initializedLoggingClass = Timber.class;
            Timber.plant(tree);
        }

        if (!EmulatorUtil.isRunningOnEmulator()) {
            Fabric.with(context, new Crashlytics());
        }
    }

    private static class CrashlyticsTree extends Timber.Tree {
        @Override
        protected void log(int priority, @NonNls String tag, String message, Throwable t) {
            if (BuildConfig.DEBUG) {
                tag = "DEBUG";
            } else if (TextUtils.isEmpty(tag)) {
                tag = "HeadsetHarry";
            }

            // This call logs to *both* Crashlytics and LogCat
            Crashlytics.log(priority, tag, message);

            if (t != null) {
                Crashlytics.logException(t);
                Log.println(priority, tag, message + "\n" + Log.getStackTraceString(t));
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
