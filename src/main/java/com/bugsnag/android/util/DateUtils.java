package com.bugsnag.android;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

class DateUtils {
    private static DateFormat iso8601;

    static {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        iso8601.setTimeZone(tz);
    }

    static String toISO8601(Date date) {
        return iso8601.format(date);
    }
}
