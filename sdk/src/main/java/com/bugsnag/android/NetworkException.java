package com.bugsnag.android;

import com.facebook.infer.annotation.ThreadSafe;

import java.io.IOException;

/**
 * @deprecated use {@link DeliveryFailureException} instead
 */
@Deprecated
@ThreadSafe
public class NetworkException extends IOException {
    private static final long serialVersionUID = -4370366096145029322L;

    public NetworkException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
