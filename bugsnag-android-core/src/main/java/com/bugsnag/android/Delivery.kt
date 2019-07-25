package com.bugsnag.android

/**
 * Implementations of this interface deliver Error Reports and Sessions captured to the Bugsnag API.
 *
 * A default [Delivery] implementation is provided as part of Bugsnag initialization,
 * but you may wish to use your own implementation if you have requirements such
 * as pinning SSL certificates, for example.
 *
 * Any custom implementation must be capable of sending
 * [Error Reports](https://docs.bugsnag.com/api/error-reporting/)
 * and [Sessions](https://docs.bugsnag.com/api/sessions/) as
 * documented at [https://docs.bugsnag.com/api/](https://docs.bugsnag.com/api/)
 *
 * @see DefaultDelivery
 */
interface Delivery {

    /**
     * Posts an array of sessions to the Bugsnag Session Tracking API.
     *
     *
     * This request must be delivered to the endpoint specified in deliveryParams with the given
     * HTTP headers.
     *
     *
     * You should return the status code of the network request made within your Delivery
     * implementation. A 2xx status will indicate success, a 4xx status will remove the cached
     * payload from disk, and a 5xx status will save the payload and attempt delivery again later.
     *
     * See [https://docs.bugsnag.com/api/sessions/](https://docs.bugsnag.com/api/sessions/)
     *
     * @param payload        The session tracking payload
     * @param deliveryParams The delivery parameters to be used for this request
     */
    fun deliver(payload: SessionTrackingPayload, deliveryParams: DeliveryParams): DeliveryStatus

    /**
     * Posts an Error Report to the Bugsnag Error Reporting API.
     *
     * This request must be delivered to the endpoint specified in deliveryParams with the given
     * HTTP headers.
     *
     * You should return the status code of the network request made within your Delivery
     * implementation. A 2xx status will indicate success, a 4xx status will remove the cached
     * payload from disk, and a 5xx status will save the payload and attempt delivery again later.
     *
     * See [https://docs.bugsnag.com/api/error-reporting/]
     * (https://docs.bugsnag.com/api/error-reporting/)
     *
     * @param report         The error report
     * @param deliveryParams The delivery parameters to be used for this request
     */
    fun deliver(report: Report, deliveryParams: DeliveryParams): DeliveryStatus
}
