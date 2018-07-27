package com.bugsnag.android;

import com.facebook.infer.annotation.ThreadSafe;

import java.util.Map;

/**
 * Posts an error report to the Bugsnag API. Custom implementations of this client can be used in
 * place of the default implementation, by calling
 * {@link Bugsnag#setErrorReportApiClient(ErrorReportApiClient)}
 *
 * @deprecated use {@link Delivery} to send error reports
 */
@SuppressWarnings("WeakerAccess")
@Deprecated
@ThreadSafe
public interface ErrorReportApiClient {

    /**
     * Posts an Error Report to the Bugsnag API.
     * <p>
     * See <a href="https://docs.bugsnag.com/api/error-reporting/">
     *     https://docs.bugsnag.com/api/error-reporting/</a>
     *
     * @param urlString the Bugsnag endpoint
     * @param report    The error report
     * @param headers   the HTTP headers
     * @throws NetworkException     if the client was unable to complete the request
     * @throws BadResponseException when a non-200 response code is received from the server
     */
    void postReport(String urlString, Report report, Map<String, String> headers)
        throws NetworkException, BadResponseException;

}
