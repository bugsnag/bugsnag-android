package com.bugsnag.android;


/**
 * Implementations of this interface deliver Error Reports and Sessions captured to the Bugsnag API.
 * <p>
 * A default {@link Delivery} implementation is provided as part of Bugsnag initialization,
 * but you may wish to use your own implementation if you have requirements such
 * as pinning SSL certificates, for example.
 * <p>
 * Any custom implementation must be capable of sending
 * <a href="https://docs.bugsnag.com/api/error-reporting/">
 * Error Reports</a> and <a href="https://docs.bugsnag.com/api/sessions/">Sessions</> as
 * documented at <a href="https://docs.bugsnag.com/api/">https://docs.bugsnag.com/api/</a>
 *
 * @see DefaultDelivery
 */
public interface Delivery {

    /**
     * Posts an array of sessions to the Bugsnag Session Tracking API.
     * <p>
     * This request must be delivered to the endpoint at {@link Configuration#getSessionEndpoint()},
     * and contain the HTTP headers from {@link Configuration#getSessionApiHeaders()}.
     * <p>
     * If the response status code is not 202, then the implementation must throw
     * {@link DeliveryFailureException} with a reason of
     * {@link DeliveryFailureException.Reason#REQUEST_FAILURE}.
     * <p>
     * If the request could not be delivered due to connectivity issues, then the implementation
     * must throw {@link DeliveryFailureException} with a reason of
     * {@link DeliveryFailureException.Reason#CONNECTIVITY}, as this will cache the request for
     * delivery at a future time.
     * <p>
     * See <a href="https://docs.bugsnag.com/api/sessions/">
     * https://docs.bugsnag.com/api/sessions/</a>
     *
     * @param payload The session tracking payload
     * @param config  The configuration by which this request will be sent
     * @throws DeliveryFailureException when delivery does not receive a 202 status code.
     */
    void deliver(SessionTrackingPayload payload,
                 Configuration config) throws DeliveryFailureException;

    /**
     * Posts an Error Report to the Bugsnag Error Reporting API.
     * <p>
     * This request must be delivered to the endpoint at {@link Configuration#getSessionEndpoint()},
     * and contain the HTTP headers from {@link Configuration#getSessionApiHeaders()}.
     * <p>
     * If the response status code is not 2xx, then the implementation must throw
     * {@link DeliveryFailureException} with a reason of
     * {@link DeliveryFailureException.Reason#REQUEST_FAILURE}.
     * <p>
     * If the request could not be delivered due to connectivity issues, then the implementation
     * must throw {@link DeliveryFailureException} with a reason of
     * {@link DeliveryFailureException.Reason#CONNECTIVITY}, as this will cache the request for
     * delivery at a future time.
     * <p>
     * See <a href="https://docs.bugsnag.com/api/error-reporting/">
     * https://docs.bugsnag.com/api/error-reporting/</a>
     *
     * @param report The error report
     * @param config The configuration by which this request will be sent
     * @throws DeliveryFailureException when delivery does not receive a 2xx status code.
     */
    void deliver(Report report,
                 Configuration config) throws DeliveryFailureException;
}
