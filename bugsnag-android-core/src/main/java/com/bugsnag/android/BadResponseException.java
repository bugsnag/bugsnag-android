package com.bugsnag.android;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * @deprecated use {@link DeliveryFailureException} instead
 */
@Deprecated
public class BadResponseException extends Exception {
    private static final long serialVersionUID = -870190454845379171L;

    public BadResponseException(@NonNull String msg, int responseCode) {
        super(String.format(Locale.US, "%s (%d)", msg, responseCode));
    }
}
