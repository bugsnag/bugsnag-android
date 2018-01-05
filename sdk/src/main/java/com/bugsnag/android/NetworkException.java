package com.bugsnag.android;

import java.io.IOException;

public class NetworkException extends IOException {
    public NetworkException(String url, Exception ex) {
        super(String.format("Network error when posting to %s", url));
        initCause(ex);
    }
}
