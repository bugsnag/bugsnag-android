package com.bugsnag.android;

/**
 * @deprecated use {@link DeliveryFailureException} instead
 */
@Deprecated
public class BadResponseException extends Exception {
    public BadResponseException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
