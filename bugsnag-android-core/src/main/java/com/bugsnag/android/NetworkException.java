package com.bugsnag.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

/**
 * @deprecated use {@link DeliveryFailureException} instead
 */
@Deprecated
@ThreadSafe
public class NetworkException extends IOException {
    private static final long serialVersionUID = -4370366096145029322L;

    public NetworkException(@NonNull String msg, @Nullable Throwable cause) {
        super(msg, cause);
    }
}
