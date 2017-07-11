package com.bugsnag.android;

import java.io.IOException;
import java.util.Locale;

/**
 * Posts an error report to the Bugsnag API. Custom implementations of this client can be used in
 * place of the default implementation, by calling
 * {@link Bugsnag#setErrorReportApiClient(ErrorReportApiClient)}
 *
 * @see DefaultHttpClient
 */
@SuppressWarnings("WeakerAccess")
public interface ErrorReportApiClient {

    /**
     * Posts an Error Report to the Bugsnag API.
     * <p>
     * See <a href="https://docs.bugsnag.com/api/error-reporting/">https://docs.bugsnag.com/api/error-reporting/</a>
     *
     * @param urlString the Bugsnag endpoint
     * @param report    The error report
     * @throws NetworkException     if the client was unable to complete the request
     * @throws BadResponseException when a non-200 response code is received from the server
     */
    void postReport(String urlString, Report report) throws NetworkException, BadResponseException;

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
