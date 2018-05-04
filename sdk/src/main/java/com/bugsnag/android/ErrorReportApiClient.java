package com.bugsnag.android;

import java.util.Map;

/**
 * @see Delivery
 */
@SuppressWarnings("WeakerAccess")
@Deprecated
public interface ErrorReportApiClient {

    /**
     * @see Delivery#deliver(Report, Configuration)
     */
    @Deprecated
    void postReport(String urlString, Report report, Map<String, String> headers)
        throws NetworkException, BadResponseException;

}
