package com.bugsnag.android;

/**
 * This should be thrown if delivery of a request was not successful and you wish to try again
 * later. The notifier will cache the payload and initiate delivery at a future time.
 *
 * @see Delivery
 */
public class DeliveryFailureException extends Exception {
    public DeliveryFailureException(String message) {
        super(message);
    }

    public DeliveryFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
