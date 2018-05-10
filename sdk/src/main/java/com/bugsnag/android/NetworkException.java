package com.bugsnag.android;

import java.io.IOException;

public class NetworkException extends IOException {
    public NetworkException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
