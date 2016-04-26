package com.gmail.walles.johan.headsetharry;

import android.os.Build;

import org.jetbrains.annotations.NonNls;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class EmulatorUtil {
    private EmulatorUtil() {
        throw new UnsupportedOperationException("Utility class, please don't instantiate");
    }

    public static boolean isRunningOnEmulator() {
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
}
