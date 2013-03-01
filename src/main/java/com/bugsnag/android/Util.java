package com.bugsnag.android;

import android.content.Context;

public class Util {
    public static String getContextName(Context context) {
        String name = context.getClass().getName();
        return name.substring(name.lastIndexOf('.') + 1);
    }
}