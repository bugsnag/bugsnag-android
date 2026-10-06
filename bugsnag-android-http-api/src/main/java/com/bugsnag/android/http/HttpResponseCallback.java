package com.bugsnag.android.http;

import androidx.annotation.NonNull;

/**
 * A callback that is invoked after an HTTP response is received. This can be used to modify the
 * response information that is reported to BugSnag, or to prevent the response from being
 * reported at all.
 *
 * @param <R> the request type of the HTTP API being instrumented
 * @param <S> the response type of the HTTP API being instrumented
 * @see HttpInstrumentationBuilder#addResponseCallback(HttpResponseCallback)
 */
public interface HttpResponseCallback<R, S> {
    /**
     * Called after an HTTP response is received.
     *
     * @param response the instrumented response
     */
    void onHttpResponse(@NonNull HttpInstrumentedResponse<R, S> response);
}
