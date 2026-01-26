package com.bugsnag.android.http;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Represents an HTTP request that has been instrumented by BugSnag. This interface provides
 * access to the original request object, as well as methods to modify the information that is
 * reported to BugSnag.
 *
 * @param <R> the request type of the HTTP API being instrumented
 */
public interface HttpInstrumentedRequest<R> {
    /**
     * The original HTTP request object.
     *
     * @return the original request
     */
    @NonNull
    R getRequest();

    /**
     * The URL that will be reported to BugSnag for this request. This may be different from the
     * original request URL if it has been modified by a callback.
     *
     * @return the reported URL
     */
    @Nullable
    String getReportedUrl();

    /**
     * Set the URL that will be reported to BugSnag for this request. Setting this to {@code null}
     * will prevent the request from being reported at all.
     *
     * @param reportedUrl the URL to report
     */
    void setReportedUrl(@Nullable String reportedUrl);

    /**
     * The request body that will be reported to BugSnag for this request. This may be different
     * from the original request body if it has been modified by a callback.
     *
     * @return the reported request body
     */
    @Nullable
    String getReportedRequestBody();

    /**
     * Set the request body that will be reported to BugSnag for this request. Setting this to
     * {@code null} will prevent the request body from being reported.
     *
     * @param requestBody the request body to report
     */
    void setReportedRequestBody(@Nullable String requestBody);
}
