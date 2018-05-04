package com.bugsnag.android;

import java.util.Map;

/**
 * @see Delivery
 */
@Deprecated
public interface SessionTrackingApiClient {

    /**
     * @see Delivery#deliver(SessionTrackingPayload, Configuration)
     */
    @Deprecated
    void postSessionTrackingPayload(String urlString,
                                    SessionTrackingPayload payload,
                                    Map<String, String> headers)
        throws NetworkException, BadResponseException;

}
