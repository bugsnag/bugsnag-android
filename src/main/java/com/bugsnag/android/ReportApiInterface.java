package com.bugsnag.android;

import java.io.IOException;
import java.util.Locale;

public interface ReportApiInterface {

    void postReport(String urlString, JsonStream.Streamable reportPayload) throws NetworkException, BadResponseException;

    class BadResponseException extends Exception {
        public BadResponseException(String url, int responseCode) {
            super(String.format(Locale.US, "Got non-200 response code (%d) from %s", responseCode, url));
        }
    }

    class NetworkException extends IOException {
        public NetworkException(String url, Exception ex) {
            super(String.format("Network error when posting to %s", url));
            initCause(ex);
        }
    }
}
