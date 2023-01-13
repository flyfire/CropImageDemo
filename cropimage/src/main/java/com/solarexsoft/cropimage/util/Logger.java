package com.solarexsoft.cropimage.util;

/*
 * Creadted by houruhou on 2023/01/13 16:42
 */
public class Logger {
    private static final String TAG = "CropView";
    public static boolean enabled = false;

    public static void e(String msg) {
        if (!enabled) return;
        android.util.Log.e(TAG, msg);
    }

    public static void e(String msg, Throwable e) {
        if (!enabled) return;
        android.util.Log.e(TAG, msg, e);
    }

    public static void i(String msg) {
        if (!enabled) return;
        android.util.Log.i(TAG, msg);
    }

    public static void i(String msg, Throwable e) {
        if (!enabled) return;
        android.util.Log.i(TAG, msg, e);
    }
}
