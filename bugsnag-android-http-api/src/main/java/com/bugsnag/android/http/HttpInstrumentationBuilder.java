package com.bugsnag.android.http;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

/**
 * An abstraction of basic HTTP instrumentation configuration. This interface defines the basic
 * capabilities that can be configured for HTTP instrumentation without defining how the
 * instrumentation gets configured for its target HTTP implementation.
 *
 * @param <R> the request type of the HTTP API being instrumented
 * @param <S> the response type of the HTTP API being instrumented
 */
public interface HttpInstrumentationBuilder<R, S> {
    /**
     * Mark the given HTTP response status code as an error. Error responses are automatically
     * reported as "HTTP Errors" to BugSnag.
     *
     * @param statusCode the HTTP status code to add
     * @return this
     */
    @NonNull
    HttpInstrumentationBuilder<R, S> addHttpErrorCode(@IntRange(from = 0) int statusCode);

    /**
     * Mark the given HTTP response status code range as an error. Error responses are automatically
     * reported as "HTTP Errors" to BugSnag. All status codes within this (inclusive) range will
     * be considered errors.
     *
     * @param minStatusCode the low status code to mark as an error
     * @param maxStatusCode the high status code (inclusive) to mark as an error
     * @return this
     */
    @NonNull
    HttpInstrumentationBuilder<R, S> addHttpErrorCodes(@IntRange(from = 0) int minStatusCode,
                                                       @IntRange(from = 0) int maxStatusCode);

    /**
     * Un-mark the given HTTP response status code as an error.
     *
     * @param statusCode the HTTP status code to remove
     * @return this
     */
    @NonNull
    HttpInstrumentationBuilder<R, S> removeHttpErrorCode(@IntRange(from = 0) int statusCode);

    /**
     * Un-mark the given HTTP response status code range as an error.
     *
     * @param minStatusCode the low status code to un-mark as an error
     * @param maxStatusCode the high status code (inclusive) to un-mark as an error
     * @return this
     */
    @NonNull
    HttpInstrumentationBuilder<R, S> removeHttpErrorCodes(@IntRange(from = 0) int minStatusCode,
                                                          @IntRange(from = 0) int maxStatusCode);

    /**
     * Define the maximum number of bytes that can be captured from the request body (when one
     * exists). Setting the number to 0 will turn off request body capture.
     *
     * @param maxBytes the maximum number of bytes to capture
     * @return this
     */
    @NonNull
    HttpInstrumentationBuilder<R, S> maxRequestBodyCapture(@IntRange(from = 0) long maxBytes);

    /**
     * Define the maximum number of bytes that can be captured from the response body (when one
     * exists). Setting the number to 0 will turn off response body capture.
     *
     * @param maxBytes the maximum number of bytes to capture
     * @return this
     */
    @NonNull
    HttpInstrumentationBuilder<R, S> maxResponseBodyCapture(@IntRange(from = 0) long maxBytes);

    /**
     * Shorthand for {@link #logBreadcrumbs(boolean) logBreadcrumbs(true)}.
     *
     * @return this
     */
    @NonNull
    HttpInstrumentationBuilder<R, S> logBreadcrumbs();

    /**
     * Set the default for whether breadcrumbs should be logged for all HTTP requests observed by
     * the instrumentation. This can be overridden on a per-request basis in an
     * {@link #addResponseCallback(HttpResponseCallback) HttpResponseCallback}.
     * <p>
     * The {@link com.bugsnag.android.Configuration#setEnabledBreadcrumbTypes(java.util.Set)
     * enabledBreadcrumbTypes} configuration option will override this setting. If {@code REQUEST}
     * breadcrumbs have been disabled in the configuration, this option will have no effect.
     *
     * @param logBreadcrumbs true if breadcrumbs should be logged
     * @return this
     */
    @NonNull
    HttpInstrumentationBuilder<R, S> logBreadcrumbs(boolean logBreadcrumbs);

    /**
     * Add a callback to be invoked before an HTTP request is sent. This can be used to modify
     * the request information that is reported to BugSnag, or to prevent the request from being
     * reported at all.
     *
     * @param callback the callback to add
     * @return this
     */
    @NonNull
    HttpInstrumentationBuilder<R, S> addRequestCallback(@NonNull HttpRequestCallback<R> callback);

    /**
     * Add a callback to be invoked after an HTTP response is received. This can be used to modify
     * the response information that is reported to BugSnag, or to prevent the response from being
     * reported at all. The callback can also be used to override the default breadcrumb and
     * error reporting behaviour on a per-request basis.
     *
     * @param callback the callback to add
     * @return this
     */
    @NonNull
    HttpInstrumentationBuilder<R, S> addResponseCallback(@NonNull HttpResponseCallback<R, S> callback);

}
