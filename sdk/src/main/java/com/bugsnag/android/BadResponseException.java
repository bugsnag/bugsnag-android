package com.bugsnag.android;

import java.util.Locale;

/**
 * @deprecated use {@link DeliveryFailureException} instead
 */
@Deprecated
public class BadResponseException extends Exception {
    public BadResponseException(String msg, int responseCode) {
        super(String.format(Locale.US, "%s (%d)", msg, responseCode));
    }
}
