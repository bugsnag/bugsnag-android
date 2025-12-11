package com.bugsnag.android

/**
 * The parameters which should be used to deliver an Event/Session.
 */
class DeliveryParams @JvmOverloads constructor(

    /**
     * The endpoint to which the payload should be sent
     */
    val endpoint: String,

    /**
     * The HTTP headers which must be attached to the request
     */
    val headers: Map<String, String?>,

    /**
     * Additional encoding that can be applied by the `Delivery` implementation.
     */
    val payloadEncoding: PayloadEncoding = PayloadEncoding.NONE,
) {
    /**
     * Possible encodings to be applied by `Delivery` implementations.
     */
    enum class PayloadEncoding {
        NONE,

        /**
         * The `Delivery` implementation should attempt to gzip the payload before sending it.
         * This is used when the server is known to support gzip ahead of the delivery.
         *
         * This does *not* imply that the `Content-Encoding: gzip` header is in
         * [DeliveryParams.headers] and the implementation is entirely dependant on the `Delivery`
         * class.
         */
        GZIP
    }
}
