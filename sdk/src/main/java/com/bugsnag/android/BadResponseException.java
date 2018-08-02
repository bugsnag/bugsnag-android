package com.bugsnag.android;

import com.facebook.infer.annotation.ThreadSafe;

import java.util.Locale;

/**
 * @deprecated use {@link DeliveryFailureException} instead
 */
@Deprecated
@ThreadSafe
public class BadResponseException extends Exception {
    private static final long serialVersionUID = -870190454845379171L;

    public BadResponseException(String msg, int responseCode) {
        super(String.format(Locale.US, "%s (%d)", msg, responseCode));
    }
}
