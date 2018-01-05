package com.bugsnag.android;


import java.util.Locale;

public class BadResponseException extends Exception {
    public BadResponseException(String url, int responseCode) {
        super(String.format(Locale.US, "Got non-200 response code (%d) from %s", responseCode, url));
    }
}
