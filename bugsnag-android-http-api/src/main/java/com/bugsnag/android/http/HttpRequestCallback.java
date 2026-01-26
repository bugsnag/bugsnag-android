package com.bugsnag.android.http;

import androidx.annotation.NonNull;

/**
 * A callback that is invoked before an HTTP request is sent. This can be used to modify the
 * request information that is reported to BugSnag, or to prevent the request from being
 * reported at all.
 *
 * @param <R> the request type of the HTTP API being instrumented
 * @see HttpInstrumentationBuilder#addRequestCallback(HttpRequestCallback)
 */
public interface HttpRequestCallback<R> {
    /**
     * Called before an HTTP request is sent.
     *
     * @param req the instrumented request
     */
    void onHttpRequest(@NonNull HttpInstrumentedRequest<R> req);
}
