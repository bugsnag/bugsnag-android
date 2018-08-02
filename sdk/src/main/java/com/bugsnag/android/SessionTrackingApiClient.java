package com.bugsnag.android;

import com.facebook.infer.annotation.ThreadSafe;

import java.util.Map;

/**
 * Posts an array of sessions to the Bugsnag Session Tracking API. Custom implementations
 * of this client can be used in place of the default implementation, by calling
 * {@link Bugsnag#setSessionTrackingApiClient(SessionTrackingApiClient)}
 *
 * @deprecated use {@link Delivery} to send sessions
 */
@Deprecated
@ThreadSafe
public interface SessionTrackingApiClient {

    /**
     * Posts an array of sessions to the Bugsnag API.
     *
     * @param urlString the Bugsnag endpoint
     * @param payload   The session tracking
     * @param headers   the HTTP headers
     * @throws NetworkException     if the client was unable to complete the request
     * @throws BadResponseException when a non-202 response code is received from the server
     */
    void postSessionTrackingPayload(String urlString,
                                    SessionTrackingPayload payload,
                                    Map<String, String> headers)
        throws NetworkException, BadResponseException;

}
