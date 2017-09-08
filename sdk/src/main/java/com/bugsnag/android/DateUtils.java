package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

class DateUtils {
    // SimpleDateFormat isn't thread safe, cache one instance per thread as needed.
    private static final ThreadLocal<DateFormat> iso8601Holder = new ThreadLocal<DateFormat>() {
        @NonNull
        @Override
        protected DateFormat initialValue() {
            TimeZone tz = TimeZone.getTimeZone("UTC");
            DateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            iso8601.setTimeZone(tz);
            return iso8601;
        }
    };

    static String toISO8601(@NonNull Date date) {
        return iso8601Holder.get().format(date);
    }
}
