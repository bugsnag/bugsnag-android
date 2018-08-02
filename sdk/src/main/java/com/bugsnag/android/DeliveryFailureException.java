package com.bugsnag.android;

import com.facebook.infer.annotation.ThreadSafe;

/**
 * This should be thrown if delivery of a request was not successful and you wish to try again
 * later. The notifier will cache the payload and initiate delivery at a future time.
 *
 * @see Delivery
 */
@ThreadSafe
public class DeliveryFailureException extends Exception {
    private static final long serialVersionUID = 1501477209400426470L;

    public DeliveryFailureException(String message) {
        super(message);
    }

    public DeliveryFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
