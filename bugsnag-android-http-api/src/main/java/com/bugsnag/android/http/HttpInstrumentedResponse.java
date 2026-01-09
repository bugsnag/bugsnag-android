package com.bugsnag.android.http;

import com.bugsnag.android.OnErrorCallback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Represents an HTTP response that has been instrumented by BugSnag. This interface provides
 * access to the original request and response objects, as well as methods to modify the
 * information that is reported to BugSnag.
 *
 * @param <R> the request type of the HTTP API being instrumented
 * @param <S> the response type of the HTTP API being instrumented
 */
public interface HttpInstrumentedResponse<R, S> {
    /**
     * The original HTTP request object.
     *
     * @return the original request
     */
    @NonNull
    R getRequest();

    /**
     * The original HTTP response object, if one was received. This may be {@code null} if the
     * request failed to produce a response (e.g. due to a network error).
     *
     * @return the original response, or {@code null}
     */
    @Nullable
    S getResponse();

    /**
     * Override whether this request/response should be reported as a breadcrumb. This defaults
     * to the value passed to {@link HttpInstrumentationBuilder#logBreadcrumbs(boolean)}, but
     * cannot override whether {@link com.bugsnag.android.BreadcrumbType#REQUEST REQUEST}
     * breadcrumbs are enabled.
     *
     * @param isBreadcrumbReported false if a breadcrumb should not be reported
     */
    void setBreadcrumbReported(boolean isBreadcrumbReported);

    /**
     * Check whether a breadcrumb will be reported for this response.
     *
     * @return true if a breadcrumb will be reported
     */
    boolean isBreadcrumbReported();

    /**
     * Mark that an error should be reported for this response. This will default to true if the
     * HTTP status code in the {@link #getResponse() response} was added as an "error code" using
     * {@link HttpInstrumentationBuilder#addHttpErrorCode(int)}.
     *
     * @param isErrorReported true if an error has been reported
     */
    void setErrorReported(boolean isErrorReported);

    /**
     * Check whether an error will be reported for this response.
     *
     * @return true if an error should be reported
     */
    boolean isErrorReported();

    /**
     * The response body that will be reported to BugSnag for this response. This may be different
     * from the original response body if it has been modified by a callback.
     *
     * @return the reported response body
     */
    @Nullable
    String getReportedResponseBody();

    /**
     * Set the response body that will be reported to BugSnag for this response. Setting this to
     * {@code null} will prevent the response body from being reported.
     *
     * @param responseBody the response body to report
     */
    void setReportedResponseBody(@Nullable String responseBody);

    /**
     * Set an {@code OnErrorCallback} that can customise {@link com.bugsnag.android.Event Events}
     * created as a consequence to this response (when {@link #isErrorReported()} is true). Setting
     * this to {@code null} will remove any existing error callback.
     *
     * @param onErrorCallback the error callback to customise HTTP events
     */
    void setErrorCallback(@Nullable OnErrorCallback onErrorCallback);

    /**
     * Return the error callback if one has been set.
     *
     * @return the error callback for this response
     */
    @Nullable
    OnErrorCallback getErrorCallback();
}
