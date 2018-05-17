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
 * Error Reports</a> and <a href="https://docs.bugsnag.com/api/sessions/">Sessions</a> as
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
     * If the request was not successful and you wish to try again later, throw
     * {@link DeliveryFailureException}. The notifier will cache the payload and initiate delivery
     * at a future time. For example, if you are unable to obtain a network connection, it would
     * make sense to attempt delivery later on, whereas if the status code was 400, it would not.
     * <p>
     * See <a href="https://docs.bugsnag.com/api/sessions/">
     * https://docs.bugsnag.com/api/sessions/</a>
     *
     * @param payload The session tracking payload
     * @param config  The configuration by which this request will be sent
     * @throws DeliveryFailureException when delivery is not successful and
     *                                  the report should be stored for future attempts.
     */
    void deliver(SessionTrackingPayload payload,
                 Configuration config) throws DeliveryFailureException;

    /**
     * Posts an Error Report to the Bugsnag Error Reporting API.
     * <p>
     * This request must be delivered to the endpoint at {@link Configuration#getSessionEndpoint()},
     * and contain the HTTP headers from {@link Configuration#getSessionApiHeaders()}.
     * <p>
     * If the request was not successful and you wish to try again later, throw
     * {@link DeliveryFailureException}. The notifier will cache the payload and initiate delivery
     * at a future time. For example, if you are unable to obtain a network connection, it would
     * make sense to attempt delivery later on, whereas if the status code was 400, it would not.
     * <p>
     * See <a href="https://docs.bugsnag.com/api/error-reporting/">
     * https://docs.bugsnag.com/api/error-reporting/</a>
     *
     * @param report The error report
     * @param config The configuration by which this request will be sent
     * @throws DeliveryFailureException when delivery is not successful and
     *                                  the report should be stored for future attempts.
     */
    void deliver(Report report,
                 Configuration config) throws DeliveryFailureException;
}
