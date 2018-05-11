package com.bugsnag.android;

import java.io.IOException;

/**
 * @deprecated use {@link DeliveryFailureException} instead
 */
@Deprecated
public class NetworkException extends IOException {
    public NetworkException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
